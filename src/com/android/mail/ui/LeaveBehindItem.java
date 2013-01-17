/*
 * Copyright (C) 2012 Google Inc.
 * Licensed to The Android Open Source Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mail.ui;

import android.animation.ObjectAnimator;
import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.mail.R;
import com.android.mail.browse.ConversationCursor;
import com.android.mail.browse.ConversationItemViewCoordinates;
import com.android.mail.providers.Account;
import com.android.mail.providers.Conversation;
import com.android.mail.providers.Folder;
import com.android.mail.utils.Utils;
import com.google.common.collect.ImmutableList;

public class LeaveBehindItem extends FrameLayout implements OnClickListener, SwipeableItemView {

    private ToastBarOperation mUndoOp;
    private Account mAccount;
    private AnimatedAdapter mAdapter;
    private TextView mText;
    private View mSwipeableContent;
    public int position;
    private static int sShrinkAnimationDuration = -1;
    private static int sFadeInAnimationDuration = -1;
    private static int sDismissAllLeaveBehindsDelay = -1;
    private static float sScrollSlop;
    private static float OPAQUE = 1.0f;
    private static float INVISIBLE = 0.0f;

    public LeaveBehindItem(Context context) {
        this(context, null);
    }

    public LeaveBehindItem(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public LeaveBehindItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (sShrinkAnimationDuration == -1) {
            Resources res = context.getResources();
            sShrinkAnimationDuration = res.getInteger(R.integer.shrink_animation_duration);
            sFadeInAnimationDuration = res.getInteger(R.integer.fade_in_animation_duration);
            sScrollSlop = res.getInteger(R.integer.leaveBehindSwipeScrollSlop);
            sDismissAllLeaveBehindsDelay = res
                    .getInteger(R.integer.dismiss_all_leavebehinds_delay);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.swipeable_content:
                if (mAccount.undoUri != null && !mInert) {
                    // NOTE: We might want undo to return the messages affected,
                    // in which case the resulting cursor might be interesting...
                    // TODO: Use UIProvider.SEQUENCE_QUERY_PARAMETER to indicate
                    // the set of commands to undo
                    mAdapter.setSwipeUndo(true);
                    mAdapter.clearLeaveBehind(getConversationId());
                    ConversationCursor cursor = mAdapter.getConversationCursor();
                    if (cursor != null) {
                        cursor.undo(getContext(), mAccount.undoUri);
                    }
                }
                break;
            case R.id.undo_descriptionview:
                // Essentially, makes sure that tapping description view doesn't highlight
                // either the undo button icon or text.
                break;
        }
    }

    public void bindOperations(int pos, Account account, AnimatedAdapter adapter,
            ToastBarOperation undoOp, Conversation target, Folder folder) {
        position = pos;
        mUndoOp = undoOp;
        mAccount = account;
        mAdapter = adapter;
        setData(target);
        mSwipeableContent = findViewById(R.id.swipeable_content);
        // Listen on swipeable content so that we can show both the undo icon
        // and button text as selected since they set duplicateParentState to true
        mSwipeableContent.setOnClickListener(this);
        mSwipeableContent.setAlpha(INVISIBLE);
        mText = ((TextView) findViewById(R.id.undo_descriptionview));
        mText.setText(Utils.convertHtmlToPlainText(mUndoOp
                .getSingularDescription(getContext(), folder)));
        mText.setOnClickListener(this);
    }

    public void commit() {
        ConversationCursor cursor = mAdapter.getConversationCursor();
        if (cursor != null) {
            cursor.delete(getContext(), ImmutableList.of(getData()));
        }
    }

    @Override
    public void dismiss() {
        if (mAdapter != null) {
            mAdapter.fadeOutSpecificLeaveBehindItem(mData.id);
            mAdapter.notifyDataSetChanged();
        }
    }

    public long getConversationId() {
        return getData().id;
    }

    @Override
    public View getSwipeableView() {
        return mSwipeableContent;
    }

    @Override
    public boolean canChildBeDismissed() {
        return !mInert;
    }

    public LeaveBehindData getLeaveBehindData() {
        return new LeaveBehindData(getData(), mUndoOp);
    }

    private Conversation mData;
    private int mAnimatedHeight = -1;
    private int mWidth;
    private boolean mAnimating;
    private boolean mFadingInText;
    private boolean mInert = false;
    private ObjectAnimator mFadeIn;

    /**
     * Animate shrinking the height of this view.
     * @param item the conversation to animate
     * @param listener the method to call when the animation is done
     * @param undo true if an operation is being undone. We animate the item
     *            away during delete. Undoing populates the item.
     */
    public void startShrinkAnimation(ViewMode viewMode, AnimatorListener listener) {
        if (!mAnimating) {
            mAnimating = true;
            int minHeight = ConversationItemViewCoordinates.getMinHeight(getContext(), viewMode);
            setMinimumHeight(minHeight);
            final int start = minHeight;
            final int end = 0;
            ObjectAnimator height = ObjectAnimator.ofInt(this, "animatedHeight", start, end);
            mAnimatedHeight = start;
            mWidth = getMeasuredWidth();
            height.setInterpolator(new DecelerateInterpolator(1.75f));
            height.setDuration(sShrinkAnimationDuration);
            height.addListener(listener);
            height.start();
        }
    }

    /**
     * Set the alpha value for the text displayed by this item.
     */
    public void setTextAlpha(float alpha) {
        if (mSwipeableContent.getAlpha() > INVISIBLE) {
            mSwipeableContent.setAlpha(alpha);
        }
    }

    public void startFadeInTextAnimation() {
        // If this thing isn't already fully visible AND its not already animating...
        if (!mFadingInText && mSwipeableContent.getAlpha() != OPAQUE) {
            mFadingInText = true;
            final float start = INVISIBLE;
            final float end = OPAQUE;
            mFadeIn = ObjectAnimator.ofFloat(mSwipeableContent, "alpha", start, end);
            mSwipeableContent.setAlpha(INVISIBLE);
            mFadeIn.setStartDelay(sDismissAllLeaveBehindsDelay);
            mFadeIn.setInterpolator(new DecelerateInterpolator(OPAQUE));
            mFadeIn.setDuration(sFadeInAnimationDuration / 2);
            mFadeIn.start();
        }
    }

    public void cancelFadeInTextAnimation() {
        if (mFadeIn != null) {
            mFadingInText = false;
            mFadeIn.cancel();
        }
    }

    public void setData(Conversation conversation) {
        mData = conversation;
    }

    public Conversation getData() {
        return mData;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mAnimatedHeight != -1) {
            setMeasuredDimension(mWidth, mAnimatedHeight);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
        return;
    }

    // Used by animator
    @SuppressWarnings("unused")
    public void setAnimatedHeight(int height) {
        mAnimatedHeight = height;
        requestLayout();
    }

    @Override
    public float getMinAllowScrollDistance() {
        return sScrollSlop;
    }

    public void makeInert() {
        if (mFadeIn != null) {
            mFadeIn.cancel();
        }
        mSwipeableContent.setVisibility(View.GONE);
        mInert = true;
    }

    public void cancelFadeOutText() {
        mSwipeableContent.setAlpha(OPAQUE);
    }
}
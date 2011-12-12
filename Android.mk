LOCAL_PATH:= $(call my-dir)

# Include res dir from chips
chips_dir := ../../../frameworks/ex/chips/res
mail_common_dir := ../../../frameworks/opt/mailcommon/res
res_dirs := $(chips_dir) $(mail_common_dir) res


# Remove symlinks created by a previous Gmail ADT build
$(shell rm -f $(LOCAL_PATH)/chips)
$(shell rm -f $(LOCAL_PATH)/mailcommon)

##################################################
# Build APK
include $(CLEAR_VARS)

LOCAL_PACKAGE_NAME := UnifiedEmail

LOCAL_STATIC_JAVA_LIBRARIES := mail-common
LOCAL_STATIC_JAVA_LIBRARIES := android-common-chips

LOCAL_SDK_VERSION := current

LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs)) \
        $(call all-logtags-files-under, $(src_dirs))
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs)) $(LOCAL_PATH)/res
LOCAL_AAPT_FLAGS := --auto-add-overlay

include $(BUILD_PACKAGE)


##################################################
# Build Static Lib
include $(CLEAR_VARS)

LOCAL_MODULE := unified-email

LOCAL_STATIC_JAVA_LIBRARIES := mail-common
LOCAL_STATIC_JAVA_LIBRARIES := android-common-chips

LOCAL_SDK_VERSION := current

LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs)) \
        $(call all-logtags-files-under, $(src_dirs))

include $(BUILD_STATIC_JAVA_LIBRARY)
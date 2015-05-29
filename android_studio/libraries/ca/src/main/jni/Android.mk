﻿APP_PATH := $(call my-dir)

#specify project root path here wrt make file directory
CONNECTIVITY_ROOT_PATH = ../../../../../resource/csdk/connectivity

include $(CLEAR_VARS)
LOCAL_PATH := ./jni
LOCAL_MODULE := CAInterface
LOCAL_STATIC_LIBRARIES = CA
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_PATH :=  ./jni
LOCAL_MODULE := RMInterface
LOCAL_SRC_FILES := ResourceModel.c
LOCAL_STATIC_LIBRARIES := CA
LOCAL_LDLIBS := -llog
LOCAL_C_INCLUDES := $(CONNECTIVITY_ROOT_PATH)/api
include $(BUILD_SHARED_LIBRARY)

#+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
#Build CACommon CACoap CA
# PROJECT_ROOT_PATH used by included make files.
PROJECT_ROOT_PATH = $(CONNECTIVITY_ROOT_PATH)

include $(CLEAR_VARS)
CA_LIB_PATH = $(CONNECTIVITY_ROOT_PATH)/build/android
include $(CA_LIB_PATH)/jni/Android.mk
#+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

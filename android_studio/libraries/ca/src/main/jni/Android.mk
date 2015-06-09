﻿APP_PATH := $(call my-dir)

#specify project root path here wrt make file directory
CSDK_ROOT_PATH         = ../../../../../resource/csdk
CONNECTIVITY_ROOT_PATH = $(CSDK_ROOT_PATH)/connectivity
C_COMMON_ROOT_PATH     = ../../../../../resource/c_common
MESHCOP_ROOT           = ../../../../../resource/meshcop

include $(CLEAR_VARS)
LOCAL_PATH := .
LOCAL_MODULE := CAInterface
LOCAL_STATIC_LIBRARIES = CA
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_PATH := .
LOCAL_MODULE := MeshCop
LOCAL_SRC_FILES := jni/ResourceModel.c $(MESHCOP_ROOT)/src/MeshCop.c
LOCAL_STATIC_LIBRARIES := CA
LOCAL_LDLIBS := -llog
LOCAL_C_INCLUDES := $(CONNECTIVITY_ROOT_PATH)/api $(CONNECTIVITY_ROOT_PATH)/common/inc $(C_COMMON_ROOT_PATH)/oic_malloc/include $(MESHCOP_ROOT)/include
include $(BUILD_SHARED_LIBRARY)

#+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
#Build CACommon CACoap CA
# PROJECT_ROOT_PATH used by included make files.
PROJECT_ROOT_PATH = $(CONNECTIVITY_ROOT_PATH)

include $(CLEAR_VARS)
CA_LIB_PATH = $(CONNECTIVITY_ROOT_PATH)/build/android
include $(CA_LIB_PATH)/jni/Android.mk
#+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

﻿APP_STL := gnustl_shared

APP_PLATFORM = android-21
APP_CPPFLAGS += -fexceptions
APP_CPPFLAGS += -frtti += -Wno-error=format-security
APP_CFLAGS = -Wno-error=format-security

APP_ABI := armeabi-v7a
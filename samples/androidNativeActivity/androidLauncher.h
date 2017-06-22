/*
  * Copyright 2010-2017 JetBrains s.r.o.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

#ifndef ANDROID_LAUNCHER_H
#define ANDROID_LAUNCHER_H

#include <unistd.h>

#include <android/configuration.h>
#include <android/looper.h>
#include <android/native_activity.h>

#ifdef __cplusplus
extern "C" {
#endif

struct NativeActivityState {
  struct ANativeActivity* activity;
  void* savedState;
  size_t savedStateSize;
  struct ALooper* looper;
};

void getNativeActivityState(struct NativeActivityState* state);

void notifySysEventProcessed();

#define LOOPER_ID_SYS 1

typedef enum NativeActivityEventKind {
  UNKNOWN,
  DESTROY,
  START,
  RESUME,
  SAVE_INSTANCE_STATE,
  PAUSE,
  STOP,
  CONFIGURATION_CHANGED,
  LOW_MEMORY,
  WINDOW_GAINED_FOCUS,
  WINDOW_LOST_FOCUS,
  NATIVE_WINDOW_CREATED,
  NATIVE_WINDOW_DESTROYED,
  INPUT_QUEUE_CREATED,
  INPUT_QUEUE_DESTROYED
} NativeActivityEventKind;

struct NativeActivityEvent {
  NativeActivityEventKind eventKind;
};

struct NativeActivitySaveStateEvent {
  NativeActivityEventKind eventKind;
  void* savedState;
  size_t savedStateSize;
};

struct NativeActivityWindowEvent {
  NativeActivityEventKind eventKind;
  struct ANativeWindow* window;
};

struct NativeActivityQueueEvent {
  NativeActivityEventKind eventKind;
  struct AInputQueue* queue;
};

#ifdef __cplusplus
}
#endif

#endif // ANDROID_LAUNCHER_H
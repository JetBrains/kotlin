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

#include "Memory.h"
#include "Natives.h"
#include "Runtime.h"
#include "KString.h"
#include "Types.h"

#ifdef ANDROID
#include <unistd.h>
#include <pthread.h>
#include <sys/types.h>
#include <sys/socket.h>

#include "androidLauncher.h"

#include <android/log.h>

#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "Konan_main", __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, "Konan_main", __VA_ARGS__))

/* For debug builds, always enable the debug traces in this library */
#ifndef NDEBUG
#  define LOGV(...)  ((void)__android_log_print(ANDROID_LOG_VERBOSE, "Konan_main", __VA_ARGS__))
#else
#  define LOGV(...)  ((void)0)
#endif

//--- main --------------------------------------------------------------------//
extern "C" KInt Konan_start(const ObjHeader*);

static int pipeC, pipeKonan;

static NativeActivityState nativeActivityState;

extern "C" void getNativeActivityState(NativeActivityState* state) {
  state->activity = nativeActivityState.activity;
  state->savedState = nativeActivityState.savedState;
  state->savedStateSize = nativeActivityState.savedStateSize;
  state->looper = nativeActivityState.looper;
}

extern "C" void notifySysEventProcessed(int fd) {
  if (fd != pipeKonan) return;
  int8_t message;
  write(pipeKonan, &message, sizeof(message));
}

static void* entry(void* param) {
  ALooper* looper = ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS);
  ALooper_addFd(looper, pipeKonan, LOOPER_ID_SYS, ALOOPER_EVENT_INPUT, NULL, NULL);
  nativeActivityState.looper = looper;

  RuntimeState* state = InitRuntime();

  if (state == nullptr) {
    LOGE("Unable to init runtime\n");
    return nullptr;
  }

  KInt exitStatus;
  {
    ObjHolder args;
    AllocArrayInstance(theArrayTypeInfo, 0, args.slot());
    exitStatus = Konan_start(args.obj());
  }

  DeinitRuntime(state);
  return nullptr;
}

static void runKonan_start() {
  int pipes[2];
  if (socketpair(AF_UNIX, SOCK_STREAM, 0, pipes)) {
    LOGE("Could not create pipe: %s", strerror(errno));
    return;
  }
  pipeC = pipes[0];
  pipeKonan = pipes[1];

  pthread_attr_t attr;
  pthread_attr_init(&attr);
  pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
  pthread_t thread;
  pthread_create(&thread, &attr, entry, nullptr);
}

static void putEventSynchronously(void* event) {
  uint64_t value = static_cast<uint64_t>(reinterpret_cast<uintptr_t>(event));
  if (write(pipeC, &value, sizeof(value)) != sizeof(value)) {
    LOGE("Failure writing event: %s\n", strerror(errno));
  }
  int8_t response;
  if (read(pipeC, &response, sizeof(response)) != sizeof(response)) {
    LOGE("Failure reading response: %s\n", strerror(errno));
  }
}

static void onDestroy(ANativeActivity* activity) {
  LOGV("onDestroy called");
  NativeActivityEvent event = { DESTROY };
  putEventSynchronously(&event);
}

static void onStart(ANativeActivity* activity) {
  LOGV("onStart called");
  NativeActivityEvent event = { START };
  putEventSynchronously(&event);
}

static void onResume(ANativeActivity* activity) {
  LOGV("onResume called");
  NativeActivitySaveStateEvent event = { RESUME };
  putEventSynchronously(&event);
}

static void* onSaveInstanceState(ANativeActivity* activity, size_t* outLen) {
  LOGV("onSaveInstanceState called");
  NativeActivitySaveStateEvent event = { SAVE_INSTANCE_STATE };
  putEventSynchronously(&event);
  *outLen = event.savedStateSize;
  return event.savedState;
}

static void onPause(ANativeActivity* activity) {
  LOGV("onPause called");
  NativeActivityEvent event = { PAUSE };
  putEventSynchronously(&event);
}

static void onStop(ANativeActivity* activity) {
  LOGV("onStop called");
  NativeActivityEvent event = { STOP };
  putEventSynchronously(&event);
}

static void onConfigurationChanged(ANativeActivity* activity) {
  LOGV("onConfigurationChanged called");
  NativeActivityEvent event = { CONFIGURATION_CHANGED };
  putEventSynchronously(&event);
}

static void onLowMemory(ANativeActivity* activity) {
  LOGV("onLowMemory called");
  NativeActivityEvent event = { LOW_MEMORY };
  putEventSynchronously(&event);
}

static void onWindowFocusChanged(ANativeActivity* activity, int focused) {
  LOGV("onWindowFocusChanged called");
  NativeActivityEvent event = { focused ? WINDOW_GAINED_FOCUS : WINDOW_LOST_FOCUS };
  putEventSynchronously(&event);
}

static void onNativeWindowCreated(ANativeActivity* activity, ANativeWindow* window) {
  LOGV("onNativeWindowCreated called");
  NativeActivityWindowEvent event = { NATIVE_WINDOW_CREATED, window };
  putEventSynchronously(&event);
}

static void onNativeWindowDestroyed(ANativeActivity* activity, ANativeWindow* window) {
  LOGV("onNativeWindowDestroyed called");
  NativeActivityWindowEvent event = { NATIVE_WINDOW_DESTROYED, window };
  putEventSynchronously(&event);
}

static void onInputQueueCreated(ANativeActivity* activity, AInputQueue* queue) {
  LOGV("onInputQueueCreated called");
  NativeActivityQueueEvent event = { INPUT_QUEUE_CREATED, queue };
  putEventSynchronously(&event);
}

static void onInputQueueDestroyed(ANativeActivity* activity, AInputQueue* queue) {
  LOGV("onInputQueueDestroyed called");
  NativeActivityQueueEvent event = { INPUT_QUEUE_DESTROYED, queue };
  putEventSynchronously(&event);
}

// ANativeActivity_onCreate.
extern "C" void Konan_main(ANativeActivity* activity, void* savedState, size_t savedStateSize) {
  nativeActivityState = {activity, savedState, savedStateSize};

  activity->callbacks->onDestroy = onDestroy;
  activity->callbacks->onStart = onStart;
  activity->callbacks->onResume = onResume;
  activity->callbacks->onSaveInstanceState = onSaveInstanceState;
  activity->callbacks->onPause = onPause;
  activity->callbacks->onStop = onStop;
  activity->callbacks->onConfigurationChanged = onConfigurationChanged;
  activity->callbacks->onLowMemory = onLowMemory;
  activity->callbacks->onWindowFocusChanged = onWindowFocusChanged;
  activity->callbacks->onNativeWindowCreated = onNativeWindowCreated;
  activity->callbacks->onNativeWindowDestroyed = onNativeWindowDestroyed;
  activity->callbacks->onInputQueueCreated = onInputQueueCreated;
  activity->callbacks->onInputQueueDestroyed = onInputQueueDestroyed;

  runKonan_start();
}

#endif
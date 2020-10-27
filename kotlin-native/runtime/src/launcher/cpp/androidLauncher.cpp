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

#include "KString.h"
#include "Memory.h"
#include "Natives.h"
#include "Porting.h"
#include "Runtime.h"
#include "Types.h"

#ifdef KONAN_ANDROID

#include <unistd.h>
#include <pthread.h>
#include <sys/errno.h>
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

namespace {

typedef struct {
    int pipeC;
    int pipeKonan;
    NativeActivityState nativeActivityState;
} LauncherState;

LauncherState* launcherState = nullptr;

}

extern "C" void getNativeActivityState(NativeActivityState* state) {
  state->activity = launcherState->nativeActivityState.activity;
  state->savedState = launcherState->nativeActivityState.savedState;
  state->savedStateSize = launcherState->nativeActivityState.savedStateSize;
  state->looper = launcherState->nativeActivityState.looper;
}

extern "C" void notifySysEventProcessed() {
  int8_t message;
  write(launcherState->pipeKonan, &message, sizeof(message));
}

namespace {

void launchMain() {
  Kotlin_initRuntimeIfNeeded();

  {
    ObjHolder args;
    AllocArrayInstance(theArrayTypeInfo, 0, args.slot());
    Konan_start(args.obj());
  }

  Kotlin_deinitRuntimeIfNeeded();
}

void* entry(void* param) {
  ALooper* looper = ALooper_prepare(ALOOPER_PREPARE_ALLOW_NON_CALLBACKS);
  ALooper_addFd(looper, launcherState->pipeKonan, LOOPER_ID_SYS, ALOOPER_EVENT_INPUT, NULL, NULL);
  launcherState->nativeActivityState.looper = looper;

  launchMain();

  return nullptr;
}

void runKonan_start(bool startThread) {
  if (!startThread) {
     launchMain();
     return;
  }

  int pipes[2];
  if (socketpair(AF_UNIX, SOCK_STREAM, 0, pipes)) {
    LOGE("Could not create pipe: %s", strerror(errno));
    return;
  }
  launcherState->pipeC = pipes[0];
  launcherState->pipeKonan = pipes[1];

  LOGE("runKonan_start() %d %d", launcherState->pipeC, launcherState->pipeKonan);

  pthread_attr_t attr;
  pthread_attr_init(&attr);
  pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
  pthread_t thread;
  pthread_create(&thread, &attr, entry, nullptr);
}

void putEventSynchronously(void* event) {
  auto value = reinterpret_cast<uintptr_t>(event);

  if (write(launcherState->pipeC, &value, sizeof(value)) != sizeof(value)) {
    LOGE("Failure writing event: %s\n", strerror(errno));
  }
  int8_t response;
  if (read(launcherState->pipeC, &response, sizeof(response)) != sizeof(response)) {
    LOGE("Failure reading response: %s\n", strerror(errno));
  }
}

void onDestroy(ANativeActivity* activity) {
  LOGV("onDestroy called");
  NativeActivityEvent event = { DESTROY };
  putEventSynchronously(&event);
}

void onStart(ANativeActivity* activity) {
  LOGV("onStart called");
  NativeActivityEvent event = { START };
  putEventSynchronously(&event);
}

void onResume(ANativeActivity* activity) {
  LOGV("onResume called");
  NativeActivitySaveStateEvent event = { RESUME, nullptr, 0 };
  putEventSynchronously(&event);
}

void* onSaveInstanceState(ANativeActivity* activity, size_t* outLen) {
  LOGV("onSaveInstanceState called");
  NativeActivitySaveStateEvent event = { SAVE_INSTANCE_STATE, nullptr, 0 };
  putEventSynchronously(&event);
  *outLen = event.savedStateSize;
  return event.savedState;
}

void onPause(ANativeActivity* activity) {
  LOGV("onPause called");
  NativeActivityEvent event = { PAUSE };
  putEventSynchronously(&event);
}

void onStop(ANativeActivity* activity) {
  LOGV("onStop called");
  NativeActivityEvent event = { STOP };
  putEventSynchronously(&event);
}

void onConfigurationChanged(ANativeActivity* activity) {
  LOGV("onConfigurationChanged called");
  NativeActivityEvent event = { CONFIGURATION_CHANGED };
  putEventSynchronously(&event);
}

void onLowMemory(ANativeActivity* activity) {
  LOGV("onLowMemory called");
  NativeActivityEvent event = { LOW_MEMORY };
  putEventSynchronously(&event);
}

void onWindowFocusChanged(ANativeActivity* activity, int focused) {
  LOGV("onWindowFocusChanged called");
  NativeActivityEvent event = { focused ? WINDOW_GAINED_FOCUS : WINDOW_LOST_FOCUS };
  putEventSynchronously(&event);
}

void onNativeWindowCreated(ANativeActivity* activity, ANativeWindow* window) {
  LOGV("onNativeWindowCreated called");
  NativeActivityWindowEvent event = { NATIVE_WINDOW_CREATED, window };
  putEventSynchronously(&event);
}

void onNativeWindowDestroyed(ANativeActivity* activity, ANativeWindow* window) {
  LOGV("onNativeWindowDestroyed called");
  NativeActivityWindowEvent event = { NATIVE_WINDOW_DESTROYED, window };
  putEventSynchronously(&event);
}

void onInputQueueCreated(ANativeActivity* activity, AInputQueue* queue) {
  LOGV("onInputQueueCreated called");
  NativeActivityQueueEvent event = { INPUT_QUEUE_CREATED, queue };
  putEventSynchronously(&event);
}

void onInputQueueDestroyed(ANativeActivity* activity, AInputQueue* queue) {
  LOGV("onInputQueueDestroyed called");
  NativeActivityQueueEvent event = { INPUT_QUEUE_DESTROYED, queue };
  putEventSynchronously(&event);
}
}

extern "C" void RUNTIME_USED Konan_main(
    ANativeActivity* activity, void* savedState, size_t savedStateSize) {
  bool launchThread = activity->instance == nullptr;
  if (launchThread) {
    launcherState = (LauncherState*)konan::calloc(sizeof(LauncherState), 1);
    launcherState->nativeActivityState = {activity, savedState, savedStateSize, nullptr};
    activity->instance = launcherState;
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
  } else {
    launcherState = (LauncherState*)activity->instance;
  }

  runKonan_start(launchThread);
}

#endif // KONAN_ANDROID

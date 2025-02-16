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

#include "CallsChecker.hpp"
#include "Porting.h"
#include "Types.h"

using namespace kotlin;

extern "C" {

KLong Kotlin_system_getSteadyTimeMillis() {
  // Should complete relatively fast.
  CallsCheckerIgnoreGuard guard;
  return konan::getTimeMillis();
}

KLong Kotlin_system_getSteadyTimeNanos() {
  // Should complete relatively fast.
  CallsCheckerIgnoreGuard guard;
  return konan::getTimeNanos();
}

KLong Kotlin_system_getSteadyTimeMicros() {
  // Should complete relatively fast.
  CallsCheckerIgnoreGuard guard;
  return konan::getTimeMicros();
}

KLong Kotlin_system_getSystemTimeNanos() {
  // Should complete relatively fast.
  CallsCheckerIgnoreGuard guard;
  return konan::getSystemTimeNanos();
}

}  // extern "C"

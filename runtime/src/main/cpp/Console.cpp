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

#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

#include <string>

#include "Assert.h"
#include "Memory.h"
#include "Natives.h"
#include "KString.h"
#include "Types.h"

#include "utf8.h"

#ifdef KONAN_ANDROID
#include <android/log.h>
#endif

extern "C" {

// io/Console.kt
void Kotlin_io_Console_print(KString message) {
  RuntimeAssert(message->type_info() == theStringTypeInfo, "Must use a string");
  // TODO: system stdout must be aware about UTF-8.
  const KChar* utf16 = CharArrayAddressOfElementAt(message, 0);
  std::string utf8;
  utf8::utf16to8(utf16, utf16 + message->count_, back_inserter(utf8));
#ifdef KONAN_ANDROID
  __android_log_print(ANDROID_LOG_INFO, "Konan_main", "%s", utf8.c_str());
#else
  write(STDOUT_FILENO, utf8.c_str(), utf8.size());
#endif
}

void Kotlin_io_Console_println(KString message) {
  Kotlin_io_Console_print(message);
  Kotlin_io_Console_println0();
}

void Kotlin_io_Console_println0() {
#ifdef KONAN_ANDROID
  __android_log_print(ANDROID_LOG_INFO, "Konan_main", "\n");
#else
  write(STDOUT_FILENO, "\n", 1);
#endif
}

OBJ_GETTER0(Kotlin_io_Console_readLine) {
  char data[4096];
  if (!fgets(data, sizeof(data) - 1, stdin)) {
    return nullptr;
  }
  RETURN_RESULT_OF(CreateStringFromCString, data);
}

} // extern "C"
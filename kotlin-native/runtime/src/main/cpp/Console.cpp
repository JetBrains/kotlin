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
#include "KAssert.h"
#include "Memory.h"
#include "Natives.h"
#include "KString.h"
#include "Porting.h"
#include "Types.h"
#include "Exceptions.h"

#include "utf8.h"

extern "C" {

// io/Console.kt
void Kotlin_io_Console_print(KString message) {
    if (message->type_info() != theStringTypeInfo) {
        ThrowClassCastException(message->obj(), theStringTypeInfo);
    }
    // TODO: system stdout must be aware about UTF-8.
    const KChar* utf16 = CharArrayAddressOfElementAt(message, 0);
    KStdString utf8;
    utf8.reserve(message->count_);
    // Replace incorrect sequences with a default codepoint (see utf8::with_replacement::default_replacement)
    utf8::with_replacement::utf16to8(utf16, utf16 + message->count_, back_inserter(utf8));
    konan::consoleWriteUtf8(utf8.c_str(), utf8.size());
}

void Kotlin_io_Console_println(KString message) {
    Kotlin_io_Console_print(message);
#ifndef KONAN_ANDROID
    // On Android single print produces logcat entry, so no need in linefeed.
    Kotlin_io_Console_println0();
#endif
}

void Kotlin_io_Console_println0() {
    konan::consoleWriteUtf8("\n", 1);
}

OBJ_GETTER0(Kotlin_io_Console_readLine) {
    char data[4096];
    int32_t result;
    {
        kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
        result = konan::consoleReadUtf8(data, sizeof(data));
    }
    if (result < 0) {
        RETURN_OBJ(nullptr);
    }
    RETURN_RESULT_OF(CreateStringFromCString, data);
}

} // extern "C"

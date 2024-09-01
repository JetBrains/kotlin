/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

#include <stdio.h>
#include <stdarg.h>

#if KONAN_MACOSX || KONAN_IOS || KONAN_TVOS || KONAN_WATCHOS || KONAN_ANDROID
#include <stdlib.h>
#elif KONAN_LINUX
#include <unistd.h>
#include <sys/syscall.h>
#include <errno.h>
#elif KONAN_WINDOWS
#include <windows.h>
#include <bcrypt.h>
#include <ntdef.h>
#endif

#include "KString.h"
#include "Exceptions.h"
#include "Format.h"
#include "Memory.h"
#include "Natives.h"

namespace {

#if KONAN_LINUX || KONAN_WINDOWS
void throwReadingRandomBytesFailed(const char* format, ...) {
    va_list args;
    va_start(args, format);
    std::array<char, 128> buffer;
    kotlin::std_support::span<char> span(buffer);
    span = kotlin::VFormatToSpan(span, format, args);
    va_end(args);

    ObjHolder holder;
    StringFromUtf8Buffer(buffer.data(), buffer.size() - span.size(), holder.slot());
    ThrowIllegalStateExceptionWithMessage(holder.obj());
}
#endif

}  // namespace


extern "C" {

// Mostly taken from kotlin-native/runtime/src/mimalloc/c/random.c
void Kotlin_Uuid_getRandomBytes(KRef byteArray, KInt size) {
    kotlin::ThreadStateGuard guard(kotlin::ThreadState::kNative);
    ArrayHeader* array = byteArray->array();
    uint8_t* address = reinterpret_cast<uint8_t*>(ByteArrayAddressOfElementAt(array, 0));

#if KONAN_MACOSX || KONAN_IOS || KONAN_TVOS || KONAN_WATCHOS || KONAN_ANDROID
    arc4random_buf(address, size);
#elif KONAN_LINUX
    long count = 0;
    while (count < size) {
        long ret = syscall(SYS_getrandom, address + count, size - count, 0); // blocking
        if (ret >= 0) {
            count += ret;
        } else if (errno != EINTR) { // repeat if interrupted
            throwReadingRandomBytesFailed("getrandom returned a negative value: %ld, errno: %d", ret, errno);
        }
    }
#elif KONAN_WINDOWS
    NTSTATUS status = BCryptGenRandom(NULL, (PUCHAR)address, (ULONG)size, BCRYPT_USE_SYSTEM_PREFERRED_RNG);
    if (!NT_SUCCESS(status)) {
        throwReadingRandomBytesFailed("Unexpected failure in random bytes generation: %ld", status);
    }
#else
#error "How to Kotlin_Uuid_getRandomBytes()?"
#endif
}

}  // extern "C"
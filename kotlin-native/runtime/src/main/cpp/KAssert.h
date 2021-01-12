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

#ifndef RUNTIME_ASSERT_H
#define RUNTIME_ASSERT_H

#include "Common.h"

// To avoid cluttering optimized code with asserts, they could be turned off.
#define KONAN_ENABLE_ASSERT 1

#define STRINGIFY(x) #x
#define TOSTRING(x) STRINGIFY(x)

#if KONAN_ENABLE_ASSERT
#define CURRENT_SOURCE_LOCATION __FILE__ ":" TOSTRING(__LINE__)
#else
// Do not generate location strings, when asserts are disabled to reduce code size.
#define CURRENT_SOURCE_LOCATION nullptr
#endif

RUNTIME_NORETURN void RuntimeAssertFailed(const char* location, const char* message, ...);

namespace internal {

inline RUNTIME_NORETURN void TODOImpl(const char* location) {
    RuntimeAssertFailed(location, "Unimplemented");
}

// TODO: Support format string when `RuntimeAssertFailed` supports it.
inline RUNTIME_NORETURN void TODOImpl(const char* location, const char* message) {
    RuntimeAssertFailed(location, message);
}

} // namespace internal

// During codegeneration we set this constant to 1 or 0 to allow bitcode optimizer
// to get rid of code behind condition.
extern "C" const int KonanNeedDebugInfo;

#if KONAN_ENABLE_ASSERT
// Use RuntimeAssert() in internal state checks, which could be ignored in production.
#define RuntimeAssert(condition, format, ...) \
    do { \
        if (KonanNeedDebugInfo && (!(condition))) { \
            RuntimeAssertFailed(CURRENT_SOURCE_LOCATION, format, ##__VA_ARGS__); \
        } \
    } while (false)
#else
#define RuntimeAssert(condition, format, ...) \
    do { \
    } while (false)
#endif

// Use RuntimeCheck() in runtime checks that could fail due to external condition and shall lead
// to program termination. Never compiled out.
// TODO: Consider using `CURRENT_SOURCE_LOCATION` when `KonanNeedDebugInfo` is `true`.
#define RuntimeCheck(condition, format, ...) \
    do { \
        if (!(condition)) { \
            RuntimeAssertFailed(nullptr, format, ##__VA_ARGS__); \
        } \
    } while (false)

#define TODO(...) \
    do { \
        ::internal::TODOImpl(CURRENT_SOURCE_LOCATION, ##__VA_ARGS__); \
    } while (false)

#endif // RUNTIME_ASSERT_H

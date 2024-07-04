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
#include "CompilerConstants.hpp"
#include "std_support/TypeTraits.hpp"

#define STRINGIFY(x) #x
#define TOSTRING(x) STRINGIFY(x)

#define CURRENT_SOURCE_LOCATION __FILE__ ":" TOSTRING(__LINE__)

namespace kotlin {
namespace internal {

void RuntimeAssertFailedLog(bool allowStacktrace, const char* location, const char* format, ...) __attribute__((format(printf, 3, 4)));
RUNTIME_NORETURN void RuntimeAssertFailedPanic(bool allowStacktrace, const char* location, const char* format, ...) __attribute__((format(printf, 3, 4)));

inline RUNTIME_NORETURN void TODOImpl(const char* location) {
    RuntimeAssertFailedPanic(true, location, "Unimplemented");
}

// TODO: Support format string when `RuntimeAssertFailed` supports it.
inline RUNTIME_NORETURN void TODOImpl(const char* location, const char* message) {
    RuntimeAssertFailedPanic(true, location, "%s", message);
}

} // namespace internal
} // namespace kotlin

// Use RuntimeAssert() in internal state checks, which could be ignored in production.
#define RuntimeAssert(condition, format, ...) \
    do {                                      \
        if (!::kotlin::std_support::is_constant_evaluated() || !(condition)) { \
            switch (::kotlin::compiler::runtimeAssertsMode()) { \
                case ::kotlin::compiler::RuntimeAssertsMode::kIgnore: break; \
                case ::kotlin::compiler::RuntimeAssertsMode::kLog: \
                    if (!(condition)) { \
                        ::kotlin::internal::RuntimeAssertFailedLog(true, CURRENT_SOURCE_LOCATION, format, ##__VA_ARGS__); \
                    } \
                    break; \
                case ::kotlin::compiler::RuntimeAssertsMode::kPanic: \
                    if (!(condition)) { \
                        ::kotlin::internal::RuntimeAssertFailedPanic(true, CURRENT_SOURCE_LOCATION, format, ##__VA_ARGS__); \
                    } \
                    break; \
            } \
        } \
    } while (false)

// Use RuntimeCheck() in runtime checks that could fail due to external condition and shall lead
// to program termination. Never compiled out.
// TODO: Consider using `CURRENT_SOURCE_LOCATION` and stacktraces when `kotlin::compiler::runtimeAssertsMode()` is not `kIgnore`.
#define RuntimeCheck(condition, format, ...) \
    do { \
        if (!(condition)) { \
            ::kotlin::internal::RuntimeAssertFailedPanic(false, nullptr, format, ##__VA_ARGS__); \
        } \
    } while (false)

#define TODO(...) \
    do { \
        ::kotlin::internal::TODOImpl(CURRENT_SOURCE_LOCATION, ##__VA_ARGS__); \
    } while (false)

// Use RuntimeFail() to unconditionally fail, signifying compiler/runtime bug.
// TODO: Consider using `CURRENT_SOURCE_LOCATION` and stacktraces when `kotlin::compiler::runtimeAssertsMode()` is not `kIgnore`.
#define RuntimeFail(format, ...) \
    do { \
        ::kotlin::internal::RuntimeAssertFailedPanic(false, nullptr, format, ##__VA_ARGS__); \
    } while (false)

#endif // RUNTIME_ASSERT_H

/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "CallsChecker.hpp"

#include <atomic>
#include <string_view>
#include <cstring>
#include <unordered_set>

#if KONAN_APPLE
#include <objc/message.h>
#include <objc/runtime.h>
#endif

#include "KAssert.h"
#include "Memory.h"
#include "Porting.h"
#include "StackTrace.hpp"
#include "mm/ThreadData.hpp"
#include "ExecFormat.h"
#include "concurrent/Mutex.hpp"

using namespace kotlin;

// Defined by ModuleCallsCheckerPass.
extern "C" const char** const Kotlin_callsChecker_goodFunctionNamesSorted;
extern "C" const int64_t Kotlin_callsChecker_goodFunctionNamesSize;

namespace {

class KnownFunctionChecker : private Pinned {
public:
    KnownFunctionChecker() :
        goodFunctions_(
                Kotlin_callsChecker_goodFunctionNamesSorted,
                Kotlin_callsChecker_goodFunctionNamesSorted + Kotlin_callsChecker_goodFunctionNamesSize) {}

    ~KnownFunctionChecker() = delete;

    bool isKnown(const void* fun) const noexcept {
        // This is a hot path, but we have already tried to read another atomic, so this
        // shouldn't add too much overhead.
        if (!sealed_.load(std::memory_order_relaxed)) {
            PrintStackTraceStderr();
            RuntimeFail("isKnown is called before the checker is sealed");
        }
        return known_functions_.find(fun) != known_functions_.end();
    }

    // Shared with CallsCheckerPass in libllvmext.
    bool isSafeByName(std::string_view name) const noexcept {
        auto it = std::lower_bound(std::begin(goodFunctions_), std::end(goodFunctions_), name);
        auto check = [&](std::string_view banned) {
            if (banned.back() != '*') {
                return banned == name;
            }
            return name.substr(0, banned.size() - 1) == banned.substr(0, banned.size() - 1);
        };
        if (it != std::end(goodFunctions_) && check(*it)) {
            return true;
        }
        if (it != std::begin(goodFunctions_) && check(*std::prev(it))) {
            return true;
        }
        return false;
    }

    void insert(std_support::span<const void*> knownFunctions) noexcept {
        std::unique_lock guard(initLock_);
        if (sealed_.load(std::memory_order_relaxed)) {
            PrintStackTraceStderr();
            RuntimeFail(
                    "Using CallsChecker with dynamically loaded modules is unsupported: all known functions must be registered before "
                    "GlobalData is created.");
        }
        known_functions_.insert(knownFunctions.begin(), knownFunctions.end());
    }

    void seal() noexcept {
        std::unique_lock guard(initLock_);
        sealed_.store(true, std::memory_order_relaxed);
    }

private:
    SpinLock initLock_; // may be used very early on the start, safer to use a simple spin lock.
    std::atomic<bool> sealed_ = false;
    std::unordered_set<const void*> known_functions_;
    std::vector<std::string_view> goodFunctions_;
};

// This can't be just a part of `GlobalData`, because this may be called very early from
// global constructors.
// For the same reason, it can't just be a simple global: if some global constructor
// used this global before the global initialized, we have UB. Using lazy initialization
// avoid this problem.
static KnownFunctionChecker& knownFunctionChecker() noexcept {
    [[clang::no_destroy]] static KnownFunctionChecker instance;
    return instance;
}

thread_local size_t ignoreGuardsCount = 0;

/**
 * This function calls is inserted to llvm bitcode automatically, so it can be called almost anywhre.
 *
 * Although, function itself is excluded, it can call itself indirectly, from other called functions.
 * Because of this, thread_local guard is used to avoid recursive calls.
 *
 * Unfortunately, function can be called in thread constructors or destructors, where thread local data
 * should not be accessed. So before guard checking we need to check is thread destructor is running,
 * which requires special handling of recursive calls from this check.
 */
NO_EXTERNAL_CALLS_CHECK void checkStateAtCall(
        void* calledPtr,
        const char* calledName,
        const char* callerName,
        std::string (*callSiteDescription)(const char*)) noexcept {
    if (ignoreGuardsCount != 0) return;
    if (konan::isOnThreadExitNotSetOrAlreadyStarted()) return;
    if (!mm::IsCurrentThreadRegistered()) return;
    CallsCheckerIgnoreGuard recursiveGuard;

    auto actualState = GetThreadState();
    if (actualState == ThreadState::kNative) {
        return;
    }

    auto& checker = knownFunctionChecker();
    if (calledPtr && checker.isKnown(calledPtr)) {
        return;
    }

    char buf[200];
    if (!calledName) {
        ptrdiff_t unused;
        if (AddressToSymbol(calledPtr, buf, sizeof(buf), unused)) {
            calledName = buf;
        } else {
            calledName = "unknown function";
        }
    }

    if (checker.isSafeByName(calledName)) {
        return;
    }

    std::string descriptionStorage;
    const char* description = callerName;
    if (callSiteDescription) {
        descriptionStorage = callSiteDescription(callerName);
        description = descriptionStorage.c_str();
    }
    PrintStackTraceStderr();
    RuntimeFail("Expected kNative thread state at call of function %s by function %s", calledName, description);
}

} // namespace

extern "C" RUNTIME_NOTHROW void Kotlin_callsChecker_check(
        const char* callerName, const char* calledName, void* calledPtr) noexcept {
    checkStateAtCall(calledPtr, calledName, callerName, nullptr);
}

extern "C" RUNTIME_NOTHROW void Kotlin_callsChecker_checkMsgSend(const char* callerName, void* obj, void* selector) {
#if KONAN_APPLE
    if (obj == nullptr) {
        // objc_sendMsg called on nil, it does nothing, so it's ok
        return;
    }
    void* calledPtr = reinterpret_cast<void*>(
            class_getMethodImplementation(object_getClass(reinterpret_cast<id>(obj)), reinterpret_cast<SEL>(selector)));
    checkStateAtCall(calledPtr, nullptr, callerName, [](const char* callerName) noexcept {
        std::ostringstream os;
        os << callerName << " (over objc_msgSend)";
        return os.str();
    });
#endif
}

extern "C" RUNTIME_NOTHROW void Kotlin_callsChecker_checkMsgSendSuper2(const char* callerName, void* super, void* selector) {
#if KONAN_APPLE
    void* calledPtr = reinterpret_cast<void*>(class_getMethodImplementation(
            class_getSuperclass(reinterpret_cast<objc_super*>(super)->super_class), reinterpret_cast<SEL>(selector)));
    checkStateAtCall(calledPtr, nullptr, callerName, [](const char* callerName) noexcept {
        std::ostringstream os;
        os << callerName << " (over objc_msgSendSuper2)";
        return os.str();
    });
#endif
}

// Called from global constructors.
extern "C" RUNTIME_NOTHROW RUNTIME_EXPORT void Kotlin_callsChecker_init(const void** knownFunctions, uint64_t knownFunctionsCount) {
    knownFunctionChecker().insert({knownFunctions, static_cast<size_t>(knownFunctionsCount)});
}

ALWAYS_INLINE CallsCheckerIgnoreGuard::CallsCheckerIgnoreGuard() noexcept {
    ++ignoreGuardsCount;
}
ALWAYS_INLINE CallsCheckerIgnoreGuard::~CallsCheckerIgnoreGuard() {
    --ignoreGuardsCount;
}

CallsChecker::CallsChecker() noexcept {
    knownFunctionChecker().seal();
}

CallsChecker::~CallsChecker() = default;

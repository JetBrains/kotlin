/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "CallsChecker.hpp"

#include <atomic>
#include <string_view>
#include <cstring>
#include <unordered_set>

#include "KAssert.h"
#include "Memory.h"
#include "Porting.h"
#include "StackTrace.hpp"
#include "ThreadData.hpp"
#include "ExecFormat.h"
#include "concurrent/Mutex.hpp"

using namespace kotlin;

extern const char** Kotlin_callsChecker_goodFunctionNamesSorted;
extern const uint64_t Kotlin_callsChecker_goodFunctionNamesSize;

namespace {

class KnownFunctionChecker : private MoveOnly {
public:
    KnownFunctionChecker() {
        std_support::span<const char*> goodFunctionNamesSorted(
                Kotlin_callsChecker_goodFunctionNamesSorted, static_cast<size_t>(Kotlin_callsChecker_goodFunctionNamesSize));
        std::transform(goodFunctionNamesSorted.begin(), goodFunctionNamesSorted.end(), std::back_inserter(goodNames_), [](const char* s) {
            return s;
        });
    }

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

    bool isSafeByName(std::string_view name) const noexcept {
        auto it = std::lower_bound(goodNames_.begin(), goodNames_.end(), name);
        auto check = [&](std::string_view banned) {
            if (banned.back() != '*') {
                return banned == name;
            }
            return name.substr(0, banned.size() - 1) == banned.substr(0, banned.size() - 1);
        };
        if (it != goodNames_.end() && check(*it)) {
            return true;
        }
        if (it != goodNames_.begin() && check(*std::prev(it))) {
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
    std::vector<std::string_view> goodNames_; // sorted
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

constexpr int MSG_SEND_TO_NULL = -1;
constexpr int CALLED_LLVM_BUILTIN = -2;

thread_local size_t ignoreGuardsCount = 0;

} // namespace

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
extern "C" RUNTIME_NOTHROW RUNTIME_NODEBUG void Kotlin_mm_checkStateAtExternalFunctionCall(
        const char* caller, const char* callee, const void* calleePtr) noexcept {
    if (reinterpret_cast<int64_t>(calleePtr) == MSG_SEND_TO_NULL) return; // objc_sendMsg called on nil, it does nothing, so it's ok
    if (ignoreGuardsCount != 0) return;
    if (konan::isOnThreadExitNotSetOrAlreadyStarted()) return;
    if (!mm::IsCurrentThreadRegistered()) return;
    CallsCheckerIgnoreGuard recursiveGuard;

    auto actualState = GetThreadState();
    if (actualState == ThreadState::kNative) {
        return;
    }

    auto& checker = knownFunctionChecker();
    if (reinterpret_cast<int64_t>(calleePtr) != CALLED_LLVM_BUILTIN && checker.isKnown(calleePtr)) {
        return;
    }

    char buf[200];
    if (callee == nullptr) {
        ptrdiff_t unused;
        if (AddressToSymbol(calleePtr, buf, sizeof(buf), unused)) {
            callee = buf;
        } else {
            callee = "unknown function";
        }
    }

    if (checker.isSafeByName(callee)) {
        return;
    }

    PrintStackTraceStderr();
    RuntimeFail("Expected kNative thread state at call of function %s by function %s", callee, caller);
}

// Called from global constructors.
extern "C" RUNTIME_NOTHROW RUNTIME_EXPORT void Kotlin_callsChecker_init(const void** knownFunctions, uint64_t knownFunctionsCount) {
    knownFunctionChecker().insert({knownFunctions, static_cast<size_t>(knownFunctionsCount)});
}

ALWAYS_INLINE NO_EXTERNAL_CALLS_CHECK CallsCheckerIgnoreGuard::CallsCheckerIgnoreGuard() noexcept {
    ++ignoreGuardsCount;
}
ALWAYS_INLINE NO_EXTERNAL_CALLS_CHECK CallsCheckerIgnoreGuard::~CallsCheckerIgnoreGuard() {
    --ignoreGuardsCount;
}

CallsChecker::CallsChecker() noexcept {
    knownFunctionChecker().seal();
}

CallsChecker::~CallsChecker() = default;

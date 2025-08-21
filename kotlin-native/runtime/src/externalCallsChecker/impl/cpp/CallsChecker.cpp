/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "CallsChecker.hpp"

#include <string_view>
#include <cstring>
#include <unordered_set>

#include "KAssert.h"
#include "Memory.h"
#include "Porting.h"
#include "StackTrace.hpp"
#include "ThreadData.hpp"
#include "ExecFormat.h"

using namespace kotlin;

// this values will be substituted by compiler
extern "C" const void*** Kotlin_callsCheckerKnownFunctions;
extern "C" const int** Kotlin_callsCheckerKnownFunctionsCounts;
extern "C" const int Kotlin_callsCheckerKnownFunctionsCountsCount;

extern "C" const char* Kotlin_callsCheckerGoodFunctionNames[] = {
        "\x01_mprotect",
        "mprotect",
        "posix_memalign",

        "_ZL15_objc_terminatev", // _objc_terminate()
        "_ZNKSt8__detail20_Prime_rehash_policy14_M_need_rehashEmmm", // std::__detail::_Prime_rehash_policy::_M_need_rehash(unsigned long, unsigned long, unsigned long) const
        "_ZNKSt8__detail20_Prime_rehash_policy14_M_need_rehashEyyy", // std::__detail::_Prime_rehash_policy::_M_need_rehash(unsigned long long, unsigned long long, unsigned long long) const
        "_ZNSaIcED2Ev", // std::allocator<char>::~allocator()
        "_ZNSt13exception_ptrC1ERKS_", // std::exception_ptr::exception_ptr(std::exception_ptr const&)
        "_ZNSt13exception_ptrD1Ev", // std::exception_ptr::~exception_ptr()
        "_ZNSt15__exception_ptr13exception_ptrC1ERKS0_", // std::__exception_ptr::exception_ptr::exception_ptr(std::__exception_ptr::exception_ptr const&)
        "_ZNSt15__exception_ptr13exception_ptrD1Ev", // std::__exception_ptr::exception_ptr::~exception_ptr()
        "_ZNSt18condition_variableD1Ev", // std::condition_variable::~condition_variable()
        "_ZNSt3__112__next_primeEm", // std::__1::__next_prime(unsigned long)
        "_ZNSt3__112basic_stringIcNS_11char_traitsIcEENS_9allocatorIcEEE7reserveEm", // std::__1::basic_string<char, std::__1::char_traits<char>, std::__1::allocator<char>>::reserve(unsigned long)
        "_ZNSt3__112basic_stringIcNS_11char_traitsIcEENS_9allocatorIcEEE9push_backEc", // std::__1::basic_string<char, std::__1::char_traits<char>, std::__1::allocator<char>>::push_back(char)
        "_ZNSt3__112basic_stringIcNS_11char_traitsIcEENS_9allocatorIcEEED1Ev", // std::__1::basic_string<char, std::__1::char_traits<char>, std::__1::allocator<char> >::~basic_string()
        "_ZNSt3__16chrono12steady_clock3nowEv", // std::__1::chrono::steady_clock::now()
        "_ZNSt3__19to_stringEi", // std::__1::to_string(int)
        "_ZNSt6chrono3_V212steady_clock3nowEv", // std::chrono::_V2::steady_clock::now()
        "_ZNSt8__detail15_List_node_base7_M_hookEPS0_", // std::__detail::_List_node_base::_M_hook(std::__detail::_List_node_base*)
        "_ZNSt8__detail15_List_node_base9_M_unhookEv", // std::__detail::_List_node_base::_M_unhook()
        "_ZNSt8__detail15_List_node_base11_M_transferEPS0_S1_", // std::__detail::_List_node_base::_M_transfer(std::__detail::_List_node_base*, std::__detail::_List_node_base*)
        "_ZNSt9exceptionD2Ev", // std::exception::~exception()
        "_ZSt17current_exceptionv", // std::current_exception()
        "_ZSt17rethrow_exceptionSt13exception_ptr", // std::rethrow_exception(std::exception_ptr)
        "_ZSt29_Rb_tree_insert_and_rebalancebPSt18_Rb_tree_node_baseS0_RS_", // std::_Rb_tree_insert_and_rebalance(bool, std::_Rb_tree_node_base*, std::_Rb_tree_node_base*, std::_Rb_tree_node_base&)
        "_ZSt9terminatev", // std::terminate()
        "_ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEED1Ev", // std::__cxx11::basic_string<char, std::char_traits<char>, std::allocator<char> >::~basic_string()
        "_ZSt17rethrow_exceptionNSt15__exception_ptr13exception_ptrE", // std::rethrow_exception(std::__exception_ptr::exception_ptr)
        "_ZSt28_Rb_tree_rebalance_for_erasePSt18_Rb_tree_node_baseRS_", // std::_Rb_tree_rebalance_for_erase(std::_Rb_tree_node_base*, std::_Rb_tree_node_base&)
        "_ZN9__gnu_cxx27__verbose_terminate_handlerEv", // __gnu_cxx::__verbose_terminate_handler()
        "_Znwm", // new
        "_Znwy", // operator new(unsigned long long)
        "_ZdlPv", // delete
        "_ZdlPvm", // operator delete(void*, unsigned long)
        "_ZNSt3__16thread20hardware_concurrencyEv", // std::__1::thread::hardware_concurrency()
        "_ZNSt6thread20hardware_concurrencyEv", // std::thread::hardware_concurrency()
        "__mingw_vsnprintf",
        "__cxa_allocate_exception",
        "__cxa_begin_catch",
        "__cxa_end_catch",
        "__cxa_throw",
        "__cxa_rethrow",
        "__memset_chk",

        "abort",
        "acos",
        "acosf",
        "acosh",
        "acoshf",
        "asin",
        "asinf",
        "asinh",
        "asinhf",
        "atan",
        "atanf",
        "atan2",
        "atan2f",
        "atanf",
        "atanh",
        "atanhf",
        "calloc",
        "clock_gettime",
        "cos",
        "cosf",
        "cosh",
        "cosh",
        "coshf",
        "coshf",
        "cbrt",
        "cbrtf",
        "exit",
        "exp",
        "expf",
        "expm1",
        "expm1f",
        "exp10",
        "exp10f",
        "__exp10",
        "__exp10f",
        "free",
        "getrusage",
        "gettimeofday",
        "hypot",
        "hypotf",
        "isinf",
        "isnan",
        "log",
        "logf",
        "log1p",
        "log1pf",
        "log10",
        "log10f",
        "log2",
        "log2f",
        "malloc",
        "memcmp",
        "memmem",
        "mmap",
        "\x01_mmap",
        "munmap",
        "\x01_munmap",
        "nextafter",
        "nextafterf",
        "pow",
        "powf",
        "remainder",
        "remainderf",
        "sin",
        "sinf",
        "sinh",
        "sinhf",
        "snprintf",
        "sqrt",
        "sqrtf",
        "strcmp",
        "strlen",
        "strnlen",
        "tan",
        "tanf",
        "tanh",
        "tanhf",
        "vsnprintf",
        "bcmp",

        "gettid",

        "getenv",
        "setenv",
        "unsetenv",

        "dispatch_async_f",
        "dispatch_once",
        "pthread_equal",
        "pthread_key_create",
        "pthread_once",
        "pthread_main_np",
        "pthread_self",

        "+[NSMethodSignature signatureWithObjCTypes:]",
        "+[NSNull null]",
        "+[NSObject allocWithZone:]",
        "+[NSObject class]",
        "+[NSObject conformsToProtocol:]",
        "+[NSObject isKindOfClass:]",
        "+[NSObject isSubclassOfClass:]",
        "+[NSObject new]",
        "+[NSString stringWithFormat:]",
        "+[NSString stringWithUTF8String:]",
        "-[NSPlaceholderValue initWithBytes:objCType:]",
        "-[NSException name]",
        "-[NSException reason]",
        "-[NSMethodSignature getArgumentTypeAtIndex:]",
        "-[NSMethodSignature methodReturnType]",
        "-[NSMethodSignature numberOfArguments]",
        "-[NSObject class]",
        "-[NSObject conformsToProtocol:]",
        "-[NSObject init]",
        "-[NSObject isKindOfClass:]",
        "-[NSObject retain]",
        "-[NSPlaceholderString initWithBytes:length:encoding:]",
        "-[NSPlaceholderString initWithBytesNoCopy:length:encoding:freeWhenDone:]",
        "-[NSValue init]",
        "-[NSValue pointerValue]",
        "-[__NSCFBoolean boolValue]",
        "-[__NSCFNumber doubleValue]",
        "-[__NSCFNumber floatValue]",
        "-[__NSCFNumber intValue]",
        "-[__NSCFNumber longLongValue]",
        "-[__NSCFNumber objCType]",
        "-[__NSCFString isEqual:]",
        "CFStringCreateCopy",
        "CFStringGetCharacters",
        "CFStringGetLength",
        "CFStringGetFastestEncoding",
        "_Block_copy",
        "_Block_object_assign",
        "class_getName",
        "class_getSuperclass",
        "class_isMetaClass",
        "ivar_getOffset",
        "method_getName",
        "method_getTypeEncoding",
        "objc_alloc",
        "objc_alloc_init",
        "objc_autorelease",
        "objc_autoreleasePoolPush",
        "objc_autoreleaseReturnValue",
        "objc_getAssociatedObject",
        "objc_getClass",
        "objc_getProtocol",
        "objc_lookUpClass",
        "objc_retain",
        "objc_retainAutoreleaseReturnValue",
        "objc_retainBlock",
        "objc_storeWeak",
        "object_getClass",
        "object_isClass",
        "_os_signpost_emit_with_name_impl",
        "os_signpost_enabled",
        "os_signpost_id_make_with_pointer",
        "protocol_getName",

        "llvm.abs.*",
        "llvm.assume",
        "llvm.ceil.*",
        "llvm.copysign.*",
        "llvm.cos.*",
        "llvm.ctlz.*",
        "llvm.ctpop.*",
        "llvm.cttz.*",
        "llvm.dbg.*",
        "llvm.eh.typeid.for",
        "llvm.eh.typeid.for.p0",
        "llvm.exp.*",
        "llvm.exp10.*",
        "llvm.experimental.noalias.scope.decl",
        "llvm.fabs.*",
        "llvm.fabs.*",
        "llvm.floor.*",
        "llvm.fmuladd.*",
        "llvm.instrprof.*",
        "llvm.lifetime.*",
        "llvm.log.*",
        "llvm.log10.*",
        "llvm.log2.*",
        "llvm.memcpy.*",
        "llvm.memmove.*",
        "llvm.memset.*",
        "llvm.objc.autorelease",
        "llvm.objc.autoreleaseReturnValue",
        "llvm.objc.retain",
        "llvm.vector.*",

        // Not used in Runnable state, but this would be ok.
        // If we don't include it to the good functions list, the code generator will emit redundant state check at the callsite,
        // and this would ruin the code: the state check would be inserted between retainAutoreleasedReturnValue and the actual call
        // producing "autoreleased return value", so the latter won't be able to detect the former, and the autorelease elimination
        // won't work.
        "llvm.objc.retainAutoreleasedReturnValue",
        "llvm.objectsize.*",
        "llvm.pow.*",
        "llvm.rint.*",
        "llvm.sin.*",
        "llvm.sinh.*",
        "llvm.cosh.*",
        "llvm.asin.*",
        "llvm.acos.*",
        "llvm.tan.*",
        "llvm.tanh.*",
        "llvm.atan.*",
        "llvm.smax.*",
        "llvm.smin.*",
        "llvm.sqrt.*",
        "llvm.threadlocal.address*",
        "llvm.umax.*",
        "llvm.umin.*",
        "llvm.umul.*",
        "llvm.va_end",
        "llvm.va_start",
        "llvm.x86.avx2.*",
        "llvm.x86.ssse3.*",
        "llvm.x86.sse2.*",
        "llvm.uadd.sat.*",
        "llvm.aarch64.neon.*",

        "SetConsoleOutputCP",
        "SetConsoleCP",
        "QueryPerformanceCounter",
        "VirtualAlloc",
        "FlsSetValue",
        "GetCurrentProcess",
        "GetCurrentThreadId",
        "GetLastError",
        "FlsFree",
        "K32GetProcessMemoryInfo",
        "VirtualFree",
        "madvise",
        "_aligned_free",
        "_aligned_malloc",
};

namespace {

class KnownFunctionChecker {
public:
    KnownFunctionChecker() {
        for (int i = 0; i < Kotlin_callsCheckerKnownFunctionsCountsCount; i++) {
            for (int j = 0; j < *Kotlin_callsCheckerKnownFunctionsCounts[i]; j++) {
                known_functions_.insert(Kotlin_callsCheckerKnownFunctions[i][j]);
            }
        }
        std::copy(
                std::begin(Kotlin_callsCheckerGoodFunctionNames), std::end(Kotlin_callsCheckerGoodFunctionNames),
                std::begin(good_names_copy_));
        std::sort(std::begin(good_names_copy_), std::end(good_names_copy_));
    }

    bool isKnown(const void* fun) const noexcept { return known_functions_.find(fun) != known_functions_.end(); }

    bool isSafeByName(std::string_view name) const noexcept {
        auto it = std::lower_bound(std::begin(good_names_copy_), std::end(good_names_copy_), name);
        auto check = [&](std::string_view banned) {
            if (banned.back() != '*') {
                return banned == name;
            }
            return name.substr(0, banned.size() - 1) == banned.substr(0, banned.size() - 1);
        };
        if (it != std::end(good_names_copy_) && check(*it)) {
            return true;
        }
        if (it != std::begin(good_names_copy_) && check(*std::prev(it))) {
            return true;
        }
        return false;
    }

    ~KnownFunctionChecker() = delete;

private:
    std::unordered_set<const void*> known_functions_;
    std::string_view good_names_copy_[sizeof(Kotlin_callsCheckerGoodFunctionNames) / sizeof(Kotlin_callsCheckerGoodFunctionNames[0])];
};

[[clang::no_destroy]] const KnownFunctionChecker checker;

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

ALWAYS_INLINE CallsCheckerIgnoreGuard::CallsCheckerIgnoreGuard() noexcept {
    ++ignoreGuardsCount;
}
ALWAYS_INLINE CallsCheckerIgnoreGuard::~CallsCheckerIgnoreGuard() {
    --ignoreGuardsCount;
}

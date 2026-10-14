# Calls Checker

External calls checker is a sanitizer-like instrumentation for Kotlin/Native.
It enforces, that all calls made from Kotlin to the native code are done in the native thread state.

## Background

Kotlin/Native uses a tracing GC that runs on a separate thread. To support communication between
the GC thread and the threads running Kotlin code, Kotlin/Native uses safepoints:
* the GC thread requests some action to be performed on a safepoint (e.g. suspend)
* other threads make sure to periodically call safepoints
  * when running Kotlin code, code generator inserts safepoint calls in the entry point and loop back edges of
    each function
  * when running Native code, a special flag is set up, that allows the GC thread to directly interact
    with Kotlin-specific thread data without the need to pause the thread

When the flag is set, we say that the thread is in the native state, and when it's not set - in the runnable
state. It's critical to correctly maintain the thread state:
* running Kotlin code in the native state will lead to data races: the GC thread and the thread itself will
  attempt to read and write into the thread's Kotlin-specific data
* running native code in the runnable state will block the GC thread and can lead to deadlocks:
  our thread is sleeping trying to lock some mutex, the GC thread is waiting for our thread to reach a
  safepoint, and some other thread (that holds the mutex we wan't to lock) is itself waiting for the GC
  thread to finish.

The first problem is tackled by a bunch of runtime assertions.
The Calls Checker instrumentation helps with the second problem.

## Configuration

Kotlin compiler's binary option `checkStateAtExternalCalls` enables the instrumentation.

In [the pass implementation](../../libllvmext/src/main/cpp/Passes/CallsChecker.cpp) there's a list
`GoodFunctionNames` with well-known functions and LLVM intrinsics, that are considered
to always be okay to call in the runnable thread state. These function should be nonblocking
and generally fast.

Alternatively, there's a [runtime API](../main/cpp/CallsChecker.hpp) `CallsCheckerIgnoreGuard`, a RAII
guard that turns instrumentation into no-op under its scope. And `NO_EXTERNAL_CALLS_CHECK` attribute
to skip instrumenting the marked function.

## Implementation details

Implementation consists of 3 parts: 2 [passes](../../libllvmext/src/main/cpp/Passes/CallsChecker.cpp)
(`CallsCheckerPass` and `ModuleCallsCheckerPass`) and [runtime support](impl/cpp/CallsChecker.cpp).

### CallsCheckerPass (kotlin-calls-checker)

The algorithm finds every call (either `call` or `invoke` LLVM instruction) and inspects its target:
* if it's a direct call to a function defined in the current module, skip instrumentation;
  the function is considered a Kotlin function and safe to call in the runnable state
* if it's a direct call to a function with the name matcing `GoodFunctionNames`, skip instrumentation;
  the function is considered a safe function to call in the runnable state (non-blocking and fast)
* if it's a direct call to `objc_msgSend` or `objc_msgSendSuper2`, inject calls to (respectively)
  `Kotlin_callsChecker_checkMsgSend` or `Kotlin_callsChecker_checkMsgSendSuper2` just before the calls
  and pass them the caller function name and the first 2 arguments
* other calls inject `Kotlin_callsChecker_check` just before the call
  and pass it the caller function name, and the name and address of the called function
  * for indirect calls the name is always `nullptr`
  * for LLVM intrinsics the address is always `nullptr` (because the intrinsic might not have an address)
  * for `llvm.objc.retainAutoreleasedReturnValue` the instrumentation is inserted after the call to preserve
    an important optimization in LLVM ObjC handling

### Runtime support

Exposes 3 functions for the `CallsCheckerPass`:
```cpp
void Kotlin_callsChecker_check(const char* callerName, const char* calledName, void* calledPtr);
void Kotlin_callsChecker_checkMsgSend(const char* callerName, void* obj, void* selector);
void Kotlin_callsChecker_checkMsgSendSuper2(const char* callerName, void* super, void* selector);
```
The latter 2 use Obj-C runtime API (`object_getClass`, `class_getMethodImplementation` and
`class_getSuperclass`) to essentially construct `calledPtr` for the former (`calledName` will be `nullptr`).

The main checker uses the following algorithm:
1. if "ignore" flag is set (using `CallsCheckerIgnoreGuard`), return
2. if the thread is not registered with the runtime or its in the native state, return
3. if `calledPtr` is in the list of known functions, return
4. if `calledName` is `nullptr` attempt to find its name from `calledPtr`
5. if `calledName` is found in `GoodFunctionNames`, return
6. otherwise loudly fail printing `callerName`, `calledName` and the stacktrace

`GoodFunctionNames` are expected to be defined in a global `Kotlin_callsChecker_goodFunctionNamesSorted` and
the list of known functions gets incrementally built via calls to `Kotlin_callsChecker_init`.

### ModuleCallsCheckerPass (kotlin-calls-checker-module)

The pass communicates `GoodFunctionNames` and a list of known functions (i.e. defined in the current module)
to the runtime.

`Kotlin_callsChecker_goodFunctionNamesSorted` and `Kotlin_callsChecker_goodFunctionNamesSize`
are defined as `linkonce` global constants to keep exactly one in the final binary.

Known functions are passed in a call to `Kotlin_callsChecker_init` from the global constructor.

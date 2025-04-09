# Implementing Kotlin Suspend Functions Using WebAssembly Stack Switching

## TL;DR
It seems possible to implement Kotlin Suspend Functions using Stack Switching proposal.
Benefits:
* Simpler implementation of suspend funs.
* Size.
* Unclear:
  * Performance.

## Disclaimers
* It's high level overview
* Suspend functions design in Kotlin is highly influenced by JVM
  * one of important goal was ability to make a performant implementation on JVM

## Introduction

### Kotlin Suspend Functions

Suspend functions in Kotlin are special functions that can be paused and resumed at specific suspension points.
They are building block for:
* Kotlin's coroutines ([kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines)).
* Generators like utilities in standard library (`kotlin.sequences.sequence`)

```kotlin
fun foo():Sequence<Any> {
    return sequence { 
        yield(1)
        yieldAll(foo())
    }
}
```

Low-level APIs
[CoroutinesIntrinsics.kt](../../libraries/stdlib/wasm/src/kotlin/coroutines/CoroutinesIntrinsics.kt)
* startCoroutineUninterceptedOrReturn
  * could be finished without suspending and return value
* createCoroutineUnintercepted

[Intrinsics.kt](../../libraries/stdlib/src/kotlin/coroutines/intrinsics/Intrinsics.kt)
* suspendCoroutineUninterceptedOrReturn (intrinsic!)
  * cont identity? should it be preserved?
  * we can create it on demand and write to local
    * creating at the start could simpler
      * and more optimal -- fewer ifs, fewer instructions
* COROUTINE_SUSPENDED

[Continuation.kt](../../libraries/stdlib/src/kotlin/coroutines/Continuation.kt)
* Continuation
* CoroutineContext
* ContinuationInterceptor

Middle-level APIs
[Continuation.kt](../../libraries/stdlib/src/kotlin/coroutines/Continuation.kt)
Implemented on top of low level APIs
* createCoroutine
* startCoroutine
* suspendCoroutine
* coroutineContext (intrinsic!)

Impl details
[CoroutineImpl.kt](../../libraries/stdlib/wasm/src/kotlin/coroutines/CoroutineImpl.kt)
* CoroutineImpl
  [Coroutines.kt](../../libraries/stdlib/wasm/internal/kotlin/wasm/internal/Coroutines.kt)
* startCoroutineUninterceptedOrReturnIntrinsic*

[Coroutines.kt](../../libraries/stdlib/wasm/internal/kotlin/wasm/internal/Coroutines.kt)
* getContinuation
* getCoroutineContext


### WebAssembly Stack Switching

## Implementation

Start from general solution for suspend functions (Continuation & co). 
Later we can consider specialized implementation for sequences.

Low-level APIs


[CoroutinesIntrinsics.kt](../../libraries/stdlib/wasm/src/kotlin/coroutines/CoroutinesIntrinsics.kt)
### startCoroutineUninterceptedOrReturn

```text

fun <T> (suspend () -> T).startCoroutineUninterceptedOrReturn(completion: Continuation<T>): Any? {
  val wasmCont: WasmContinuation = wasm_cont_new(suspend_invoke) // TODO combine to cont.call/resume_ref?
  
  wasm_resume(wasmCont, this/* suspend fun */, completion) 
      on $kont_tag (c: Continuation<?>, newWasmCont: WasmContinuation) {
          c.wasmContinuation = newWasmCont
          return COROUTINE_SUSPENDED
      }
      else (v: Any?) {
          completion.resume()
          return v
      }
}

```

### createCoroutineUnintercepted
More or less is same as know -- creates CoroutineImpl and delegates to `startCoroutineUninterceptedOrReturn` defined above.


[Intrinsics.kt](../../libraries/stdlib/src/kotlin/coroutines/intrinsics/Intrinsics.kt)
### suspendCoroutineUninterceptedOrReturn

```text
suspend inline fun <T> suspendCoroutineUninterceptedOrReturn(crossinline block: (Continuation<T>) -> Any?): T {
    val result = block(currentSuspendContinuation())
    if (result == COROUTINE_SUSPENDED) {
        return wasm_suspend() as T
    }
    return result as T
}

class SuspendContinuation() : Continuation<Any?> {
    private val wasmContinuation: Any? = null
    override val context: CoroutineContext get() = TODO()
    override fun resumeWith(result: Result<T>) {
        val newResult = 
            if (result.isSuccess) {
                wasm_resume(wasmContinuation!!, result.getOrNull()!!)
                    on $kont_tag (c: Continuation<?>, newWasmCont: WasmContinuation) {
                        c.wasmContinuation = newWasmCont
                    }
            } else {
                wasm_resume_throw(wasmContinuation!!, result.exceptionOrNull()!!)
                    on $kont_tag (c: Continuation<?>, newWasmCont: WasmContinuation) {
                        c.wasmContinuation = newWasmCont
                    }
            }
    }
}
```

* cont identity? should it be preserved?
* we can create it on demand and write to local
  * creating at the start could simpler
    * and more optimal -- fewer ifs, fewer instructions

QQ
* what if kotlin continuation is called before suspension?
  * Looks lika a bug, unspecified behavior
  * 

### coroutineContext
Possible implementations:
* pass context throw all suspend functions as additional argument.
* store in a global
  * write on resume ???
  * reset on suspend ???
  * need to save and restore prev value
  * could be fully managed on call site???
  * for multithreading, it should be thread local

### Using `switch` instruction
TODO
* Can we implement same things with switch instruction?
* May a such implementation be more performant? 

## Useful links
* https://kotlinlang.org/docs/coroutines-overview.html
* https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/
* [coroutines-codegen.md](../../compiler/backend/src/org/jetbrains/kotlin/codegen/coroutines/coroutines-codegen.md)

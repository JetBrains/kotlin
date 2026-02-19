@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Foo::class, "4main3FooC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction
import kotlinx.coroutines.*

@ExportedBridge("__root___Foo_init_allocate")
public fun __root___Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Foo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, Foo())
}

@ExportedBridge("__root___closure_returning_flow__TypesOfArguments__U28KotlinCoroutineSupport__KotlinTypedFlow_main_Foo_U29202D_U20Swift_Void__")
public fun __root___closure_returning_flow__TypesOfArguments__U28KotlinCoroutineSupport__KotlinTypedFlow_main_Foo_U29202D_U20Swift_Void__(i: kotlin.native.internal.NativePtr): Unit {
    val __i = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Unit>(i);
        { arg0: kotlinx.coroutines.flow.Flow<Foo> ->
            val _result = kotlinFun(kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
            Unit
        }
    }
    closure_returning_flow(__i)
}

@ExportedBridge("__root___demo")
public fun __root___demo(): kotlin.native.internal.NativePtr {
    val _result = demo()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___flowFoo_get")
public fun __root___flowFoo_get(): kotlin.native.internal.NativePtr {
    val _result = flowFoo
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___produce_flow")
public fun __root___produce_flow(): kotlin.native.internal.NativePtr {
    val _result = produce_flow()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___produce_function")
public fun __root___produce_function(): kotlin.native.internal.NativePtr {
    val _result = produce_function()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___produce_function_typealias")
public fun __root___produce_function_typealias(continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Unit>(continuation);
        { arg0: Function1<Float, Int> ->
            val _result = kotlinFun(kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
            _result
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Unit>(exception);
        { arg0: kotlin.Any? ->
            val _result = kotlinFun(if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
            _result
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    CoroutineScope(__cancellation + Dispatchers.Default).launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            val _result = produce_function_typealias()
            __continuation(_result)
        } catch (error: CancellationException) {
            __cancellation.cancel()
            __exception(null)
            throw error
        } catch (error: Throwable) {
            __exception(error)
        }
    }.alsoCancel(__cancellation)
}

@ExportedBridge("__root___produce_suspend_function")
public fun __root___produce_suspend_function(continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Unit>(continuation);
        { arg0: kotlin.coroutines.SuspendFunction1<Double, Int> ->
            val _result = kotlinFun(kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
            _result
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Unit>(exception);
        { arg0: kotlin.Any? ->
            val _result = kotlinFun(if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
            _result
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    CoroutineScope(__cancellation + Dispatchers.Default).launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            val _result = produce_suspend_function()
            __continuation(_result)
        } catch (error: CancellationException) {
            __cancellation.cancel()
            __exception(null)
            throw error
        } catch (error: Throwable) {
            __exception(error)
        }
    }.alsoCancel(__cancellation)
}

@ExportedBridge("__root___produce_suspend_function_typealias")
public fun __root___produce_suspend_function_typealias(continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Unit>(continuation);
        { arg0: kotlin.coroutines.SuspendFunction1<Float, Long> ->
            val _result = kotlinFun(kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
            _result
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Unit>(exception);
        { arg0: kotlin.Any? ->
            val _result = kotlinFun(if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
            _result
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    CoroutineScope(__cancellation + Dispatchers.Default).launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            val _result = produce_suspend_function_typealias()
            __continuation(_result)
        } catch (error: CancellationException) {
            __cancellation.cancel()
            __exception(null)
            throw error
        } catch (error: Throwable) {
            __exception(error)
        }
    }.alsoCancel(__cancellation)
}

@ExportedBridge("main_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__")
public fun main_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(pointerToBlock: kotlin.native.internal.NativePtr, _1: Int, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = _1
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Int)->Unit>(continuation);
        { arg0: Int ->
            val _result = kotlinFun(arg0)
            _result
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Unit>(exception);
        { arg0: kotlin.Any? ->
            val _result = kotlinFun(if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
            _result
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    CoroutineScope(__cancellation + Dispatchers.Default).launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            val _result = (__pointerToBlock as kotlin.coroutines.SuspendFunction1<Int, Int>).invoke(___1)
            __continuation(_result)
        } catch (error: CancellationException) {
            __cancellation.cancel()
            __exception(null)
            throw error
        } catch (error: Throwable) {
            __exception(error)
        }
    }.alsoCancel(__cancellation)
}

@ExportedBridge("main_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float__")
public fun main_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float__(pointerToBlock: kotlin.native.internal.NativePtr, _1: Float): Int {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = _1
    val _result = (__pointerToBlock as Function1<Float, Int>).invoke(___1)
    return _result
}

@ExportedBridge("main_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Double__")
public fun main_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Double__(pointerToBlock: kotlin.native.internal.NativePtr, _1: Double, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = _1
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Int)->Unit>(continuation);
        { arg0: Int ->
            val _result = kotlinFun(arg0)
            _result
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Unit>(exception);
        { arg0: kotlin.Any? ->
            val _result = kotlinFun(if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
            _result
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    CoroutineScope(__cancellation + Dispatchers.Default).launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            val _result = (__pointerToBlock as kotlin.coroutines.SuspendFunction1<Double, Int>).invoke(___1)
            __continuation(_result)
        } catch (error: CancellationException) {
            __cancellation.cancel()
            __exception(null)
            throw error
        } catch (error: Throwable) {
            __exception(error)
        }
    }.alsoCancel(__cancellation)
}

@ExportedBridge("main_internal_functional_type_caller_SwiftU2EInt64__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float__")
public fun main_internal_functional_type_caller_SwiftU2EInt64__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float__(pointerToBlock: kotlin.native.internal.NativePtr, _1: Float, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = _1
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Long)->Unit>(continuation);
        { arg0: Long ->
            val _result = kotlinFun(arg0)
            _result
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Unit>(exception);
        { arg0: kotlin.Any? ->
            val _result = kotlinFun(if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
            _result
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    CoroutineScope(__cancellation + Dispatchers.Default).launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            val _result = (__pointerToBlock as kotlin.coroutines.SuspendFunction1<Float, Long>).invoke(___1)
            __continuation(_result)
        } catch (error: CancellationException) {
            __cancellation.cancel()
            __exception(null)
            throw error
        } catch (error: Throwable) {
            __exception(error)
        }
    }.alsoCancel(__cancellation)
}

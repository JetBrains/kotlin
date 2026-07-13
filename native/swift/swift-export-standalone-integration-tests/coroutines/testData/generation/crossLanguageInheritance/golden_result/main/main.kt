@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(AsyncAbstractBase::class, "4main17AsyncAbstractBaseC")
@file:kotlin.native.internal.objc.BindClassToObjCName(AsyncBase::class, "4main9AsyncBaseC")
@file:kotlin.native.internal.objc.BindClassToObjCName(AsyncGreeterBase::class, "4main16AsyncGreeterBaseC")
@file:kotlin.native.internal.objc.BindClassToObjCName(AsyncDefaulter::class, "_AsyncDefaulter")
@file:kotlin.native.internal.objc.BindClassToObjCName(AsyncGreeter::class, "_AsyncGreeter")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch as kotlinx_coroutines_launch

@ImportedBridge("AsyncAbstractBase_abstractGreet__reverse_swift")
internal external fun AsyncAbstractBase_abstractGreet__reverse_swift(self: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(AsyncAbstractBase::class, "abstractGreet")
public suspend fun AsyncAbstractBase_abstractGreet__reverse(self: AsyncAbstractBase): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    return awaitSwiftCoroutine { __resume, __cancellation ->
        val __continuation: Function1<kotlin.String, Unit> = { _result -> __resume(kotlin.Result.success(_result)) }
        val __exception: Function1<platform.Foundation.NSError?, Unit> = { _error -> __resume(kotlin.Result.failure(_error?.let(::SwiftException) ?: kotlinx.coroutines.CancellationException("Cancelled using CancellationError in Swift"))) }
        val __continuationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__continuation)
        val __exceptionPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__exception)
        val __cancellationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__cancellation)
        AsyncAbstractBase_abstractGreet__reverse_swift(__self, __continuationPtr, __exceptionPtr, __cancellationPtr)
    }
}

@ImportedBridge("AsyncAbstractBase_concreteGreet__reverse_swift")
internal external fun AsyncAbstractBase_concreteGreet__reverse_swift(self: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(AsyncAbstractBase::class, "concreteGreet")
public suspend fun AsyncAbstractBase_concreteGreet__reverse(self: AsyncAbstractBase): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    return awaitSwiftCoroutine { __resume, __cancellation ->
        val __continuation: Function1<kotlin.String, Unit> = { _result -> __resume(kotlin.Result.success(_result)) }
        val __exception: Function1<platform.Foundation.NSError?, Unit> = { _error -> __resume(kotlin.Result.failure(_error?.let(::SwiftException) ?: kotlinx.coroutines.CancellationException("Cancelled using CancellationError in Swift"))) }
        val __continuationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__continuation)
        val __exceptionPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__exception)
        val __cancellationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__cancellation)
        AsyncAbstractBase_concreteGreet__reverse_swift(__self, __continuationPtr, __exceptionPtr, __cancellationPtr)
    }
}

@ImportedBridge("AsyncBase_count__reverse_swift")
internal external fun AsyncBase_count__reverse_swift(self: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(AsyncBase::class, "count")
public suspend fun AsyncBase_count__reverse(self: AsyncBase): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    return awaitSwiftCoroutine { __resume, __cancellation ->
        val __continuation: Function1<Int, Unit> = { _result -> __resume(kotlin.Result.success(_result)) }
        val __exception: Function1<platform.Foundation.NSError?, Unit> = { _error -> __resume(kotlin.Result.failure(_error?.let(::SwiftException) ?: kotlinx.coroutines.CancellationException("Cancelled using CancellationError in Swift"))) }
        val __continuationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__continuation)
        val __exceptionPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__exception)
        val __cancellationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__cancellation)
        AsyncBase_count__reverse_swift(__self, __continuationPtr, __exceptionPtr, __cancellationPtr)
    }
}

@ImportedBridge("AsyncBase_greet__TypesOfArguments__Swift_String____reverse_swift")
internal external fun AsyncBase_greet__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(AsyncBase::class, "greet")
public suspend fun AsyncBase_greet__TypesOfArguments__Swift_String____reverse(self: AsyncBase, name: kotlin.String): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __name = name.objcPtr()
    return awaitSwiftCoroutine { __resume, __cancellation ->
        val __continuation: Function1<kotlin.String, Unit> = { _result -> __resume(kotlin.Result.success(_result)) }
        val __exception: Function1<platform.Foundation.NSError?, Unit> = { _error -> __resume(kotlin.Result.failure(_error?.let(::SwiftException) ?: kotlinx.coroutines.CancellationException("Cancelled using CancellationError in Swift"))) }
        val __continuationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__continuation)
        val __exceptionPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__exception)
        val __cancellationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__cancellation)
        AsyncBase_greet__TypesOfArguments__Swift_String____reverse_swift(__self, __name, __continuationPtr, __exceptionPtr, __cancellationPtr)
    }
}

@ImportedBridge("AsyncBase_sync__TypesOfArguments__Swift_String____reverse_swift")
internal external fun AsyncBase_sync__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(AsyncBase::class, "sync")
public fun AsyncBase_sync__TypesOfArguments__Swift_String____reverse(self: AsyncBase, name: kotlin.String): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __name = name.objcPtr()
    val _result = AsyncBase_sync__TypesOfArguments__Swift_String____reverse_swift(__self, __name)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("AsyncDefaulter_describe__reverse_swift")
internal external fun AsyncDefaulter_describe__reverse_swift(self: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(AsyncDefaulter::class, "describe")
public suspend fun AsyncDefaulter_describe__reverse(self: AsyncDefaulter): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    return awaitSwiftCoroutine { __resume, __cancellation ->
        val __continuation: Function1<kotlin.String, Unit> = { _result -> __resume(kotlin.Result.success(_result)) }
        val __exception: Function1<platform.Foundation.NSError?, Unit> = { _error -> __resume(kotlin.Result.failure(_error?.let(::SwiftException) ?: kotlinx.coroutines.CancellationException("Cancelled using CancellationError in Swift"))) }
        val __continuationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__continuation)
        val __exceptionPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__exception)
        val __cancellationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__cancellation)
        AsyncDefaulter_describe__reverse_swift(__self, __continuationPtr, __exceptionPtr, __cancellationPtr)
    }
}

@ImportedBridge("AsyncDefaulter_tag__reverse_swift")
internal external fun AsyncDefaulter_tag__reverse_swift(self: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(AsyncDefaulter::class, "tag")
public suspend fun AsyncDefaulter_tag__reverse(self: AsyncDefaulter): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    return awaitSwiftCoroutine { __resume, __cancellation ->
        val __continuation: Function1<kotlin.String, Unit> = { _result -> __resume(kotlin.Result.success(_result)) }
        val __exception: Function1<platform.Foundation.NSError?, Unit> = { _error -> __resume(kotlin.Result.failure(_error?.let(::SwiftException) ?: kotlinx.coroutines.CancellationException("Cancelled using CancellationError in Swift"))) }
        val __continuationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__continuation)
        val __exceptionPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__exception)
        val __cancellationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__cancellation)
        AsyncDefaulter_tag__reverse_swift(__self, __continuationPtr, __exceptionPtr, __cancellationPtr)
    }
}

@ImportedBridge("AsyncGreeterBase_greet__TypesOfArguments__Swift_String____reverse_swift")
internal external fun AsyncGreeterBase_greet__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(AsyncGreeterBase::class, "greet")
public suspend fun AsyncGreeterBase_greet__TypesOfArguments__Swift_String____reverse(self: AsyncGreeterBase, name: kotlin.String): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __name = name.objcPtr()
    return awaitSwiftCoroutine { __resume, __cancellation ->
        val __continuation: Function1<kotlin.String, Unit> = { _result -> __resume(kotlin.Result.success(_result)) }
        val __exception: Function1<platform.Foundation.NSError?, Unit> = { _error -> __resume(kotlin.Result.failure(_error?.let(::SwiftException) ?: kotlinx.coroutines.CancellationException("Cancelled using CancellationError in Swift"))) }
        val __continuationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__continuation)
        val __exceptionPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__exception)
        val __cancellationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__cancellation)
        AsyncGreeterBase_greet__TypesOfArguments__Swift_String____reverse_swift(__self, __name, __continuationPtr, __exceptionPtr, __cancellationPtr)
    }
}

@ImportedBridge("AsyncGreeterBase_salutation__reverse_swift")
internal external fun AsyncGreeterBase_salutation__reverse_swift(self: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(AsyncGreeterBase::class, "salutation")
public suspend fun AsyncGreeterBase_salutation__reverse(self: AsyncGreeterBase): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    return awaitSwiftCoroutine { __resume, __cancellation ->
        val __continuation: Function1<kotlin.String, Unit> = { _result -> __resume(kotlin.Result.success(_result)) }
        val __exception: Function1<platform.Foundation.NSError?, Unit> = { _error -> __resume(kotlin.Result.failure(_error?.let(::SwiftException) ?: kotlinx.coroutines.CancellationException("Cancelled using CancellationError in Swift"))) }
        val __continuationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__continuation)
        val __exceptionPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__exception)
        val __cancellationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__cancellation)
        AsyncGreeterBase_salutation__reverse_swift(__self, __continuationPtr, __exceptionPtr, __cancellationPtr)
    }
}

@ImportedBridge("AsyncGreeter_greet__TypesOfArguments__Swift_String____reverse_swift")
internal external fun AsyncGreeter_greet__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(AsyncGreeter::class, "greet")
public suspend fun AsyncGreeter_greet__TypesOfArguments__Swift_String____reverse(self: AsyncGreeter, name: kotlin.String): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __name = name.objcPtr()
    return awaitSwiftCoroutine { __resume, __cancellation ->
        val __continuation: Function1<kotlin.String, Unit> = { _result -> __resume(kotlin.Result.success(_result)) }
        val __exception: Function1<platform.Foundation.NSError?, Unit> = { _error -> __resume(kotlin.Result.failure(_error?.let(::SwiftException) ?: kotlinx.coroutines.CancellationException("Cancelled using CancellationError in Swift"))) }
        val __continuationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__continuation)
        val __exceptionPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__exception)
        val __cancellationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__cancellation)
        AsyncGreeter_greet__TypesOfArguments__Swift_String____reverse_swift(__self, __name, __continuationPtr, __exceptionPtr, __cancellationPtr)
    }
}

@ImportedBridge("AsyncGreeter_salutation__reverse_swift")
internal external fun AsyncGreeter_salutation__reverse_swift(self: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(AsyncGreeter::class, "salutation")
public suspend fun AsyncGreeter_salutation__reverse(self: AsyncGreeter): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    return awaitSwiftCoroutine { __resume, __cancellation ->
        val __continuation: Function1<kotlin.String, Unit> = { _result -> __resume(kotlin.Result.success(_result)) }
        val __exception: Function1<platform.Foundation.NSError?, Unit> = { _error -> __resume(kotlin.Result.failure(_error?.let(::SwiftException) ?: kotlinx.coroutines.CancellationException("Cancelled using CancellationError in Swift"))) }
        val __continuationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__continuation)
        val __exceptionPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__exception)
        val __cancellationPtr = kotlin.native.internal.ref.createRetainedExternalRCRef(__cancellation)
        AsyncGreeter_salutation__reverse_swift(__self, __continuationPtr, __exceptionPtr, __cancellationPtr)
    }
}

@ExportedBridge("AsyncAbstractBase_abstractGreet")
public fun AsyncAbstractBase_abstractGreet(self: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AsyncAbstractBase
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(continuation);
        { arg0: kotlin.String ->
            val _arg0 = arg0.objcPtr()
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    swiftCoroutine(__continuation, __exception, __cancellation) {
        __self.abstractGreet()
    }
}

@ExportedBridge("AsyncAbstractBase_concreteGreet")
public fun AsyncAbstractBase_concreteGreet(self: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AsyncAbstractBase
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(continuation);
        { arg0: kotlin.String ->
            val _arg0 = arg0.objcPtr()
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    swiftCoroutine(__continuation, __exception, __cancellation) {
        __self.concreteGreet()
    }
}

@ExportedBridge("AsyncAbstractBase_concreteGreet_direct", nonVirtualTargetMethod = "concreteGreet")
public fun AsyncAbstractBase_concreteGreet_direct(self: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AsyncAbstractBase
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(continuation);
        { arg0: kotlin.String ->
            val _arg0 = arg0.objcPtr()
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    swiftCoroutine(__continuation, __exception, __cancellation) {
        __self.concreteGreet()
    }
}

@ExportedBridge("AsyncBase_count")
public fun AsyncBase_count(self: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AsyncBase
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Int)->Boolean>(continuation);
        { arg0: Int ->
            val _arg0 = arg0
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    swiftCoroutine(__continuation, __exception, __cancellation) {
        __self.count()
    }
}

@ExportedBridge("AsyncBase_count_direct", nonVirtualTargetMethod = "count")
public fun AsyncBase_count_direct(self: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AsyncBase
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Int)->Boolean>(continuation);
        { arg0: Int ->
            val _arg0 = arg0
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    swiftCoroutine(__continuation, __exception, __cancellation) {
        __self.count()
    }
}

@ExportedBridge("AsyncBase_greet__TypesOfArguments__Swift_String__")
public fun AsyncBase_greet__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AsyncBase
    val __name = interpretObjCPointer<kotlin.String>(name)
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(continuation);
        { arg0: kotlin.String ->
            val _arg0 = arg0.objcPtr()
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    swiftCoroutine(__continuation, __exception, __cancellation) {
        __self.greet(__name)
    }
}

@ExportedBridge("AsyncBase_greet__TypesOfArguments__Swift_String___direct", nonVirtualTargetMethod = "greet")
public fun AsyncBase_greet__TypesOfArguments__Swift_String___direct(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AsyncBase
    val __name = interpretObjCPointer<kotlin.String>(name)
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(continuation);
        { arg0: kotlin.String ->
            val _arg0 = arg0.objcPtr()
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    swiftCoroutine(__continuation, __exception, __cancellation) {
        __self.greet(__name)
    }
}

@ExportedBridge("AsyncBase_notOpen")
public fun AsyncBase_notOpen(self: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AsyncBase
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(continuation);
        { arg0: kotlin.String ->
            val _arg0 = arg0.objcPtr()
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    swiftCoroutine(__continuation, __exception, __cancellation) {
        __self.notOpen()
    }
}

@ExportedBridge("AsyncBase_sync__TypesOfArguments__Swift_String__")
public fun AsyncBase_sync__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AsyncBase
    val __name = interpretObjCPointer<kotlin.String>(name)
    val _result = run { __self.sync(__name) }
    return _result.objcPtr()
}

@ExportedBridge("AsyncBase_sync__TypesOfArguments__Swift_String___direct", nonVirtualTargetMethod = "sync")
public fun AsyncBase_sync__TypesOfArguments__Swift_String___direct(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AsyncBase
    val __name = interpretObjCPointer<kotlin.String>(name)
    val _result = run { __self.sync(__name) }
    return _result.objcPtr()
}

@ExportedBridge("AsyncDefaulter_describe")
public fun AsyncDefaulter_describe(self: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AsyncDefaulter
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(continuation);
        { arg0: kotlin.String ->
            val _arg0 = arg0.objcPtr()
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    swiftCoroutine(__continuation, __exception, __cancellation) {
        __self.describe()
    }
}

@ExportedBridge("AsyncDefaulter_describe_direct", nonVirtualTargetMethod = "describe")
public fun AsyncDefaulter_describe_direct(self: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AsyncDefaulter
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(continuation);
        { arg0: kotlin.String ->
            val _arg0 = arg0.objcPtr()
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    swiftCoroutine(__continuation, __exception, __cancellation) {
        __self.describe()
    }
}

@ExportedBridge("AsyncDefaulter_tag")
public fun AsyncDefaulter_tag(self: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AsyncDefaulter
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(continuation);
        { arg0: kotlin.String ->
            val _arg0 = arg0.objcPtr()
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    swiftCoroutine(__continuation, __exception, __cancellation) {
        __self.tag()
    }
}

@ExportedBridge("AsyncGreeterBase_greet__TypesOfArguments__Swift_String__")
public fun AsyncGreeterBase_greet__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AsyncGreeterBase
    val __name = interpretObjCPointer<kotlin.String>(name)
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(continuation);
        { arg0: kotlin.String ->
            val _arg0 = arg0.objcPtr()
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    swiftCoroutine(__continuation, __exception, __cancellation) {
        __self.greet(__name)
    }
}

@ExportedBridge("AsyncGreeterBase_greet__TypesOfArguments__Swift_String___direct", nonVirtualTargetMethod = "greet")
public fun AsyncGreeterBase_greet__TypesOfArguments__Swift_String___direct(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AsyncGreeterBase
    val __name = interpretObjCPointer<kotlin.String>(name)
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(continuation);
        { arg0: kotlin.String ->
            val _arg0 = arg0.objcPtr()
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    swiftCoroutine(__continuation, __exception, __cancellation) {
        __self.greet(__name)
    }
}

@ExportedBridge("AsyncGreeterBase_salutation")
public fun AsyncGreeterBase_salutation(self: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AsyncGreeterBase
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(continuation);
        { arg0: kotlin.String ->
            val _arg0 = arg0.objcPtr()
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    swiftCoroutine(__continuation, __exception, __cancellation) {
        __self.salutation()
    }
}

@ExportedBridge("AsyncGreeterBase_salutation_direct", nonVirtualTargetMethod = "salutation")
public fun AsyncGreeterBase_salutation_direct(self: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AsyncGreeterBase
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(continuation);
        { arg0: kotlin.String ->
            val _arg0 = arg0.objcPtr()
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    swiftCoroutine(__continuation, __exception, __cancellation) {
        __self.salutation()
    }
}

@ExportedBridge("AsyncGreeter_greet__TypesOfArguments__Swift_String__")
public fun AsyncGreeter_greet__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AsyncGreeter
    val __name = interpretObjCPointer<kotlin.String>(name)
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(continuation);
        { arg0: kotlin.String ->
            val _arg0 = arg0.objcPtr()
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    swiftCoroutine(__continuation, __exception, __cancellation) {
        __self.greet(__name)
    }
}

@ExportedBridge("AsyncGreeter_salutation")
public fun AsyncGreeter_salutation(self: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr, exception: kotlin.native.internal.NativePtr, cancellation: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AsyncGreeter
    val __continuation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(continuation);
        { arg0: kotlin.String ->
            val _arg0 = arg0.objcPtr()
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __exception = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(exception);
        { arg0: kotlin.Any? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = kotlinFun(_arg0)
            run<Unit> { _result }
        }
    }
    val __cancellation = kotlin.native.internal.ref.dereferenceExternalRCRef(cancellation) as SwiftJob
    swiftCoroutine(__continuation, __exception, __cancellation) {
        __self.salutation()
    }
}

@ExportedBridge("__root___AsyncBase_init_allocate")
public fun __root___AsyncBase_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<AsyncBase>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___AsyncBase_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___AsyncBase_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, AsyncBase()) }
    return run { _result; true }
}

@ExportedBridge("__root___AsyncGreeterBase_init_allocate")
public fun __root___AsyncGreeterBase_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<AsyncGreeterBase>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___AsyncGreeterBase_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___AsyncGreeterBase_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, AsyncGreeterBase()) }
    return run { _result; true }
}

@ExportedBridge("main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__")
public fun main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(pointerToBlock: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = interpretObjCPointer<kotlin.String>(_1)
    val _result = run { (__pointerToBlock as Function1<kotlin.String, Unit>).invoke(___1) }
    return run { _result; true }
}

@ExportedBridge("main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___")
public fun main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = if (_1 == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<platform.Foundation.NSError>(_1)
    val _result = run { (__pointerToBlock as Function1<platform.Foundation.NSError?, Unit>).invoke(___1) }
    return run { _result; true }
}

@ExportedBridge("main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__")
public fun main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(pointerToBlock: kotlin.native.internal.NativePtr, _1: Int): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = _1
    val _result = run { (__pointerToBlock as Function1<Int, Unit>).invoke(___1) }
    return run { _result; true }
}

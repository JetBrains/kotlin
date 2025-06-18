@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(AnyConsumer::class, "4main11AnyConsumerC")
@file:kotlin.native.internal.objc.BindClassToObjCName(IdentityProcessor::class, "4main17IdentityProcessorC")
@file:kotlin.native.internal.objc.BindClassToObjCName(StringProducer::class, "4main14StringProducerC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___customFilter__TypesOfArguments__Swift_Array_Swift_Optional_KotlinRuntime_KotlinBase___U28Swift_Optional_KotlinRuntime_KotlinBase_U29202D_U20Swift_Bool__")
public fun __root___customFilter__TypesOfArguments__Swift_Array_Swift_Optional_KotlinRuntime_KotlinBase___U28Swift_Optional_KotlinRuntime_KotlinBase_U29202D_U20Swift_Bool__(`receiver`: kotlin.native.internal.NativePtr, predicate: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __receiver = interpretObjCPointer<kotlin.collections.List<kotlin.Any?>>(`receiver`)
    val __predicate = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(predicate);
        { arg0: kotlin.Any? ->
            kotlinFun(if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
        }
    }
    val _result = __receiver.customFilter(__predicate)
    return _result.objcPtr()
}

@ExportedBridge("__root___foo__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase__Swift_Optional_KotlinRuntime_KotlinBase___")
public fun __root___foo__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase__Swift_Optional_KotlinRuntime_KotlinBase___(param1: kotlin.native.internal.NativePtr, param2: kotlin.native.internal.NativePtr): Unit {
    val __param1 = if (param1 == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(param1) as kotlin.Any
    val __param2 = if (param2 == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(param2) as kotlin.Any
    foo(__param1, __param2)
}

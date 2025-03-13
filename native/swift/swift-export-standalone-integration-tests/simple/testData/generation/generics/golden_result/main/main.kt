@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(generation.generics.generics.AnyConsumer::class, "22ExportedKotlinPackages10generationO8genericsO8genericsO4mainE11AnyConsumerC")
@file:kotlin.native.internal.objc.BindClassToObjCName(generation.generics.generics.IdentityProcessor::class, "22ExportedKotlinPackages10generationO8genericsO8genericsO4mainE17IdentityProcessorC")
@file:kotlin.native.internal.objc.BindClassToObjCName(generation.generics.generics.StringProducer::class, "22ExportedKotlinPackages10generationO8genericsO8genericsO4mainE14StringProducerC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction
import generation.generics.generics.customFilter as generation_generics_generics_customFilter

@ExportedBridge("generation_generics_generics_customFilter__TypesOfArguments__Swift_Array_Swift_Optional_KotlinRuntime_KotlinBase___U28Swift_Optional_KotlinRuntime_KotlinBase_U29202D_U20Swift_Bool__")
public fun generation_generics_generics_customFilter__TypesOfArguments__Swift_Array_Swift_Optional_KotlinRuntime_KotlinBase___U28Swift_Optional_KotlinRuntime_KotlinBase_U29202D_U20Swift_Bool__(`receiver`: kotlin.native.internal.NativePtr, predicate: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __receiver = interpretObjCPointer<kotlin.collections.List<kotlin.Any?>>(`receiver`)
    val __predicate = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(predicate);
        { arg0: kotlin.Any? ->
            kotlinFun(if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0))
        }
    }
    val _result = __receiver.generation_generics_generics_customFilter(__predicate)
    return _result.objcPtr()
}

@ExportedBridge("generation_generics_generics_foo__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase__Swift_Optional_KotlinRuntime_KotlinBase___")
public fun generation_generics_generics_foo__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase__Swift_Optional_KotlinRuntime_KotlinBase___(param1: kotlin.native.internal.NativePtr, param2: kotlin.native.internal.NativePtr): Unit {
    val __param1 = if (param1 == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(param1) as kotlin.Any
    val __param2 = if (param2 == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(param2) as kotlin.Any
    generation.generics.generics.foo(__param1, __param2)
}

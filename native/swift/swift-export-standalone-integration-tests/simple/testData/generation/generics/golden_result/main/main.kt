@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(generation.generics.generics.AnyConsumer::class, "22ExportedKotlinPackages10generationO8genericsO8genericsO4mainE11AnyConsumerC")
@file:kotlin.native.internal.objc.BindClassToObjCName(generation.generics.generics.Box::class, "22ExportedKotlinPackages10generationO8genericsO8genericsO4mainE3BoxC")
@file:kotlin.native.internal.objc.BindClassToObjCName(generation.generics.generics.CPImpl::class, "22ExportedKotlinPackages10generationO8genericsO8genericsO4mainE6CPImplC")
@file:kotlin.native.internal.objc.BindClassToObjCName(generation.generics.generics.DefaultBox::class, "22ExportedKotlinPackages10generationO8genericsO8genericsO4mainE10DefaultBoxC")
@file:kotlin.native.internal.objc.BindClassToObjCName(generation.generics.generics.Demo::class, "22ExportedKotlinPackages10generationO8genericsO8genericsO4mainE4DemoC")
@file:kotlin.native.internal.objc.BindClassToObjCName(generation.generics.generics.IdentityProcessor::class, "22ExportedKotlinPackages10generationO8genericsO8genericsO4mainE17IdentityProcessorC")
@file:kotlin.native.internal.objc.BindClassToObjCName(generation.generics.generics.Pair::class, "22ExportedKotlinPackages10generationO8genericsO8genericsO4mainE4PairC")
@file:kotlin.native.internal.objc.BindClassToObjCName(generation.generics.generics.StringProducer::class, "22ExportedKotlinPackages10generationO8genericsO8genericsO4mainE14StringProducerC")
@file:kotlin.native.internal.objc.BindClassToObjCName(generation.generics.generics.TripleBox::class, "22ExportedKotlinPackages10generationO8genericsO8genericsO4mainE9TripleBoxC")
@file:kotlin.native.internal.objc.BindClassToObjCName(generation.generics.generics.A::class, "_A")
@file:kotlin.native.internal.objc.BindClassToObjCName(generation.generics.generics.B::class, "_B")
@file:kotlin.native.internal.objc.BindClassToObjCName(generation.generics.generics.Consumer::class, "_Consumer")
@file:kotlin.native.internal.objc.BindClassToObjCName(generation.generics.generics.ConsumerProducer::class, "_ConsumerProducer")
@file:kotlin.native.internal.objc.BindClassToObjCName(generation.generics.generics.Processor::class, "_Processor")
@file:kotlin.native.internal.objc.BindClassToObjCName(generation.generics.generics.Producer::class, "_Producer")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction
import generation.generics.generics.customFilter as generation_generics_generics_customFilter

@ExportedBridge("generation_generics_generics_A_foo_get")
public fun generation_generics_generics_A_foo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.generics.generics.A<kotlin.Any?>
    val _result = __self.foo
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_generics_generics_AnyConsumer_consume__TypesOfArguments__KotlinRuntime_KotlinBase__")
public fun generation_generics_generics_AnyConsumer_consume__TypesOfArguments__KotlinRuntime_KotlinBase__(self: kotlin.native.internal.NativePtr, item: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.generics.generics.AnyConsumer
    val __item = kotlin.native.internal.ref.dereferenceExternalRCRef(item) as kotlin.Any
    __self.consume(__item)
}

@ExportedBridge("generation_generics_generics_AnyConsumer_init_allocate")
public fun generation_generics_generics_AnyConsumer_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<generation.generics.generics.AnyConsumer>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_generics_generics_AnyConsumer_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun generation_generics_generics_AnyConsumer_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, generation.generics.generics.AnyConsumer())
}

@ExportedBridge("generation_generics_generics_B_foo_get")
public fun generation_generics_generics_B_foo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.generics.generics.B<kotlin.Any?>
    val _result = __self.foo
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_generics_generics_Box_t_get")
public fun generation_generics_generics_Box_t_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.generics.generics.Box<kotlin.Any?>
    val _result = __self.t
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_generics_generics_CPImpl_consume__TypesOfArguments__Swift_String__")
public fun generation_generics_generics_CPImpl_consume__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, item: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.generics.generics.CPImpl
    val __item = interpretObjCPointer<kotlin.String>(item)
    __self.consume(__item)
}

@ExportedBridge("generation_generics_generics_CPImpl_init_allocate")
public fun generation_generics_generics_CPImpl_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<generation.generics.generics.CPImpl>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_generics_generics_CPImpl_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun generation_generics_generics_CPImpl_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, generation.generics.generics.CPImpl())
}

@ExportedBridge("generation_generics_generics_Consumer_consume__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___")
public fun generation_generics_generics_Consumer_consume__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self: kotlin.native.internal.NativePtr, item: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.generics.generics.Consumer<kotlin.Any?>
    val __item = if (item == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(item) as kotlin.Any
    __self.consume(__item)
}

@ExportedBridge("generation_generics_generics_DefaultBox_init_allocate")
public fun generation_generics_generics_DefaultBox_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<generation.generics.generics.DefaultBox<kotlin.Any?>>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_generics_generics_DefaultBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_KotlinRuntime_KotlinBase___")
public fun generation_generics_generics_DefaultBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_KotlinRuntime_KotlinBase___(__kt: kotlin.native.internal.NativePtr, t: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __t = if (t == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(t) as kotlin.Any
    kotlin.native.internal.initInstance(____kt, generation.generics.generics.DefaultBox<kotlin.Any?>(__t))
}

@ExportedBridge("generation_generics_generics_Demo_foo_get")
public fun generation_generics_generics_Demo_foo_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.generics.generics.Demo
    val _result = __self.foo
    return _result
}

@ExportedBridge("generation_generics_generics_Demo_init_allocate")
public fun generation_generics_generics_Demo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<generation.generics.generics.Demo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_generics_generics_Demo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun generation_generics_generics_Demo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, generation.generics.generics.Demo())
}

@ExportedBridge("generation_generics_generics_IdentityProcessor_init_allocate")
public fun generation_generics_generics_IdentityProcessor_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<generation.generics.generics.IdentityProcessor<kotlin.Any?>>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_generics_generics_IdentityProcessor_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun generation_generics_generics_IdentityProcessor_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, generation.generics.generics.IdentityProcessor<kotlin.Any?>())
}

@ExportedBridge("generation_generics_generics_IdentityProcessor_process__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___")
public fun generation_generics_generics_IdentityProcessor_process__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self: kotlin.native.internal.NativePtr, input: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.generics.generics.IdentityProcessor<kotlin.Any?>
    val __input = if (input == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(input) as kotlin.Any
    val _result = __self.process(__input)
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_generics_generics_Pair_first_get")
public fun generation_generics_generics_Pair_first_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.generics.generics.Pair<kotlin.Any?, kotlin.Any?>
    val _result = __self.first
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_generics_generics_Pair_init_allocate")
public fun generation_generics_generics_Pair_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<generation.generics.generics.Pair<kotlin.Any?, kotlin.Any?>>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_generics_generics_Pair_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_KotlinRuntime_KotlinBase__Swift_Optional_KotlinRuntime_KotlinBase___")
public fun generation_generics_generics_Pair_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_KotlinRuntime_KotlinBase__Swift_Optional_KotlinRuntime_KotlinBase___(__kt: kotlin.native.internal.NativePtr, first: kotlin.native.internal.NativePtr, second: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __first = if (first == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(first) as kotlin.Any
    val __second = if (second == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(second) as kotlin.Any
    kotlin.native.internal.initInstance(____kt, generation.generics.generics.Pair<kotlin.Any?, kotlin.Any?>(__first, __second))
}

@ExportedBridge("generation_generics_generics_Pair_second_get")
public fun generation_generics_generics_Pair_second_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.generics.generics.Pair<kotlin.Any?, kotlin.Any?>
    val _result = __self.second
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_generics_generics_Processor_process__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___")
public fun generation_generics_generics_Processor_process__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self: kotlin.native.internal.NativePtr, input: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.generics.generics.Processor<kotlin.Any?, kotlin.Any?>
    val __input = if (input == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(input) as kotlin.Any
    val _result = __self.process(__input)
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_generics_generics_Producer_produce")
public fun generation_generics_generics_Producer_produce(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.generics.generics.Producer<kotlin.Any?>
    val _result = __self.produce()
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_generics_generics_StringProducer_init_allocate")
public fun generation_generics_generics_StringProducer_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<generation.generics.generics.StringProducer>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_generics_generics_StringProducer_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun generation_generics_generics_StringProducer_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, generation.generics.generics.StringProducer())
}

@ExportedBridge("generation_generics_generics_StringProducer_produce")
public fun generation_generics_generics_StringProducer_produce(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as generation.generics.generics.StringProducer
    val _result = __self.produce()
    return _result.objcPtr()
}

@ExportedBridge("generation_generics_generics_TripleBox_init_allocate")
public fun generation_generics_generics_TripleBox_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<generation.generics.generics.TripleBox>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("generation_generics_generics_TripleBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun generation_generics_generics_TripleBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, generation.generics.generics.TripleBox())
}

@ExportedBridge("generation_generics_generics_createMap__TypesOfArguments__Swift_Array_ExportedKotlinPackages_generation_generics_generics_Pair___")
public fun generation_generics_generics_createMap__TypesOfArguments__Swift_Array_ExportedKotlinPackages_generation_generics_generics_Pair___(pairs: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __pairs = interpretObjCPointer<kotlin.collections.List<generation.generics.generics.Pair<kotlin.Any?, kotlin.Any?>>>(pairs)
    val _result = generation.generics.generics.createMap(__pairs)
    return _result.objcPtr()
}

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

@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(AnyConsumer::class, "4main11AnyConsumerC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ArrayBox::class, "4main8ArrayBoxC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Box::class, "4main3BoxC")
@file:kotlin.native.internal.objc.BindClassToObjCName(CPImpl::class, "4main6CPImplC")
@file:kotlin.native.internal.objc.BindClassToObjCName(DefaultBox::class, "4main10DefaultBoxC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Demo::class, "4main4DemoC")
@file:kotlin.native.internal.objc.BindClassToObjCName(GenericWithComparableUpperBound::class, "4main31GenericWithComparableUpperBoundC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Holder::class, "4main6HolderC")
@file:kotlin.native.internal.objc.BindClassToObjCName(IdentityProcessor::class, "4main17IdentityProcessorC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Pair::class, "4main4PairC")
@file:kotlin.native.internal.objc.BindClassToObjCName(StringProducer::class, "4main14StringProducerC")
@file:kotlin.native.internal.objc.BindClassToObjCName(TripleBox::class, "4main9TripleBoxC")
@file:kotlin.native.internal.objc.BindClassToObjCName(A::class, "_A")
@file:kotlin.native.internal.objc.BindClassToObjCName(B::class, "_B")
@file:kotlin.native.internal.objc.BindClassToObjCName(Consumer::class, "_Consumer")
@file:kotlin.native.internal.objc.BindClassToObjCName(ConsumerProducer::class, "_ConsumerProducer")
@file:kotlin.native.internal.objc.BindClassToObjCName(Processor::class, "_Processor")
@file:kotlin.native.internal.objc.BindClassToObjCName(Producer::class, "_Producer")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("A_foo_get")
public fun A_foo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as A<kotlin.Any?>
    val _result = __self.foo
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("AnyConsumer_consume__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__")
public fun AnyConsumer_consume__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(self: kotlin.native.internal.NativePtr, item: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AnyConsumer
    val __item = kotlin.native.internal.ref.dereferenceExternalRCRef(item) as kotlin.Any
    __self.consume(__item)
}

@ExportedBridge("ArrayBox_ints_get")
public fun ArrayBox_ints_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as ArrayBox
    val _result = __self.ints
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("B_foo_get")
public fun B_foo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as B<kotlin.Any?>
    val _result = __self.foo
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Box_t_get")
public fun Box_t_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Box<kotlin.Any?>
    val _result = __self.t
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("CPImpl_consume__TypesOfArguments__Swift_String__")
public fun CPImpl_consume__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, item: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as CPImpl
    val __item = interpretObjCPointer<kotlin.String>(item)
    __self.consume(__item)
}

@ExportedBridge("Consumer_consume__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun Consumer_consume__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, item: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Consumer<kotlin.Any?>
    val __item = if (item == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(item) as kotlin.Any
    __self.consume(__item)
}

@ExportedBridge("Demo_foo_get")
public fun Demo_foo_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val _result = __self.foo
    return _result
}

@ExportedBridge("GenericWithComparableUpperBound_t_get")
public fun GenericWithComparableUpperBound_t_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as GenericWithComparableUpperBound<kotlin.Comparable<kotlin.Any?>>
    val _result = __self.t
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Holder_headOrNull")
public fun Holder_headOrNull(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Holder<kotlin.Any?>
    val _result = __self.headOrNull()
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Holder_xs_get")
public fun Holder_xs_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Holder<kotlin.Any?>
    val _result = __self.xs
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("IdentityProcessor_process__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun IdentityProcessor_process__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, input: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as IdentityProcessor<kotlin.Any?>
    val __input = if (input == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(input) as kotlin.Any
    val _result = __self.process(__input)
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Pair_first_get")
public fun Pair_first_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Pair<kotlin.Any?, kotlin.Any?>
    val _result = __self.first
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Pair_second_get")
public fun Pair_second_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Pair<kotlin.Any?, kotlin.Any?>
    val _result = __self.second
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Processor_process__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun Processor_process__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, input: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Processor<kotlin.Any?, kotlin.Any?>
    val __input = if (input == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(input) as kotlin.Any
    val _result = __self.process(__input)
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Producer_produce")
public fun Producer_produce(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Producer<kotlin.Any?>
    val _result = __self.produce()
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("StringProducer_produce")
public fun StringProducer_produce(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as StringProducer
    val _result = __self.produce()
    return _result.objcPtr()
}

@ExportedBridge("__root___AnyConsumer_init_allocate")
public fun __root___AnyConsumer_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<AnyConsumer>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___AnyConsumer_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___AnyConsumer_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, AnyConsumer())
}

@ExportedBridge("__root___ArrayBox_init_allocate")
public fun __root___ArrayBox_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<ArrayBox>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ArrayBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___ArrayBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, ArrayBox())
}

@ExportedBridge("__root___CPImpl_init_allocate")
public fun __root___CPImpl_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<CPImpl>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___CPImpl_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___CPImpl_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, CPImpl())
}

@ExportedBridge("__root___DefaultBox_init_allocate")
public fun __root___DefaultBox_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<DefaultBox<kotlin.Any?>>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___DefaultBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun __root___DefaultBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(__kt: kotlin.native.internal.NativePtr, t: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __t = if (t == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(t) as kotlin.Any
    kotlin.native.internal.initInstance(____kt, DefaultBox<kotlin.Any?>(__t))
}

@ExportedBridge("__root___Demo_init_allocate")
public fun __root___Demo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Demo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Demo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___Demo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, Demo())
}

@ExportedBridge("__root___GenericWithComparableUpperBound_init_allocate")
public fun __root___GenericWithComparableUpperBound_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<GenericWithComparableUpperBound<kotlin.Comparable<kotlin.Any?>>>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___GenericWithComparableUpperBound_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlin_Comparable__")
public fun __root___GenericWithComparableUpperBound_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlin_Comparable__(__kt: kotlin.native.internal.NativePtr, t: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __t = kotlin.native.internal.ref.dereferenceExternalRCRef(t) as kotlin.Comparable<kotlin.Any?>
    kotlin.native.internal.initInstance(____kt, GenericWithComparableUpperBound<kotlin.Comparable<kotlin.Any?>>(__t))
}

@ExportedBridge("__root___Holder_init_allocate")
public fun __root___Holder_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Holder<kotlin.Any?>>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Holder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlin_Array__")
public fun __root___Holder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlin_Array__(__kt: kotlin.native.internal.NativePtr, xs: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __xs = kotlin.native.internal.ref.dereferenceExternalRCRef(xs) as kotlin.Array<kotlin.Any?>
    kotlin.native.internal.initInstance(____kt, Holder<kotlin.Any?>(__xs))
}

@ExportedBridge("__root___IdentityProcessor_init_allocate")
public fun __root___IdentityProcessor_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<IdentityProcessor<kotlin.Any?>>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___IdentityProcessor_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___IdentityProcessor_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, IdentityProcessor<kotlin.Any?>())
}

@ExportedBridge("__root___Pair_init_allocate")
public fun __root___Pair_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Pair<kotlin.Any?, kotlin.Any?>>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Pair_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun __root___Pair_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(__kt: kotlin.native.internal.NativePtr, first: kotlin.native.internal.NativePtr, second: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __first = if (first == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(first) as kotlin.Any
    val __second = if (second == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(second) as kotlin.Any
    kotlin.native.internal.initInstance(____kt, Pair<kotlin.Any?, kotlin.Any?>(__first, __second))
}

@ExportedBridge("__root___StringProducer_init_allocate")
public fun __root___StringProducer_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<StringProducer>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___StringProducer_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___StringProducer_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, StringProducer())
}

@ExportedBridge("__root___TripleBox_init_allocate")
public fun __root___TripleBox_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<TripleBox>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___TripleBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___TripleBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, TripleBox())
}

@ExportedBridge("__root___createMap__TypesOfArguments__Swift_Array_main_Pair___")
public fun __root___createMap__TypesOfArguments__Swift_Array_main_Pair___(pairs: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __pairs = interpretObjCPointer<kotlin.collections.List<Pair<kotlin.Any?, kotlin.Any?>>>(pairs)
    val _result = createMap(__pairs)
    return _result.objcPtr()
}

@ExportedBridge("__root___customFilter__TypesOfArguments__Swift_Array_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U29202D_U20Swift_Bool__")
public fun __root___customFilter__TypesOfArguments__Swift_Array_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U29202D_U20Swift_Bool__(`receiver`: kotlin.native.internal.NativePtr, predicate: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __receiver = interpretObjCPointer<kotlin.collections.List<kotlin.Any?>>(`receiver`)
    val __predicate = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr)->Boolean>(predicate);
        { arg0: kotlin.Any? -> kotlinFun(if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)) }
    }
    val _result = __receiver.customFilter(__predicate)
    return _result.objcPtr()
}

@ExportedBridge("__root___foo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun __root___foo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(param1: kotlin.native.internal.NativePtr, param2: kotlin.native.internal.NativePtr): Unit {
    val __param1 = if (param1 == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(param1) as kotlin.Any
    val __param2 = if (param2 == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(param2) as kotlin.Any
    foo(__param1, __param2)
}

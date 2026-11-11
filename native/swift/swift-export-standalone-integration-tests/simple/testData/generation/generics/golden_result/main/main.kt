@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(AnyConsumer::class, "4main11AnyConsumerC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ArrayBox::class, "4main8ArrayBoxC")
@file:kotlin.native.internal.objc.BindClassToObjCName(CPImpl::class, "4main6CPImplC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Demo::class, "4main4DemoC")
@file:kotlin.native.internal.objc.BindClassToObjCName(FunctionalBox::class, "4main13FunctionalBoxC")
@file:kotlin.native.internal.objc.BindClassToObjCName(StringProducer::class, "4main14StringProducerC")
@file:kotlin.native.internal.objc.BindClassToObjCName(TripleBox::class, "4main9TripleBoxC")
@file:kotlin.native.internal.objc.BindClassToObjCName(A::class, "_main_A")
@file:kotlin.native.internal.objc.BindClassToObjCName(AFactory::class, "_main_AFactory")
@file:kotlin.native.internal.objc.BindClassToObjCName(B::class, "_main_B")
@file:kotlin.native.internal.objc.BindClassToObjCName(Bar::class, "4main9_Bar_BaseC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Box::class, "4main9_Box_BaseC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Consumer::class, "_main_Consumer")
@file:kotlin.native.internal.objc.BindClassToObjCName(ConsumerProducer::class, "_main_ConsumerProducer")
@file:kotlin.native.internal.objc.BindClassToObjCName(Foo::class, "_main_Foo")
@file:kotlin.native.internal.objc.BindClassToObjCName(Processor::class, "_main_Processor")
@file:kotlin.native.internal.objc.BindClassToObjCName(Producer::class, "_main_Producer")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

@ImportedBridge("AFactory_create__reverse_swift")
internal external fun AFactory_create__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(AFactory::class, "create")
public fun AFactory_create__reverse(self: AFactory<A<kotlin.Unit>>): A<kotlin.Unit> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = AFactory_create__reverse_swift(__self)
    return (dereferenceAndDisposeBridgeableRef(_result) as A<kotlin.Unit>)
}

@ImportedBridge("A_foo_get__reverse_swift")
internal external fun A_foo_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(A::class, "<get-foo>")
public fun A_foo_get__reverse(self: A<kotlin.Any?>): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = A_foo_get__reverse_swift(__self)
    return (dereferenceAndDisposeBridgeableRef(_result) as kotlin.Any?)
}

@ImportedBridge("B_foo_get__reverse_swift")
internal external fun B_foo_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(B::class, "<get-foo>")
public fun B_foo_get__reverse(self: B<kotlin.Any?>): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = B_foo_get__reverse_swift(__self)
    return (dereferenceAndDisposeBridgeableRef(_result) as kotlin.Any?)
}

@ImportedBridge("Consumer_consume__TypesOfArguments__T____reverse_swift")
internal external fun Consumer_consume__TypesOfArguments__T____reverse_swift(self: kotlin.native.internal.NativePtr, item: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(Consumer::class, "consume")
public fun Consumer_consume__TypesOfArguments__T____reverse(self: Consumer<kotlin.Any?>, item: kotlin.Any?): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __item = if (item == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(item)
    val _result = Consumer_consume__TypesOfArguments__T____reverse_swift(__self, __item)
    return run<Unit> { _result }
}

@ImportedBridge("Processor_process__TypesOfArguments__T____reverse_swift")
internal external fun Processor_process__TypesOfArguments__T____reverse_swift(self: kotlin.native.internal.NativePtr, input: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Processor::class, "process")
public fun Processor_process__TypesOfArguments__T____reverse(self: Processor<kotlin.Any?, kotlin.Any?>, input: kotlin.Any?): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __input = if (input == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(input)
    val _result = Processor_process__TypesOfArguments__T____reverse_swift(__self, __input)
    return (dereferenceAndDisposeBridgeableRef(_result) as kotlin.Any?)
}

@ImportedBridge("Producer_produce__reverse_swift")
internal external fun Producer_produce__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Producer::class, "produce")
public fun Producer_produce__reverse(self: Producer<kotlin.Any?>): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = Producer_produce__reverse_swift(__self)
    return (dereferenceAndDisposeBridgeableRef(_result) as kotlin.Any?)
}

@ImportedBridge("StringProducer_produce__reverse_swift")
internal external fun StringProducer_produce__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(StringProducer::class, "produce")
public fun StringProducer_produce__reverse(self: StringProducer): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = StringProducer_produce__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("main_internal_functional_type_callee_SwiftU2EBool__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___")
internal external fun main_internal_functional_type_callee_SwiftU2EBool__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean

@ImportedBridge("main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Box_Swift_Optional_KotlinRuntimeSupport_KotlinAny____")
internal external fun main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Box_Swift_Optional_KotlinRuntimeSupport_KotlinAny____(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean

@ImportedBridge("main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Box_Swift_String___")
internal external fun main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Box_Swift_String___(pointerToClosure: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean

@ImportedBridge("main_internal_functional_type_callee_ctx1_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Box_Swift_String___")
internal external fun main_internal_functional_type_callee_ctx1_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Box_Swift_String___(pointerToClosure: kotlin.native.internal.NativePtr, ctx0: kotlin.native.internal.NativePtr): Boolean

@ImportedBridge("main_internal_functional_type_callee_mainU2EBoxU3CSwiftU2EOptionalU3CKotlinRuntimeSupportU2EKotlinAnyU3E3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
internal external fun main_internal_functional_type_callee_mainU2EBoxU3CSwiftU2EOptionalU3CKotlinRuntimeSupportU2EKotlinAnyU3E3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToClosure: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@ImportedBridge("main_internal_functional_type_callee_mainU2EBoxU3CSwiftU2EStringU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
internal external fun main_internal_functional_type_callee_mainU2EBoxU3CSwiftU2EStringU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToClosure: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@ExportedBridge("AFactory_create")
public fun AFactory_create(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AFactory<A<kotlin.Unit>>
    val _result = __self.create()
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("A_foo_get")
public fun A_foo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as A<kotlin.Any?>
    val _result = __self.foo
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("AnyConsumer_consume__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__")
public fun AnyConsumer_consume__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(self: kotlin.native.internal.NativePtr, item: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AnyConsumer
    val __item = (dereferenceAndDisposeBridgeableRef(item) as kotlin.Any)
    __self.consume(__item)
    return true
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
public fun CPImpl_consume__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, item: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as CPImpl
    val __item = interpretObjCPointer<kotlin.String>(item)
    __self.consume(__item)
    return true
}

@ExportedBridge("Consumer_consume__TypesOfArguments__T__")
public fun Consumer_consume__TypesOfArguments__T__(self: kotlin.native.internal.NativePtr, item: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Consumer<kotlin.Any?>
    val __item = (dereferenceAndDisposeBridgeableRef(item) as kotlin.Any?)
                    run {
        @Suppress("UNCHECKED_CAST") fun <__T0> __bridge() { (__self as Consumer<__T0>).consume((__item as __T0)) }
        __bridge<Nothing>()
    }
                    return true
}

@ExportedBridge("Demo_foo_get")
public fun Demo_foo_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Demo
    val _result = __self.foo
    return _result
}

@ExportedBridge("GenericWithComparableUpperBound_t_get")
public fun GenericWithComparableUpperBound_t_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as GenericWithComparableUpperBound<*>
                    val _result = run {
        @Suppress("UNCHECKED_CAST") fun <__T0 : kotlin.Comparable<__T0>> __bridge(): kotlin.Comparable<*> = ((__self as GenericWithComparableUpperBound<__T0>).t as kotlin.Any?) as kotlin.Comparable<*>
        __bridge<Nothing>()
    }
                    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("HolderConstrained_xs_get")
public fun HolderConstrained_xs_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as HolderConstrained<kotlin.Any>
    val _result = __self.xs
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

@ExportedBridge("IdentityProcessor_process__TypesOfArguments__T__")
public fun IdentityProcessor_process__TypesOfArguments__T__(self: kotlin.native.internal.NativePtr, input: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as IdentityProcessor<kotlin.Any?>
    val __input = (dereferenceAndDisposeBridgeableRef(input) as kotlin.Any?)
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

@ExportedBridge("Processor_process__TypesOfArguments__T__")
public fun Processor_process__TypesOfArguments__T__(self: kotlin.native.internal.NativePtr, input: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Processor<kotlin.Any?, kotlin.Any?>
    val __input = (dereferenceAndDisposeBridgeableRef(input) as kotlin.Any?)
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

@ExportedBridge("StringProducer_produce_direct", nonVirtualTargetMethod = "produce")
public fun StringProducer_produce_direct(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as StringProducer
    val _result = __self.produce()
    return _result.objcPtr()
}

@ExportedBridge("__root___A")
public fun __root___A(): kotlin.native.internal.NativePtr {
    val _result = A<kotlin.Any?>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___AnyConsumer_init_allocate")
public fun __root___AnyConsumer_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<AnyConsumer>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___AnyConsumer_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___AnyConsumer_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, AnyConsumer())
    return true
}

@ExportedBridge("__root___ArrayBox_init_allocate")
public fun __root___ArrayBox_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<ArrayBox>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ArrayBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___ArrayBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, ArrayBox())
    return true
}

@ExportedBridge("__root___Bar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__", nonVirtualTargetMethod = "<init>")
public fun __root___Bar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
                    run {
        @Suppress("UNCHECKED_CAST") fun <__T0 : Foo, __T1 : __T0> __init() { kotlin.native.internal.initInstance(____kt, Bar<__T0, __T1>()) }
        __init<Nothing, Nothing>()
    }
                    return true
}

@ExportedBridge("__root___Box_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_T__", nonVirtualTargetMethod = "<init>")
public fun __root___Box_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_T__(__kt: kotlin.native.internal.NativePtr, t: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __t = (dereferenceAndDisposeBridgeableRef(t) as kotlin.Any?)
    kotlin.native.internal.initInstance(____kt, Box<kotlin.Any?>(__t))
    return true
}

@ExportedBridge("__root___CPImpl_init_allocate")
public fun __root___CPImpl_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<CPImpl>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___CPImpl_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___CPImpl_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, CPImpl())
    return true
}

@ExportedBridge("__root___DefaultBox_init_allocate")
public fun __root___DefaultBox_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<DefaultBox<kotlin.Any?>>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___DefaultBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_T__")
public fun __root___DefaultBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_T__(__kt: kotlin.native.internal.NativePtr, t: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __t = (dereferenceAndDisposeBridgeableRef(t) as kotlin.Any?)
    kotlin.native.internal.initInstance(____kt, DefaultBox<kotlin.Any?>(__t))
    return true
}

@ExportedBridge("__root___Demo_init_allocate")
public fun __root___Demo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Demo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Demo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___Demo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, Demo())
    return true
}

@ExportedBridge("__root___FunctionalBox_init_allocate")
public fun __root___FunctionalBox_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<FunctionalBox>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___FunctionalBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___FunctionalBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, FunctionalBox())
    return true
}

@ExportedBridge("__root___GenericWithComparableUpperBound_init_allocate")
public fun __root___GenericWithComparableUpperBound_init_allocate(): kotlin.native.internal.NativePtr {
                    val _result = run {
        @Suppress("UNCHECKED_CAST") fun <__T0 : kotlin.Comparable<__T0>> __alloc(): kotlin.Any? = kotlin.native.internal.createUninitializedInstance<GenericWithComparableUpperBound<__T0>>()
        __alloc<Nothing>()
    }
                    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___GenericWithComparableUpperBound_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_T__")
public fun __root___GenericWithComparableUpperBound_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_T__(__kt: kotlin.native.internal.NativePtr, t: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __t = (dereferenceAndDisposeBridgeableRef(t) as kotlin.Comparable<*>)
                    run {
        @Suppress("UNCHECKED_CAST") fun <__T0 : kotlin.Comparable<__T0>> __init() { kotlin.native.internal.initInstance(____kt, GenericWithComparableUpperBound<__T0>((__t as __T0))) }
        __init<Nothing>()
    }
                    return true
}

@ExportedBridge("__root___HolderConstrained_init_allocate")
public fun __root___HolderConstrained_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<HolderConstrained<kotlin.Any>>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___HolderConstrained_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlin_Array_T___")
public fun __root___HolderConstrained_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlin_Array_T___(__kt: kotlin.native.internal.NativePtr, xs: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __xs = kotlin.native.internal.ref.dereferenceExternalRCRef(xs) as kotlin.Array<kotlin.Any>
    kotlin.native.internal.initInstance(____kt, HolderConstrained<kotlin.Any>(__xs))
    return true
}

@ExportedBridge("__root___Holder_init_allocate")
public fun __root___Holder_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Holder<kotlin.Any?>>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Holder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlin_Array_T___")
public fun __root___Holder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlin_Array_T___(__kt: kotlin.native.internal.NativePtr, xs: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __xs = kotlin.native.internal.ref.dereferenceExternalRCRef(xs) as kotlin.Array<kotlin.Any?>
    kotlin.native.internal.initInstance(____kt, Holder<kotlin.Any?>(__xs))
    return true
}

@ExportedBridge("__root___IdentityProcessor_init_allocate")
public fun __root___IdentityProcessor_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<IdentityProcessor<kotlin.Any?>>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___IdentityProcessor_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___IdentityProcessor_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, IdentityProcessor<kotlin.Any?>())
    return true
}

@ExportedBridge("__root___MultipleUpperBounds_init_allocate")
public fun __root___MultipleUpperBounds_init_allocate(): kotlin.native.internal.NativePtr {
                    val _result = run {
        @Suppress("UNCHECKED_CAST") fun <__T0> __alloc(): kotlin.Any? where __T0 : Producer<kotlin.String>, __T0 : Consumer<kotlin.String> = kotlin.native.internal.createUninitializedInstance<MultipleUpperBounds<__T0>>()
        __alloc<Nothing>()
    }
                    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___MultipleUpperBounds_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___MultipleUpperBounds_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
                    run {
        @Suppress("UNCHECKED_CAST") fun <__T0> __init() where __T0 : Producer<kotlin.String>, __T0 : Consumer<kotlin.String> { kotlin.native.internal.initInstance(____kt, MultipleUpperBounds<__T0>()) }
        __init<Nothing>()
    }
                    return true
}

@ExportedBridge("__root___Pair_init_allocate")
public fun __root___Pair_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Pair<kotlin.Any?, kotlin.Any?>>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Pair_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_K_V__")
public fun __root___Pair_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_K_V__(__kt: kotlin.native.internal.NativePtr, first: kotlin.native.internal.NativePtr, second: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __first = (dereferenceAndDisposeBridgeableRef(first) as kotlin.Any?)
    val __second = (dereferenceAndDisposeBridgeableRef(second) as kotlin.Any?)
    kotlin.native.internal.initInstance(____kt, Pair<kotlin.Any?, kotlin.Any?>(__first, __second))
    return true
}

@ExportedBridge("__root___StringProducer_init_allocate")
public fun __root___StringProducer_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<StringProducer>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___StringProducer_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___StringProducer_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, StringProducer())
    return true
}

@ExportedBridge("__root___TripleBox_init_allocate")
public fun __root___TripleBox_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<TripleBox>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___TripleBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___TripleBox_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, TripleBox())
    return true
}

@ExportedBridge("__root___bar__TypesOfArguments__Swift_Optional_T__T__")
public fun __root___bar__TypesOfArguments__Swift_Optional_T__T__(param1: kotlin.native.internal.NativePtr, param2: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __param1 = (dereferenceAndDisposeBridgeableRef(param1) as kotlin.Any?)
    val __param2 = (dereferenceAndDisposeBridgeableRef(param2) as kotlin.Any?)
    val _result = bar<kotlin.Any?>(__param1, __param2)
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___bar_get")
public fun __root___bar_get(): kotlin.native.internal.NativePtr {
    val _result = bar
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___baz_get")
public fun __root___baz_get(): kotlin.native.internal.NativePtr {
    val _result = baz
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___baz_set__TypesOfArguments__main_Holder_Swift_String___")
public fun __root___baz_set__TypesOfArguments__main_Holder_Swift_String___(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as Holder<kotlin.String>
    baz = __newValue
    return true
}

@ExportedBridge("__root___createMap__TypesOfArguments__Swift_Array_main_Pair_K_V____")
public fun __root___createMap__TypesOfArguments__Swift_Array_main_Pair_K_V____(pairs: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __pairs = (dereferenceAndDisposeBridgeableRef(pairs) as kotlin.collections.List<Pair<kotlin.Any?, kotlin.Any?>>)
    val _result = createMap<kotlin.Any?, kotlin.Any?>(__pairs)
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___customFilter__TypesOfArgumentsE__Swift_Array_T__U28TU29202D_U20Swift_Bool__")
public fun __root___customFilter__TypesOfArgumentsE__Swift_Array_T__U28TU29202D_U20Swift_Bool__(`receiver`: kotlin.native.internal.NativePtr, predicate: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __receiver = (dereferenceAndDisposeBridgeableRef(`receiver`) as kotlin.collections.List<kotlin.Any?>)
    val __predicate = run {
        val closurePtr = predicate;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: kotlin.Any? ->
            val _arg0 = if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = main_internal_functional_type_callee_SwiftU2EBool__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(closureBox.objcPtr(), _arg0)
            _result
        }
    }
    val _result = __receiver.customFilter<kotlin.Any?>(__predicate)
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___extProp_get__TypesOfArgumentsE__main_Holder_Swift_String___")
public fun __root___extProp_get__TypesOfArgumentsE__main_Holder_Swift_String___(`receiver`: kotlin.native.internal.NativePtr): Int {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as Holder<kotlin.String>
    val _result = __receiver.extProp
    return _result
}

@ExportedBridge("__root___foo__TypesOfArguments__T_Swift_Optional_T___")
public fun __root___foo__TypesOfArguments__T_Swift_Optional_T___(param1: kotlin.native.internal.NativePtr, param2: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __param1 = (dereferenceAndDisposeBridgeableRef(param1) as kotlin.Any?)
    val __param2 = (dereferenceAndDisposeBridgeableRef(param2) as kotlin.Any?)
    val _result = foo<kotlin.Any?>(__param1, __param2)
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___produceBox__TypesOfArguments__U28main_Box_Swift_String_U29202D_U20Swift_Void__")
public fun __root___produceBox__TypesOfArguments__U28main_Box_Swift_String_U29202D_U20Swift_Void__(box: kotlin.native.internal.NativePtr): Boolean {
    val __box = run {
        val closurePtr = box;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: Box<kotlin.String> ->
            val _arg0 = kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Box_Swift_String___(closureBox.objcPtr(), _arg0)
            run<Unit> { _result }
        }
    }
    produceBox(__box)
    return true
}

@ExportedBridge("__root___produceBoxStar__TypesOfArguments__U28main_Box_Swift_Optional_KotlinRuntimeSupport_KotlinAny__U29202D_U20Swift_Void__")
public fun __root___produceBoxStar__TypesOfArguments__U28main_Box_Swift_Optional_KotlinRuntimeSupport_KotlinAny__U29202D_U20Swift_Void__(box: kotlin.native.internal.NativePtr): Boolean {
    val __box = run {
        val closurePtr = box;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: Box<*> ->
            val _arg0 = kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Box_Swift_Optional_KotlinRuntimeSupport_KotlinAny____(closureBox.objcPtr(), _arg0)
            run<Unit> { _result }
        }
    }
    produceBoxStar(__box)
    return true
}

@ExportedBridge("__root___produceBoxUpperBound__TypesOfArguments__U28main_Box_Swift_Optional_KotlinRuntimeSupport_KotlinAny__U29202D_U20Swift_Void__")
public fun __root___produceBoxUpperBound__TypesOfArguments__U28main_Box_Swift_Optional_KotlinRuntimeSupport_KotlinAny__U29202D_U20Swift_Void__(box: kotlin.native.internal.NativePtr): Boolean {
    val __box = run {
        val closurePtr = box;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: Box<kotlin.Any?> ->
            val _arg0 = kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Box_Swift_Optional_KotlinRuntimeSupport_KotlinAny____(closureBox.objcPtr(), _arg0)
            run<Unit> { _result }
        }
    }
    produceBoxUpperBound(__box)
    return true
}

@ExportedBridge("__root___produceCtxBox__TypesOfArguments__U28main_Box_Swift_String_U29202D_U20Swift_Void__")
public fun __root___produceCtxBox__TypesOfArguments__U28main_Box_Swift_String_U29202D_U20Swift_Void__(box: kotlin.native.internal.NativePtr): Boolean {
    val __box = run {
        val closurePtr = box;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { ctx0: Box<kotlin.String> ->
            val _ctx0 = kotlin.native.internal.ref.createRetainedExternalRCRef(ctx0)
            val _result = main_internal_functional_type_callee_ctx1_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Box_Swift_String___(closureBox.objcPtr(), _ctx0)
            run<Unit> { _result }
        }
    }
    produceCtxBox(__box)
    return true
}

@ExportedBridge("__root___produceExtBox__TypesOfArguments__U28main_Box_Swift_String_U29202D_U20Swift_Void__")
public fun __root___produceExtBox__TypesOfArguments__U28main_Box_Swift_String_U29202D_U20Swift_Void__(box: kotlin.native.internal.NativePtr): Boolean {
    val __box = run {
        val closurePtr = box;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        { arg0: Box<kotlin.String> ->
            val _arg0 = kotlin.native.internal.ref.createRetainedExternalRCRef(arg0)
            val _result = main_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Box_Swift_String___(closureBox.objcPtr(), _arg0)
            run<Unit> { _result }
        }
    }
    produceExtBox(__box)
    return true
}

@ExportedBridge("__root___returnBFun")
public fun __root___returnBFun(): kotlin.native.internal.NativePtr {
    val _result = returnBFun()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___returnBoxFun")
public fun __root___returnBoxFun(): kotlin.native.internal.NativePtr {
    val _result = returnBoxFun()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___returnBoxFunIn")
public fun __root___returnBoxFunIn(): kotlin.native.internal.NativePtr {
    val _result = returnBoxFunIn()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___returnGenericConstraintToGeneric__TypesOfArguments__A_B__")
public fun __root___returnGenericConstraintToGeneric__TypesOfArguments__A_B__(arg1: kotlin.native.internal.NativePtr, arg2: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __arg1 = (dereferenceAndDisposeBridgeableRef(arg1) as kotlin.Any?)
    val __arg2 = (dereferenceAndDisposeBridgeableRef(arg2) as kotlin.Any?)
                    val _result = run {
        @Suppress("UNCHECKED_CAST") fun <__T0, __T1 : __T0> __bridge(): kotlin.Any? = (returnGenericConstraintToGeneric<__T0, __T1>((__arg1 as __T0), (__arg2 as __T1)) as kotlin.Any?) as kotlin.Any?
        __bridge<Nothing, Nothing>()
    }
                    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___returnSomeBoxForArg__TypesOfArguments__A__")
public fun __root___returnSomeBoxForArg__TypesOfArguments__A__(arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __arg = (dereferenceAndDisposeBridgeableRef(arg) as kotlin.Any?)
                    val _result = run {
        @Suppress("UNCHECKED_CAST") fun <__T0, __T1 : Box<__T0>> __bridge(): Box<kotlin.Any?> = (returnSomeBoxForArg<__T0, __T1>((__arg as __T0)) as kotlin.Any?) as Box<kotlin.Any?>
        __bridge<Nothing, Nothing>()
    }
                    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___takeAndReturn__TypesOfArguments__ExportedKotlinPackages_kotlin_Array_Swift_String___")
public fun __root___takeAndReturn__TypesOfArguments__ExportedKotlinPackages_kotlin_Array_Swift_String___(a: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __a = kotlin.native.internal.ref.dereferenceExternalRCRef(a) as kotlin.Array<kotlin.String>
    val _result = takeAndReturn(__a)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___takeBox__TypesOfArguments__main_Box_Swift_String___")
public fun __root___takeBox__TypesOfArguments__main_Box_Swift_String___(box: kotlin.native.internal.NativePtr): Boolean {
    val __box = kotlin.native.internal.ref.dereferenceExternalRCRef(box) as Box<kotlin.String>
    takeBox(__box)
    return true
}

@ExportedBridge("__root___takeBoxClosure__TypesOfArguments__U2829202D_U20main_Box_Swift_String___")
public fun __root___takeBoxClosure__TypesOfArguments__U2829202D_U20main_Box_Swift_String___(box: kotlin.native.internal.NativePtr): Boolean {
    val __box = run {
        val closurePtr = box;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        {
            val _result = main_internal_functional_type_callee_mainU2EBoxU3CSwiftU2EStringU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__(closureBox.objcPtr())
            kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as Box<kotlin.String>
        }
    }
    takeBoxClosure(__box)
    return true
}

@ExportedBridge("__root___takeBoxStarProjection__TypesOfArguments__main_Box__T0___")
public fun __root___takeBoxStarProjection__TypesOfArguments__main_Box__T0___(box: kotlin.native.internal.NativePtr): Boolean {
    val __box = kotlin.native.internal.ref.dereferenceExternalRCRef(box) as Box<kotlin.Any?>
    takeBoxStarProjection(__box)
    return true
}

@ExportedBridge("__root___takeBoxUpperBound__TypesOfArguments__main_Box_Swift_Optional_KotlinRuntimeSupport_KotlinAny____")
public fun __root___takeBoxUpperBound__TypesOfArguments__main_Box_Swift_Optional_KotlinRuntimeSupport_KotlinAny____(box: kotlin.native.internal.NativePtr): Boolean {
    val __box = kotlin.native.internal.ref.dereferenceExternalRCRef(box) as Box<kotlin.Any?>
    takeBoxUpperBound(__box)
    return true
}

@ExportedBridge("__root___takeBoxUpperBoundClosure__TypesOfArguments__U2829202D_U20main_Box_Swift_Optional_KotlinRuntimeSupport_KotlinAny____")
public fun __root___takeBoxUpperBoundClosure__TypesOfArguments__U2829202D_U20main_Box_Swift_Optional_KotlinRuntimeSupport_KotlinAny____(box: kotlin.native.internal.NativePtr): Boolean {
    val __box = run {
        val closurePtr = box;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        {
            val _result = main_internal_functional_type_callee_mainU2EBoxU3CSwiftU2EOptionalU3CKotlinRuntimeSupportU2EKotlinAnyU3E3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__(closureBox.objcPtr())
            kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as Box<kotlin.Any?>
        }
    }
    takeBoxUpperBoundClosure(__box)
    return true
}

@ExportedBridge("main_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Box_Swift_String___")
public fun main_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Box_Swift_String___(pointerToBlock: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Int {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = kotlin.native.internal.ref.dereferenceExternalRCRef(_1) as Box<kotlin.String>
    val _result = (__pointerToBlock as Function1<Box<kotlin.String>, Int>).invoke(___1)
    return _result
}

@ExportedBridge("main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20main_B__")
public fun main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20main_B__(pointerToBlock: kotlin.native.internal.NativePtr, _1: kotlin.native.internal.NativePtr): Boolean {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val ___1 = kotlin.native.internal.ref.dereferenceExternalRCRef(_1) as B<kotlin.Any?>
    (__pointerToBlock as Function1<B<*>, Unit>).invoke(___1)
    return true
}

@ExportedBridge("main_internal_functional_type_caller_mainU2EBoxU3CSwiftU2EStringU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun main_internal_functional_type_caller_mainU2EBoxU3CSwiftU2EStringU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __pointerToBlock = kotlin.native.internal.ref.dereferenceExternalRCRef(pointerToBlock)!!
    val _result = (__pointerToBlock as Function0<Box<kotlin.String>>).invoke()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

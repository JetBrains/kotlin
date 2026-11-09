@file:OptIn(ExperimentalLibApi::class, InterfaceOptInOne::class, InterfaceOptInTwo::class, InternalLibApi::class, OpenClassOptIn::class)
@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(ExperimentalLibClass::class, "3lib20ExperimentalLibClassC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OpenClass::class, "3lib9OpenClassC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OverrideBase::class, "3lib12OverrideBaseC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OverrideDerived::class, "3lib15OverrideDerivedC")
@file:kotlin.native.internal.objc.BindClassToObjCName(RegularLibClass::class, "3lib15RegularLibClassC")
@file:kotlin.native.internal.objc.BindClassToObjCName(RegularLibClass.MarkedNested::class, "3lib15RegularLibClassC12MarkedNestedC")
@file:kotlin.native.internal.objc.BindClassToObjCName(SpiAbstractClass::class, "3lib16SpiAbstractClassC")
@file:kotlin.native.internal.objc.BindClassToObjCName(InterfaceOne::class, "_lib_InterfaceOne")
@file:kotlin.native.internal.objc.BindClassToObjCName(InterfaceTwo::class, "_lib_InterfaceTwo")
@file:kotlin.native.internal.objc.BindClassToObjCName(InternalLibInterface::class, "_lib_InternalLibInterface")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

@ImportedBridge("InternalLibInterface_bar__reverse_swift")
internal external fun InternalLibInterface_bar__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(InternalLibInterface::class, "bar")
public fun InternalLibInterface_bar__reverse(self: InternalLibInterface): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = InternalLibInterface_bar__reverse_swift(__self)
    return run<Unit> { _result }
}

@ImportedBridge("InternalLibInterface_experimentalFun__reverse_swift")
internal external fun InternalLibInterface_experimentalFun__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(InternalLibInterface::class, "experimentalFun")
public fun InternalLibInterface_experimentalFun__reverse(self: InternalLibInterface): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = InternalLibInterface_experimentalFun__reverse_swift(__self)
    return run<Unit> { _result }
}

@ImportedBridge("InternalLibInterface_experimentalProp_get__reverse_swift")
internal external fun InternalLibInterface_experimentalProp_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(InternalLibInterface::class, "<get-experimentalProp>")
public fun InternalLibInterface_experimentalProp_get__reverse(self: InternalLibInterface): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = InternalLibInterface_experimentalProp_get__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("InternalLibInterface_experimentalProp_set__TypesOfArguments__Swift_String____reverse_swift")
internal external fun InternalLibInterface_experimentalProp_set__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(InternalLibInterface::class, "<set-experimentalProp>")
public fun InternalLibInterface_experimentalProp_set__TypesOfArguments__Swift_String____reverse(self: InternalLibInterface, newValue: kotlin.String): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __newValue = newValue.objcPtr()
    val _result = InternalLibInterface_experimentalProp_set__TypesOfArguments__Swift_String____reverse_swift(__self, __newValue)
    return run<Unit> { _result }
}

@ImportedBridge("InternalLibInterface_foo_get__reverse_swift")
internal external fun InternalLibInterface_foo_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(InternalLibInterface::class, "<get-foo>")
public fun InternalLibInterface_foo_get__reverse(self: InternalLibInterface): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = InternalLibInterface_foo_get__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("InternalLibInterface_foo_set__TypesOfArguments__Swift_String____reverse_swift")
internal external fun InternalLibInterface_foo_set__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(InternalLibInterface::class, "<set-foo>")
public fun InternalLibInterface_foo_set__TypesOfArguments__Swift_String____reverse(self: InternalLibInterface, newValue: kotlin.String): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __newValue = newValue.objcPtr()
    val _result = InternalLibInterface_foo_set__TypesOfArguments__Swift_String____reverse_swift(__self, __newValue)
    return run<Unit> { _result }
}

@ImportedBridge("InternalLibInterface_internalFun__reverse_swift")
internal external fun InternalLibInterface_internalFun__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(InternalLibInterface::class, "internalFun")
public fun InternalLibInterface_internalFun__reverse(self: InternalLibInterface): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = InternalLibInterface_internalFun__reverse_swift(__self)
    return run<Unit> { _result }
}

@ImportedBridge("InternalLibInterface_internalProp_get__reverse_swift")
internal external fun InternalLibInterface_internalProp_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(InternalLibInterface::class, "<get-internalProp>")
public fun InternalLibInterface_internalProp_get__reverse(self: InternalLibInterface): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = InternalLibInterface_internalProp_get__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("InternalLibInterface_internalProp_set__TypesOfArguments__Swift_String____reverse_swift")
internal external fun InternalLibInterface_internalProp_set__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(InternalLibInterface::class, "<set-internalProp>")
public fun InternalLibInterface_internalProp_set__TypesOfArguments__Swift_String____reverse(self: InternalLibInterface, newValue: kotlin.String): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __newValue = newValue.objcPtr()
    val _result = InternalLibInterface_internalProp_set__TypesOfArguments__Swift_String____reverse_swift(__self, __newValue)
    return run<Unit> { _result }
}

@ImportedBridge("OverrideBase_bar__reverse_swift")
internal external fun OverrideBase_bar__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(OverrideBase::class, "bar")
public fun OverrideBase_bar__reverse(self: OverrideBase): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = OverrideBase_bar__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("OverrideBase_foo__reverse_swift")
internal external fun OverrideBase_foo__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(OverrideBase::class, "foo")
public fun OverrideBase_foo__reverse(self: OverrideBase): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = OverrideBase_foo__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("OverrideDerived_bar__reverse_swift")
internal external fun OverrideDerived_bar__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(OverrideDerived::class, "bar")
public fun OverrideDerived_bar__reverse(self: OverrideDerived): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = OverrideDerived_bar__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("OverrideDerived_foo__reverse_swift")
internal external fun OverrideDerived_foo__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(OverrideDerived::class, "foo")
public fun OverrideDerived_foo__reverse(self: OverrideDerived): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = OverrideDerived_foo__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("SpiAbstractClass_implementMe__reverse_swift")
internal external fun SpiAbstractClass_implementMe__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(SpiAbstractClass::class, "implementMe")
public fun SpiAbstractClass_implementMe__reverse(self: SpiAbstractClass): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = SpiAbstractClass_implementMe__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ExportedBridge("ExperimentalLibClass_bar")
@OptIn(ExperimentalLibApi::class)
public fun ExperimentalLibClass_bar(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as ExperimentalLibClass
    __self.bar()
    return true
}

@ExportedBridge("ExperimentalLibClass_foo_get")
@OptIn(ExperimentalLibApi::class)
public fun ExperimentalLibClass_foo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as ExperimentalLibClass
    val _result = __self.foo
    return _result.objcPtr()
}

@ExportedBridge("ExperimentalLibClass_foo_set__TypesOfArguments__Swift_String__")
@OptIn(ExperimentalLibApi::class)
public fun ExperimentalLibClass_foo_set__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as ExperimentalLibClass
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    __self.foo = __newValue
    return true
}

@ExportedBridge("InternalLibInterface_bar")
@OptIn(InternalLibApi::class)
public fun InternalLibInterface_bar(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as InternalLibInterface
    __self.bar()
    return true
}

@ExportedBridge("InternalLibInterface_experimentalFun")
@OptIn(ExperimentalLibApi::class, InternalLibApi::class)
public fun InternalLibInterface_experimentalFun(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as InternalLibInterface
    __self.experimentalFun()
    return true
}

@ExportedBridge("InternalLibInterface_experimentalProp_get")
@OptIn(ExperimentalLibApi::class, InternalLibApi::class)
public fun InternalLibInterface_experimentalProp_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as InternalLibInterface
    val _result = __self.experimentalProp
    return _result.objcPtr()
}

@ExportedBridge("InternalLibInterface_experimentalProp_set__TypesOfArguments__Swift_String__")
@OptIn(ExperimentalLibApi::class, InternalLibApi::class)
public fun InternalLibInterface_experimentalProp_set__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as InternalLibInterface
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    __self.experimentalProp = __newValue
    return true
}

@ExportedBridge("InternalLibInterface_foo_get")
@OptIn(InternalLibApi::class)
public fun InternalLibInterface_foo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as InternalLibInterface
    val _result = __self.foo
    return _result.objcPtr()
}

@ExportedBridge("InternalLibInterface_foo_set__TypesOfArguments__Swift_String__")
@OptIn(InternalLibApi::class)
public fun InternalLibInterface_foo_set__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as InternalLibInterface
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    __self.foo = __newValue
    return true
}

@ExportedBridge("InternalLibInterface_internalFun")
@OptIn(InternalLibApi::class)
public fun InternalLibInterface_internalFun(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as InternalLibInterface
    __self.internalFun()
    return true
}

@ExportedBridge("InternalLibInterface_internalProp_get")
@OptIn(InternalLibApi::class)
public fun InternalLibInterface_internalProp_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as InternalLibInterface
    val _result = __self.internalProp
    return _result.objcPtr()
}

@ExportedBridge("InternalLibInterface_internalProp_set__TypesOfArguments__Swift_String__")
@OptIn(InternalLibApi::class)
public fun InternalLibInterface_internalProp_set__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as InternalLibInterface
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    __self.internalProp = __newValue
    return true
}

@ExportedBridge("OverrideBase_bar")
public fun OverrideBase_bar(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OverrideBase
    val _result = __self.bar()
    return _result.objcPtr()
}

@ExportedBridge("OverrideBase_bar_direct", nonVirtualTargetMethod = "bar")
public fun OverrideBase_bar_direct(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OverrideBase
    val _result = __self.bar()
    return _result.objcPtr()
}

@ExportedBridge("OverrideBase_foo")
@OptIn(ExperimentalLibApi::class)
public fun OverrideBase_foo(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OverrideBase
    val _result = __self.foo()
    return _result.objcPtr()
}

@ExportedBridge("OverrideBase_foo_direct", nonVirtualTargetMethod = "foo")
@OptIn(ExperimentalLibApi::class)
public fun OverrideBase_foo_direct(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OverrideBase
    val _result = __self.foo()
    return _result.objcPtr()
}

@ExportedBridge("OverrideDerived_bar")
@OptIn(ExperimentalLibApi::class)
public fun OverrideDerived_bar(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OverrideDerived
    val _result = __self.bar()
    return _result.objcPtr()
}

@ExportedBridge("OverrideDerived_bar_direct", nonVirtualTargetMethod = "bar")
@OptIn(ExperimentalLibApi::class)
public fun OverrideDerived_bar_direct(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OverrideDerived
    val _result = __self.bar()
    return _result.objcPtr()
}

@ExportedBridge("OverrideDerived_foo")
@OptIn(ExperimentalLibApi::class)
public fun OverrideDerived_foo(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OverrideDerived
    val _result = __self.foo()
    return _result.objcPtr()
}

@ExportedBridge("OverrideDerived_foo_direct", nonVirtualTargetMethod = "foo")
@OptIn(ExperimentalLibApi::class)
public fun OverrideDerived_foo_direct(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OverrideDerived
    val _result = __self.foo()
    return _result.objcPtr()
}

@ExportedBridge("RegularLibClass_MarkedNested_init_allocate")
@OptIn(ExperimentalLibApi::class)
public fun RegularLibClass_MarkedNested_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<RegularLibClass.MarkedNested>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("RegularLibClass_MarkedNested_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
@OptIn(ExperimentalLibApi::class)
public fun RegularLibClass_MarkedNested_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, RegularLibClass.MarkedNested())
    return true
}

@ExportedBridge("RegularLibClass_MarkedNested_member")
@OptIn(ExperimentalLibApi::class)
public fun RegularLibClass_MarkedNested_member(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as RegularLibClass.MarkedNested
    val _result = __self.member()
    return _result.objcPtr()
}

@ExportedBridge("SpiAbstractClass_implementMe")
@OptIn(ExperimentalLibApi::class)
public fun SpiAbstractClass_implementMe(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as SpiAbstractClass
    val _result = __self.implementMe()
    return _result.objcPtr()
}

@ExportedBridge("SpiAbstractClass_template")
@OptIn(ExperimentalLibApi::class)
public fun SpiAbstractClass_template(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as SpiAbstractClass
    val _result = __self.template()
    return _result.objcPtr()
}

@ExportedBridge("__root___ExperimentalLibClass_init_allocate")
@OptIn(ExperimentalLibApi::class)
public fun __root___ExperimentalLibClass_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<ExperimentalLibClass>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ExperimentalLibClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
@OptIn(ExperimentalLibApi::class)
public fun __root___ExperimentalLibClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, ExperimentalLibClass())
    return true
}

@ExportedBridge("__root___ExperimentalLibClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__")
@OptIn(ExperimentalLibApi::class)
public fun __root___ExperimentalLibClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, x: Int): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __x = x
    kotlin.native.internal.initInstance(____kt, ExperimentalLibClass(__x))
    return true
}

@ExportedBridge("__root___OpenClass_init_allocate")
@OptIn(OpenClassOptIn::class)
public fun __root___OpenClass_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<OpenClass>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___OpenClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
@OptIn(OpenClassOptIn::class)
public fun __root___OpenClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, OpenClass())
    return true
}

@ExportedBridge("__root___OverrideBase_init_allocate")
public fun __root___OverrideBase_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<OverrideBase>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___OverrideBase_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___OverrideBase_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, OverrideBase())
    return true
}

@ExportedBridge("__root___OverrideDerived_init_allocate")
public fun __root___OverrideDerived_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<OverrideDerived>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___OverrideDerived_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___OverrideDerived_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, OverrideDerived())
    return true
}

@ExportedBridge("__root___RegularLibClass_init_allocate")
public fun __root___RegularLibClass_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<RegularLibClass>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___RegularLibClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__")
@OptIn(InternalLibApi::class)
public fun __root___RegularLibClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(__kt: kotlin.native.internal.NativePtr, a: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __a = interpretObjCPointer<kotlin.String>(a)
    kotlin.native.internal.initInstance(____kt, RegularLibClass(__a))
    return true
}

@ExportedBridge("__root___RegularLibClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___RegularLibClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, RegularLibClass())
    return true
}

@ExportedBridge("__root___SpiAbstractClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__", nonVirtualTargetMethod = "<init>")
@OptIn(ExperimentalLibApi::class)
public fun __root___SpiAbstractClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, SpiAbstractClass())
    return true
}

@ExportedBridge("__root___experimentalLibFunction")
@OptIn(ExperimentalLibApi::class)
public fun __root___experimentalLibFunction(): Boolean {
    experimentalLibFunction()
    return true
}

@ExportedBridge("__root___experimentalLibSetter_get")
public fun __root___experimentalLibSetter_get(): kotlin.native.internal.NativePtr {
    val _result = experimentalLibSetter
    return _result.objcPtr()
}

@ExportedBridge("__root___experimentalLibSetter_set__TypesOfArguments__Swift_String__")
@OptIn(ExperimentalLibApi::class)
public fun __root___experimentalLibSetter_set__TypesOfArguments__Swift_String__(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    experimentalLibSetter = __newValue
    return true
}

@ExportedBridge("__root___experimentalProperty_get")
@OptIn(ExperimentalLibApi::class)
public fun __root___experimentalProperty_get(): kotlin.native.internal.NativePtr {
    val _result = experimentalProperty
    return _result.objcPtr()
}

@ExportedBridge("__root___experimentalProperty_set__TypesOfArguments__Swift_String__")
@OptIn(ExperimentalLibApi::class)
public fun __root___experimentalProperty_set__TypesOfArguments__Swift_String__(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    experimentalProperty = __newValue
    return true
}

@ExportedBridge("__root___fooA__TypesOfArgumentsC1__anyU20lib_InternalLibInterface_lib_ExperimentalLibClass__")
@OptIn(ExperimentalLibApi::class, InternalLibApi::class)
public fun __root___fooA__TypesOfArgumentsC1__anyU20lib_InternalLibInterface_lib_ExperimentalLibClass__(a: kotlin.native.internal.NativePtr, b: kotlin.native.internal.NativePtr): Boolean {
    val __a = kotlin.native.internal.ref.dereferenceExternalRCRef(a) as InternalLibInterface
    val __b = kotlin.native.internal.ref.dereferenceExternalRCRef(b) as ExperimentalLibClass
    context(__a) { fooA(__b) }
    return true
}

@ExportedBridge("__root___fooA_get")
@OptIn(InternalLibApi::class)
public fun __root___fooA_get(): kotlin.native.internal.NativePtr {
    val _result = fooA
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___fooB__TypesOfArgumentsE__anyU20lib_InternalLibInterface__")
@OptIn(ExperimentalLibApi::class, InternalLibApi::class)
public fun __root___fooB__TypesOfArgumentsE__anyU20lib_InternalLibInterface__(`receiver`: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as InternalLibInterface
    val _result = __receiver.fooB()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___fooB_get")
@OptIn(ExperimentalLibApi::class)
public fun __root___fooB_get(): kotlin.native.internal.NativePtr {
    val _result = fooB
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___fooC")
@OptIn(InternalLibApi::class)
public fun __root___fooC(): kotlin.native.internal.NativePtr {
    val _result = fooC()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___fooD")
@OptIn(ExperimentalLibApi::class)
public fun __root___fooD(): kotlin.native.internal.NativePtr {
    val _result = fooD()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___genericFunction__TypesOfArguments__anyU20lib_InternalLibInterface__")
@OptIn(InternalLibApi::class)
public fun __root___genericFunction__TypesOfArguments__anyU20lib_InternalLibInterface__(a: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __a = kotlin.native.internal.ref.dereferenceExternalRCRef(a) as InternalLibInterface
    val _result = genericFunction<InternalLibInterface>(__a)
    return _result.objcPtr()
}

@ExportedBridge("__root___genericProperty_get__TypesOfArgumentsE__anyU20lib_InternalLibInterface__")
@OptIn(InternalLibApi::class)
public fun __root___genericProperty_get__TypesOfArgumentsE__anyU20lib_InternalLibInterface__(`receiver`: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as InternalLibInterface
    val _result = __receiver.genericProperty
    return _result.objcPtr()
}

@ExportedBridge("__root___internalLibFunction")
@OptIn(InternalLibApi::class)
public fun __root___internalLibFunction(): Boolean {
    internalLibFunction()
    return true
}

@ExportedBridge("__root___internalLibSetter_get")
public fun __root___internalLibSetter_get(): kotlin.native.internal.NativePtr {
    val _result = internalLibSetter
    return _result.objcPtr()
}

@ExportedBridge("__root___internalLibSetter_set__TypesOfArguments__Swift_String__")
@OptIn(InternalLibApi::class)
public fun __root___internalLibSetter_set__TypesOfArguments__Swift_String__(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    internalLibSetter = __newValue
    return true
}

@ExportedBridge("__root___internalProperty_get")
@OptIn(InternalLibApi::class)
public fun __root___internalProperty_get(): kotlin.native.internal.NativePtr {
    val _result = internalProperty
    return _result.objcPtr()
}

@ExportedBridge("__root___internalProperty_set__TypesOfArguments__Swift_String__")
@OptIn(InternalLibApi::class)
public fun __root___internalProperty_set__TypesOfArguments__Swift_String__(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    internalProperty = __newValue
    return true
}

@ExportedBridge("__root___mapOfSpi")
@OptIn(ExperimentalLibApi::class)
public fun __root___mapOfSpi(): kotlin.native.internal.NativePtr {
    val _result = mapOfSpi()
    return _result.objcPtr()
}

@ExportedBridge("__root___normalLibFunction")
public fun __root___normalLibFunction(): Boolean {
    normalLibFunction()
    return true
}

@ExportedBridge("__root___returnAlias")
@OptIn(InternalLibApi::class)
public fun __root___returnAlias(): kotlin.native.internal.NativePtr {
    val _result = returnAlias()
    return _result.objcPtr()
}

@ExportedBridge("__root___returnsGenericAlias")
@OptIn(ExperimentalLibApi::class)
public fun __root___returnsGenericAlias(): kotlin.native.internal.NativePtr {
    val _result = returnsGenericAlias()
    return _result.objcPtr()
}

@ExportedBridge("spi_first_firstGroupFunction")
@OptIn(spi.first.OptInMarker::class)
public fun spi_first_firstGroupFunction(): Boolean {
    spi.first.firstGroupFunction()
    return true
}

@ExportedBridge("spi_second_secondGroupFunction")
@OptIn(spi.second.OptInMarker::class)
public fun spi_second_secondGroupFunction(): Boolean {
    spi.second.secondGroupFunction()
    return true
}


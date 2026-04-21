@file:OptIn(ExperimentalLibApi::class, InterfaceOptInOne::class, InterfaceOptInTwo::class, InternalLibApi::class, OpenClassOptIn::class)
@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(ExperimentalLibClass::class, "3lib20ExperimentalLibClassC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OpenClass::class, "3lib9OpenClassC")
@file:kotlin.native.internal.objc.BindClassToObjCName(RegularLibClass::class, "3lib15RegularLibClassC")
@file:kotlin.native.internal.objc.BindClassToObjCName(InterfaceOne::class, "_InterfaceOne")
@file:kotlin.native.internal.objc.BindClassToObjCName(InterfaceTwo::class, "_InterfaceTwo")
@file:kotlin.native.internal.objc.BindClassToObjCName(InternalLibInterface::class, "_InternalLibInterface")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("ExperimentalLibClass_bar")
@OptIn(ExperimentalLibApi::class)
public fun ExperimentalLibClass_bar(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as ExperimentalLibClass
    val _result = run { __self.bar() }
    return run { _result; true }
}

@ExportedBridge("ExperimentalLibClass_foo_get")
@OptIn(ExperimentalLibApi::class)
public fun ExperimentalLibClass_foo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as ExperimentalLibClass
    val _result = run { __self.foo }
    return _result.objcPtr()
}

@ExportedBridge("ExperimentalLibClass_foo_set__TypesOfArguments__Swift_String__")
@OptIn(ExperimentalLibApi::class)
public fun ExperimentalLibClass_foo_set__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as ExperimentalLibClass
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    val _result = run { __self.foo = __newValue }
    return run { _result; true }
}

@ExportedBridge("InternalLibInterface_bar")
@OptIn(InternalLibApi::class)
public fun InternalLibInterface_bar(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as InternalLibInterface
    val _result = run { __self.bar() }
    return run { _result; true }
}

@ExportedBridge("InternalLibInterface_foo_get")
@OptIn(InternalLibApi::class)
public fun InternalLibInterface_foo_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as InternalLibInterface
    val _result = run { __self.foo }
    return _result.objcPtr()
}

@ExportedBridge("InternalLibInterface_foo_set__TypesOfArguments__Swift_String__")
@OptIn(InternalLibApi::class)
public fun InternalLibInterface_foo_set__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as InternalLibInterface
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    val _result = run { __self.foo = __newValue }
    return run { _result; true }
}

@ExportedBridge("__root___ExperimentalLibClass_init_allocate")
@OptIn(ExperimentalLibApi::class)
public fun __root___ExperimentalLibClass_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<ExperimentalLibClass>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ExperimentalLibClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
@OptIn(ExperimentalLibApi::class)
public fun __root___ExperimentalLibClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, ExperimentalLibClass()) }
    return run { _result; true }
}

@ExportedBridge("__root___OpenClass_init_allocate")
@OptIn(OpenClassOptIn::class)
public fun __root___OpenClass_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<OpenClass>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___OpenClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
@OptIn(OpenClassOptIn::class)
public fun __root___OpenClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, OpenClass()) }
    return run { _result; true }
}

@ExportedBridge("__root___RegularLibClass_init_allocate")
public fun __root___RegularLibClass_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<RegularLibClass>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___RegularLibClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__")
@OptIn(InternalLibApi::class)
public fun __root___RegularLibClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(__kt: kotlin.native.internal.NativePtr, a: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __a = interpretObjCPointer<kotlin.String>(a)
    val _result = run { kotlin.native.internal.initInstance(____kt, RegularLibClass(__a)) }
    return run { _result; true }
}

@ExportedBridge("__root___RegularLibClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___RegularLibClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, RegularLibClass()) }
    return run { _result; true }
}

@ExportedBridge("__root___experimentalLibFunction")
@OptIn(ExperimentalLibApi::class)
public fun __root___experimentalLibFunction(): Boolean {
    val _result = run { experimentalLibFunction() }
    return run { _result; true }
}

@ExportedBridge("__root___experimentalLibSetter_get")
public fun __root___experimentalLibSetter_get(): kotlin.native.internal.NativePtr {
    val _result = run { experimentalLibSetter }
    return _result.objcPtr()
}

@ExportedBridge("__root___experimentalLibSetter_set__TypesOfArguments__Swift_String__")
@OptIn(ExperimentalLibApi::class)
public fun __root___experimentalLibSetter_set__TypesOfArguments__Swift_String__(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    val _result = run { experimentalLibSetter = __newValue }
    return run { _result; true }
}

@ExportedBridge("__root___experimentalProperty_get")
@OptIn(ExperimentalLibApi::class)
public fun __root___experimentalProperty_get(): kotlin.native.internal.NativePtr {
    val _result = run { experimentalProperty }
    return _result.objcPtr()
}

@ExportedBridge("__root___experimentalProperty_set__TypesOfArguments__Swift_String__")
@OptIn(ExperimentalLibApi::class)
public fun __root___experimentalProperty_set__TypesOfArguments__Swift_String__(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    val _result = run { experimentalProperty = __newValue }
    return run { _result; true }
}

@ExportedBridge("__root___fooA__TypesOfArgumentsC1__lib_ExperimentalLibClass_anyU20lib_InternalLibInterface__")
@OptIn(ExperimentalLibApi::class, InternalLibApi::class)
public fun __root___fooA__TypesOfArgumentsC1__lib_ExperimentalLibClass_anyU20lib_InternalLibInterface__(b: kotlin.native.internal.NativePtr, a: kotlin.native.internal.NativePtr): Boolean {
    val __b = kotlin.native.internal.ref.dereferenceExternalRCRef(b) as ExperimentalLibClass
    val __a = kotlin.native.internal.ref.dereferenceExternalRCRef(a) as InternalLibInterface
    val _result = run { context(__a) { fooA(__b) } }
    return run { _result; true }
}

@ExportedBridge("__root___fooA_get")
@OptIn(InternalLibApi::class)
public fun __root___fooA_get(): kotlin.native.internal.NativePtr {
    val _result = run { fooA }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___fooB__TypesOfArgumentsE__anyU20lib_InternalLibInterface__")
@OptIn(ExperimentalLibApi::class, InternalLibApi::class)
public fun __root___fooB__TypesOfArgumentsE__anyU20lib_InternalLibInterface__(`receiver`: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as InternalLibInterface
    val _result = run { __receiver.fooB() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___fooB_get")
@OptIn(ExperimentalLibApi::class)
public fun __root___fooB_get(): kotlin.native.internal.NativePtr {
    val _result = run { fooB }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___fooC")
@OptIn(InternalLibApi::class)
public fun __root___fooC(): kotlin.native.internal.NativePtr {
    val _result = run { fooC() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___fooD")
@OptIn(ExperimentalLibApi::class)
public fun __root___fooD(): kotlin.native.internal.NativePtr {
    val _result = run { fooD() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___genericFunction__TypesOfArguments__anyU20lib_InternalLibInterface__")
@OptIn(InternalLibApi::class)
public fun __root___genericFunction__TypesOfArguments__anyU20lib_InternalLibInterface__(a: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __a = kotlin.native.internal.ref.dereferenceExternalRCRef(a) as InternalLibInterface
    val _result = run { genericFunction<InternalLibInterface>(__a) }
    return _result.objcPtr()
}

@ExportedBridge("__root___genericProperty_get__TypesOfArgumentsE__anyU20lib_InternalLibInterface__")
@OptIn(InternalLibApi::class)
public fun __root___genericProperty_get__TypesOfArgumentsE__anyU20lib_InternalLibInterface__(`receiver`: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as InternalLibInterface
    val _result = run { __receiver.genericProperty }
    return _result.objcPtr()
}

@ExportedBridge("__root___internalLibFunction")
@OptIn(InternalLibApi::class)
public fun __root___internalLibFunction(): Boolean {
    val _result = run { internalLibFunction() }
    return run { _result; true }
}

@ExportedBridge("__root___internalLibSetter_get")
public fun __root___internalLibSetter_get(): kotlin.native.internal.NativePtr {
    val _result = run { internalLibSetter }
    return _result.objcPtr()
}

@ExportedBridge("__root___internalLibSetter_set__TypesOfArguments__Swift_String__")
@OptIn(InternalLibApi::class)
public fun __root___internalLibSetter_set__TypesOfArguments__Swift_String__(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    val _result = run { internalLibSetter = __newValue }
    return run { _result; true }
}

@ExportedBridge("__root___internalProperty_get")
@OptIn(InternalLibApi::class)
public fun __root___internalProperty_get(): kotlin.native.internal.NativePtr {
    val _result = run { internalProperty }
    return _result.objcPtr()
}

@ExportedBridge("__root___internalProperty_set__TypesOfArguments__Swift_String__")
@OptIn(InternalLibApi::class)
public fun __root___internalProperty_set__TypesOfArguments__Swift_String__(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    val _result = run { internalProperty = __newValue }
    return run { _result; true }
}

@ExportedBridge("__root___normalLibFunction")
public fun __root___normalLibFunction(): Boolean {
    val _result = run { normalLibFunction() }
    return run { _result; true }
}

@ExportedBridge("__root___returnAlias")
@OptIn(InternalLibApi::class)
public fun __root___returnAlias(): kotlin.native.internal.NativePtr {
    val _result = run { returnAlias() }
    return _result.objcPtr()
}

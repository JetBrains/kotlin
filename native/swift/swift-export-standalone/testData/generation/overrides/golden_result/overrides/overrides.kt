@file:kotlin.native.internal.objc.BindClassToObjCName(Child::class, "9overrides5ChildC")
@file:kotlin.native.internal.objc.BindClassToObjCName(GrandChild::class, "9overrides10GrandChildC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Parent::class, "9overrides6ParentC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("Child_finalOverrideFunc")
public fun Child_finalOverrideFunc(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    __self.finalOverrideFunc()
}

@ExportedBridge("Child_nonoverride")
public fun Child_nonoverride(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = __self.nonoverride()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Child_objectFunc__TypesOfArguments__overrides_Child__")
public fun Child_objectFunc__TypesOfArguments__overrides_Child__(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as Child
    val _result = __self.objectFunc(__arg)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Child_objectOptionalFunc__TypesOfArguments__overrides_Child__")
public fun Child_objectOptionalFunc__TypesOfArguments__overrides_Child__(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as Child
    val _result = __self.objectOptionalFunc(__arg)
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Child_objectOptionalVar_get")
public fun Child_objectOptionalVar_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = __self.objectOptionalVar
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Child_objectVar_get")
public fun Child_objectVar_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = __self.objectVar
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Child_overrideChainFunc")
public fun Child_overrideChainFunc(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    __self.overrideChainFunc()
}

@ExportedBridge("Child_primitiveTypeFunc__TypesOfArguments__Swift_Int32__")
public fun Child_primitiveTypeFunc__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, arg: Int): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val __arg = arg
    val _result = __self.primitiveTypeFunc(__arg)
    return _result
}

@ExportedBridge("Child_primitiveTypeVar_get")
public fun Child_primitiveTypeVar_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = __self.primitiveTypeVar
    return _result
}

@ExportedBridge("Child_subtypeObjectFunc__TypesOfArguments__overrides_Child__")
public fun Child_subtypeObjectFunc__TypesOfArguments__overrides_Child__(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as Child
    val _result = __self.subtypeObjectFunc(__arg)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Child_subtypeObjectVar_get")
public fun Child_subtypeObjectVar_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = __self.subtypeObjectVar
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Child_subtypeOptionalObjectFunc")
public fun Child_subtypeOptionalObjectFunc(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = __self.subtypeOptionalObjectFunc()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Child_subtypeOptionalObjectVar_get")
public fun Child_subtypeOptionalObjectVar_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = __self.subtypeOptionalObjectVar
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Child_subtypeOptionalPrimitiveFunc")
public fun Child_subtypeOptionalPrimitiveFunc(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = __self.subtypeOptionalPrimitiveFunc()
    return _result
}

@ExportedBridge("Child_subtypeOptionalPrimitiveVar_get")
public fun Child_subtypeOptionalPrimitiveVar_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = __self.subtypeOptionalPrimitiveVar
    return _result
}

@ExportedBridge("GrandChild_finalOverrideHopFunc")
public fun GrandChild_finalOverrideHopFunc(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as GrandChild
    __self.finalOverrideHopFunc()
}

@ExportedBridge("GrandChild_hopFunc")
public fun GrandChild_hopFunc(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as GrandChild
    __self.hopFunc()
}

@ExportedBridge("GrandChild_overrideChainFunc")
public fun GrandChild_overrideChainFunc(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as GrandChild
    __self.overrideChainFunc()
}

@ExportedBridge("Parent_finalOverrideFunc")
public fun Parent_finalOverrideFunc(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    __self.finalOverrideFunc()
}

@ExportedBridge("Parent_finalOverrideHopFunc")
public fun Parent_finalOverrideHopFunc(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    __self.finalOverrideHopFunc()
}

@ExportedBridge("Parent_hopFunc")
public fun Parent_hopFunc(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    __self.hopFunc()
}

@ExportedBridge("Parent_nonoverride")
public fun Parent_nonoverride(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = __self.nonoverride()
    return _result
}

@ExportedBridge("Parent_objectFunc__TypesOfArguments__overrides_Child__")
public fun Parent_objectFunc__TypesOfArguments__overrides_Child__(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as Child
    val _result = __self.objectFunc(__arg)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Parent_objectOptionalFunc__TypesOfArguments__overrides_Child__")
public fun Parent_objectOptionalFunc__TypesOfArguments__overrides_Child__(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as Child
    val _result = __self.objectOptionalFunc(__arg)
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Parent_objectOptionalVar_get")
public fun Parent_objectOptionalVar_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = __self.objectOptionalVar
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Parent_objectVar_get")
public fun Parent_objectVar_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = __self.objectVar
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Parent_overrideChainFunc")
public fun Parent_overrideChainFunc(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    __self.overrideChainFunc()
}

@ExportedBridge("Parent_primitiveTypeFunc__TypesOfArguments__Swift_Int32__")
public fun Parent_primitiveTypeFunc__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, arg: Int): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val __arg = arg
    val _result = __self.primitiveTypeFunc(__arg)
    return _result
}

@ExportedBridge("Parent_primitiveTypeVar_get")
public fun Parent_primitiveTypeVar_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = __self.primitiveTypeVar
    return _result
}

@ExportedBridge("Parent_subtypeObjectFunc__TypesOfArguments__overrides_Child__")
public fun Parent_subtypeObjectFunc__TypesOfArguments__overrides_Child__(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as Child
    val _result = __self.subtypeObjectFunc(__arg)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Parent_subtypeObjectVar_get")
public fun Parent_subtypeObjectVar_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = __self.subtypeObjectVar
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Parent_subtypeOptionalObjectFunc")
public fun Parent_subtypeOptionalObjectFunc(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = __self.subtypeOptionalObjectFunc()
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Parent_subtypeOptionalObjectVar_get")
public fun Parent_subtypeOptionalObjectVar_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = __self.subtypeOptionalObjectVar
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Parent_subtypeOptionalPrimitiveFunc")
public fun Parent_subtypeOptionalPrimitiveFunc(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = __self.subtypeOptionalPrimitiveFunc()
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return _result.objcPtr()
}

@ExportedBridge("Parent_subtypeOptionalPrimitiveVar_get")
public fun Parent_subtypeOptionalPrimitiveVar_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = __self.subtypeOptionalPrimitiveVar
    return if (_result == null) return kotlin.native.internal.NativePtr.NULL else return _result.objcPtr()
}

@ExportedBridge("__root___Child_init_allocate")
public fun __root___Child_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Child>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Child_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___Child_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, Child())
}

@ExportedBridge("__root___GrandChild_init_allocate")
public fun __root___GrandChild_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<GrandChild>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___GrandChild_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___GrandChild_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, GrandChild())
}

@ExportedBridge("__root___Parent_init_allocate")
public fun __root___Parent_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Parent>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Parent_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___Parent_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, Parent())
}


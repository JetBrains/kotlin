@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(AbstractBase::class, "9overrides12AbstractBaseC")
@file:kotlin.native.internal.objc.BindClassToObjCName(AbstractDerived2::class, "9overrides16AbstractDerived2C")
@file:kotlin.native.internal.objc.BindClassToObjCName(Child::class, "9overrides5ChildC")
@file:kotlin.native.internal.objc.BindClassToObjCName(GrandChild::class, "9overrides10GrandChildC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OpenDerived1::class, "9overrides12OpenDerived1C")
@file:kotlin.native.internal.objc.BindClassToObjCName(Parent::class, "9overrides6ParentC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("AbstractBase_abstractFun1")
public fun AbstractBase_abstractFun1(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AbstractBase
    __self.abstractFun1()
}

@ExportedBridge("AbstractBase_abstractFun2")
public fun AbstractBase_abstractFun2(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AbstractBase
    __self.abstractFun2()
}

@ExportedBridge("AbstractBase_abstractVal_get")
public fun AbstractBase_abstractVal_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AbstractBase
    val _result = __self.abstractVal
    return _result
}

@ExportedBridge("AbstractDerived2_abstractFun1")
public fun AbstractDerived2_abstractFun1(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AbstractDerived2
    __self.abstractFun1()
}

@ExportedBridge("Child_actuallyOverride__TypesOfArguments__Swift_Int32_opt__overrides_Parent_overrides_Parent_opt___")
public fun Child_actuallyOverride__TypesOfArguments__Swift_Int32_opt__overrides_Parent_overrides_Parent_opt___(self: kotlin.native.internal.NativePtr, nullable: kotlin.native.internal.NativePtr, poly: kotlin.native.internal.NativePtr, nullablePoly: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val __nullable = if (nullable == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<Int>(nullable)
    val __poly = kotlin.native.internal.ref.dereferenceExternalRCRef(poly) as Parent
    val __nullablePoly = if (nullablePoly == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(nullablePoly) as Parent
    __self.actuallyOverride(__nullable, __poly, __nullablePoly)
}

@ExportedBridge("Child_finalOverrideFunc")
public fun Child_finalOverrideFunc(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    __self.finalOverrideFunc()
}

@ExportedBridge("Child_genericReturnTypeFunc")
public fun Child_genericReturnTypeFunc(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = __self.genericReturnTypeFunc()
    return _result.objcPtr()
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

@ExportedBridge("OpenDerived1_abstractFun1")
public fun OpenDerived1_abstractFun1(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OpenDerived1
    __self.abstractFun1()
}

@ExportedBridge("OpenDerived1_abstractFun2")
public fun OpenDerived1_abstractFun2(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OpenDerived1
    __self.abstractFun2()
}

@ExportedBridge("OpenDerived1_abstractVal_get")
public fun OpenDerived1_abstractVal_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OpenDerived1
    val _result = __self.abstractVal
    return _result
}

@ExportedBridge("Parent_actuallyOverride__TypesOfArguments__Swift_Int32_overrides_Child_overrides_Child__")
public fun Parent_actuallyOverride__TypesOfArguments__Swift_Int32_overrides_Child_overrides_Child__(self: kotlin.native.internal.NativePtr, nullable: Int, poly: kotlin.native.internal.NativePtr, nullablePoly: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val __nullable = nullable
    val __poly = kotlin.native.internal.ref.dereferenceExternalRCRef(poly) as Child
    val __nullablePoly = kotlin.native.internal.ref.dereferenceExternalRCRef(nullablePoly) as Child
    __self.actuallyOverride(__nullable, __poly, __nullablePoly)
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

@ExportedBridge("Parent_genericReturnTypeFunc")
public fun Parent_genericReturnTypeFunc(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = __self.genericReturnTypeFunc()
    return _result.objcPtr()
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

@ExportedBridge("Parent_value_get")
public fun Parent_value_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = __self.value
    return _result.objcPtr()
}

@ExportedBridge("__root___Child_init_allocate")
public fun __root___Child_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Child>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Child_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__")
public fun __root___Child_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, value: Int): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __value = value
    kotlin.native.internal.initInstance(____kt, Child(__value))
}

@ExportedBridge("__root___Child_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32_overrides_Parent_overrides_Parent__")
public fun __root___Child_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32_overrides_Parent_overrides_Parent__(__kt: kotlin.native.internal.NativePtr, nullable: Int, poly: kotlin.native.internal.NativePtr, nullablePoly: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __nullable = nullable
    val __poly = kotlin.native.internal.ref.dereferenceExternalRCRef(poly) as Parent
    val __nullablePoly = kotlin.native.internal.ref.dereferenceExternalRCRef(nullablePoly) as Parent
    kotlin.native.internal.initInstance(____kt, Child(__nullable, __poly, __nullablePoly))
}

@ExportedBridge("__root___Child_init_initialize__TypesOfArguments__Swift_UInt_Swift_String__")
public fun __root___Child_init_initialize__TypesOfArguments__Swift_UInt_Swift_String__(__kt: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __value = interpretObjCPointer<kotlin.String>(value)
    kotlin.native.internal.initInstance(____kt, Child(__value))
}

@ExportedBridge("__root___GrandChild_init_allocate")
public fun __root___GrandChild_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<GrandChild>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___GrandChild_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__")
public fun __root___GrandChild_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, value: Int): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __value = value
    kotlin.native.internal.initInstance(____kt, GrandChild(__value))
}

@ExportedBridge("__root___OpenDerived1_init_allocate")
public fun __root___OpenDerived1_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<OpenDerived1>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___OpenDerived1_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___OpenDerived1_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, OpenDerived1())
}

@ExportedBridge("__root___OpenDerived1_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__")
public fun __root___OpenDerived1_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, x: Int): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __x = x
    kotlin.native.internal.initInstance(____kt, OpenDerived1(__x))
}

@ExportedBridge("__root___Parent_init_allocate")
public fun __root___Parent_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Parent>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Parent_init_initialize__TypesOfArguments__Swift_UInt_Swift_String__")
public fun __root___Parent_init_initialize__TypesOfArguments__Swift_UInt_Swift_String__(__kt: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __value = interpretObjCPointer<kotlin.String>(value)
    kotlin.native.internal.initInstance(____kt, Parent(__value))
}


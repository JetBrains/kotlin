@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(AbstractBase::class, "9overrides12AbstractBaseC")
@file:kotlin.native.internal.objc.BindClassToObjCName(AbstractDerived2::class, "9overrides16AbstractDerived2C")
@file:kotlin.native.internal.objc.BindClassToObjCName(Child::class, "9overrides5ChildC")
@file:kotlin.native.internal.objc.BindClassToObjCName(GrandChild::class, "9overrides10GrandChildC")
@file:kotlin.native.internal.objc.BindClassToObjCName(OpenDerived1::class, "9overrides12OpenDerived1C")
@file:kotlin.native.internal.objc.BindClassToObjCName(Parent::class, "9overrides6ParentC")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ImportedBridge("AbstractBase_abstractFun1__reverse_swift")
internal external fun AbstractBase_abstractFun1__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(AbstractBase::class, "abstractFun1")
public fun AbstractBase_abstractFun1__reverse(self: AbstractBase): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = AbstractBase_abstractFun1__reverse_swift(__self)
    return run<Unit> { __result }
}

@ImportedBridge("AbstractBase_abstractFun2__reverse_swift")
internal external fun AbstractBase_abstractFun2__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(AbstractBase::class, "abstractFun2")
public fun AbstractBase_abstractFun2__reverse(self: AbstractBase): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = AbstractBase_abstractFun2__reverse_swift(__self)
    return run<Unit> { __result }
}

@ImportedBridge("AbstractDerived2_abstractFun1__reverse_swift")
internal external fun AbstractDerived2_abstractFun1__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(AbstractDerived2::class, "abstractFun1")
public fun AbstractDerived2_abstractFun1__reverse(self: AbstractDerived2): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = AbstractDerived2_abstractFun1__reverse_swift(__self)
    return run<Unit> { __result }
}

@ImportedBridge("Child_actuallyOverride__TypesOfArguments__Swift_Optional_Swift_Int32__overrides_Parent_Swift_Optional_overrides_Parent_____reverse_swift")
internal external fun Child_actuallyOverride__TypesOfArguments__Swift_Optional_Swift_Int32__overrides_Parent_Swift_Optional_overrides_Parent_____reverse_swift(self: kotlin.native.internal.NativePtr, nullable: kotlin.native.internal.NativePtr, poly: kotlin.native.internal.NativePtr, nullablePoly: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(Child::class, "actuallyOverride")
public fun Child_actuallyOverride__TypesOfArguments__Swift_Optional_Swift_Int32__overrides_Parent_Swift_Optional_overrides_Parent_____reverse(self: Child, nullable: Int?, poly: Parent, nullablePoly: Parent?): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __nullable = if (nullable == null) kotlin.native.internal.NativePtr.NULL else nullable.objcPtr()
    val __poly = kotlin.native.internal.ref.createRetainedExternalRCRef(poly)
    val __nullablePoly = if (nullablePoly == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(nullablePoly)
    val __result = Child_actuallyOverride__TypesOfArguments__Swift_Optional_Swift_Int32__overrides_Parent_Swift_Optional_overrides_Parent_____reverse_swift(__self, __nullable, __poly, __nullablePoly)
    return run<Unit> { __result }
}

@ImportedBridge("Child_contains__TypesOfArguments__Swift_Int32____reverse_swift")
internal external fun Child_contains__TypesOfArguments__Swift_Int32____reverse_swift(self: kotlin.native.internal.NativePtr, element: Int): Boolean

@BindReverseBridgeToMethod(Child::class, "contains")
public fun Child_contains__TypesOfArguments__Swift_Int32____reverse(self: Child, element: Int): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = Child_contains__TypesOfArguments__Swift_Int32____reverse_swift(__self, element)
    return __result
}

@ImportedBridge("Child_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun Child_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, to: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(Child::class, "equals")
public fun Child_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: Child, to: kotlin.Any?): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __to = if (to == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(to)
    val __result = Child_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __to)
    return __result
}

@ImportedBridge("Child_genericReturnTypeFunc__reverse_swift")
internal external fun Child_genericReturnTypeFunc__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Child::class, "genericReturnTypeFunc")
public fun Child_genericReturnTypeFunc__reverse(self: Child): kotlin.collections.List<Child> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = Child_genericReturnTypeFunc__reverse_swift(__self)
    return interpretObjCPointer<kotlin.collections.List<Child>>(__result)
}

@ImportedBridge("Child_nonoverride__reverse_swift")
internal external fun Child_nonoverride__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(Child::class, "nonoverride")
public fun Child_nonoverride__reverse(self: Child): Nothing {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = Child_nonoverride__reverse_swift(__self)
    return run { __result; throw IllegalStateException() }
}

@ImportedBridge("Child_objectFunc__TypesOfArguments__overrides_Child____reverse_swift")
internal external fun Child_objectFunc__TypesOfArguments__overrides_Child____reverse_swift(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Child::class, "objectFunc")
public fun Child_objectFunc__TypesOfArguments__overrides_Child____reverse(self: Child, arg: Child): Parent {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __arg = kotlin.native.internal.ref.createRetainedExternalRCRef(arg)
    val __result = Child_objectFunc__TypesOfArguments__overrides_Child____reverse_swift(__self, __arg)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as Parent
}

@ImportedBridge("Child_objectOptionalFunc__TypesOfArguments__overrides_Child____reverse_swift")
internal external fun Child_objectOptionalFunc__TypesOfArguments__overrides_Child____reverse_swift(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Child::class, "objectOptionalFunc")
public fun Child_objectOptionalFunc__TypesOfArguments__overrides_Child____reverse(self: Child, arg: Child): Parent? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __arg = kotlin.native.internal.ref.createRetainedExternalRCRef(arg)
    val __result = Child_objectOptionalFunc__TypesOfArguments__overrides_Child____reverse_swift(__self, __arg)
    return if (__result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as Parent
}

@ImportedBridge("Child_overrideChainFunc__reverse_swift")
internal external fun Child_overrideChainFunc__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(Child::class, "overrideChainFunc")
public fun Child_overrideChainFunc__reverse(self: Child): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = Child_overrideChainFunc__reverse_swift(__self)
    return run<Unit> { __result }
}

@ImportedBridge("Child_primitiveTypeFunc__TypesOfArguments__Swift_Int32____reverse_swift")
internal external fun Child_primitiveTypeFunc__TypesOfArguments__Swift_Int32____reverse_swift(self: kotlin.native.internal.NativePtr, arg: Int): Int

@BindReverseBridgeToMethod(Child::class, "primitiveTypeFunc")
public fun Child_primitiveTypeFunc__TypesOfArguments__Swift_Int32____reverse(self: Child, arg: Int): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = Child_primitiveTypeFunc__TypesOfArguments__Swift_Int32____reverse_swift(__self, arg)
    return __result
}

@ImportedBridge("Child_subtypeObjectFunc__TypesOfArguments__overrides_Child____reverse_swift")
internal external fun Child_subtypeObjectFunc__TypesOfArguments__overrides_Child____reverse_swift(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Child::class, "subtypeObjectFunc")
public fun Child_subtypeObjectFunc__TypesOfArguments__overrides_Child____reverse(self: Child, arg: Child): Child {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __arg = kotlin.native.internal.ref.createRetainedExternalRCRef(arg)
    val __result = Child_subtypeObjectFunc__TypesOfArguments__overrides_Child____reverse_swift(__self, __arg)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as Child
}

@ImportedBridge("Child_subtypeOptionalObjectFunc__reverse_swift")
internal external fun Child_subtypeOptionalObjectFunc__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Child::class, "subtypeOptionalObjectFunc")
public fun Child_subtypeOptionalObjectFunc__reverse(self: Child): Child {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = Child_subtypeOptionalObjectFunc__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as Child
}

@ImportedBridge("Child_subtypeOptionalPrimitiveFunc__reverse_swift")
internal external fun Child_subtypeOptionalPrimitiveFunc__reverse_swift(self: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(Child::class, "subtypeOptionalPrimitiveFunc")
public fun Child_subtypeOptionalPrimitiveFunc__reverse(self: Child): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = Child_subtypeOptionalPrimitiveFunc__reverse_swift(__self)
    return __result
}

@ImportedBridge("OpenDerived1_abstractFun1__reverse_swift")
internal external fun OpenDerived1_abstractFun1__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(OpenDerived1::class, "abstractFun1")
public fun OpenDerived1_abstractFun1__reverse(self: OpenDerived1): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = OpenDerived1_abstractFun1__reverse_swift(__self)
    return run<Unit> { __result }
}

@ImportedBridge("OpenDerived1_abstractFun2__reverse_swift")
internal external fun OpenDerived1_abstractFun2__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(OpenDerived1::class, "abstractFun2")
public fun OpenDerived1_abstractFun2__reverse(self: OpenDerived1): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = OpenDerived1_abstractFun2__reverse_swift(__self)
    return run<Unit> { __result }
}

@ImportedBridge("Parent_actuallyOverride__TypesOfArguments__Swift_Int32_overrides_Child_overrides_Child____reverse_swift")
internal external fun Parent_actuallyOverride__TypesOfArguments__Swift_Int32_overrides_Child_overrides_Child____reverse_swift(self: kotlin.native.internal.NativePtr, nullable: Int, poly: kotlin.native.internal.NativePtr, nullablePoly: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(Parent::class, "actuallyOverride")
public fun Parent_actuallyOverride__TypesOfArguments__Swift_Int32_overrides_Child_overrides_Child____reverse(self: Parent, nullable: Int, poly: Child, nullablePoly: Child): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __poly = kotlin.native.internal.ref.createRetainedExternalRCRef(poly)
    val __nullablePoly = kotlin.native.internal.ref.createRetainedExternalRCRef(nullablePoly)
    val __result = Parent_actuallyOverride__TypesOfArguments__Swift_Int32_overrides_Child_overrides_Child____reverse_swift(__self, nullable, __poly, __nullablePoly)
    return run<Unit> { __result }
}

@ImportedBridge("Parent_contains__TypesOfArguments__Swift_Int32____reverse_swift")
internal external fun Parent_contains__TypesOfArguments__Swift_Int32____reverse_swift(self: kotlin.native.internal.NativePtr, element: Int): Boolean

@BindReverseBridgeToMethod(Parent::class, "contains")
public fun Parent_contains__TypesOfArguments__Swift_Int32____reverse(self: Parent, element: Int): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = Parent_contains__TypesOfArguments__Swift_Int32____reverse_swift(__self, element)
    return __result
}

@ImportedBridge("Parent_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun Parent_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, to: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(Parent::class, "equals")
public fun Parent_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: Parent, to: kotlin.Any?): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __to = if (to == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(to)
    val __result = Parent_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __to)
    return __result
}

@ImportedBridge("Parent_finalOverrideFunc__reverse_swift")
internal external fun Parent_finalOverrideFunc__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(Parent::class, "finalOverrideFunc")
public fun Parent_finalOverrideFunc__reverse(self: Parent): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = Parent_finalOverrideFunc__reverse_swift(__self)
    return run<Unit> { __result }
}

@ImportedBridge("Parent_finalOverrideHopFunc__reverse_swift")
internal external fun Parent_finalOverrideHopFunc__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(Parent::class, "finalOverrideHopFunc")
public fun Parent_finalOverrideHopFunc__reverse(self: Parent): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = Parent_finalOverrideHopFunc__reverse_swift(__self)
    return run<Unit> { __result }
}

@ImportedBridge("Parent_genericReturnTypeFunc__reverse_swift")
internal external fun Parent_genericReturnTypeFunc__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Parent::class, "genericReturnTypeFunc")
public fun Parent_genericReturnTypeFunc__reverse(self: Parent): kotlin.collections.List<Parent> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = Parent_genericReturnTypeFunc__reverse_swift(__self)
    return interpretObjCPointer<kotlin.collections.List<Parent>>(__result)
}

@ImportedBridge("Parent_hopFunc__reverse_swift")
internal external fun Parent_hopFunc__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(Parent::class, "hopFunc")
public fun Parent_hopFunc__reverse(self: Parent): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = Parent_hopFunc__reverse_swift(__self)
    return run<Unit> { __result }
}

@ImportedBridge("Parent_nonoverride__reverse_swift")
internal external fun Parent_nonoverride__reverse_swift(self: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(Parent::class, "nonoverride")
public fun Parent_nonoverride__reverse(self: Parent): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = Parent_nonoverride__reverse_swift(__self)
    return __result
}

@ImportedBridge("Parent_objectFunc__TypesOfArguments__overrides_Child____reverse_swift")
internal external fun Parent_objectFunc__TypesOfArguments__overrides_Child____reverse_swift(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Parent::class, "objectFunc")
public fun Parent_objectFunc__TypesOfArguments__overrides_Child____reverse(self: Parent, arg: Child): Parent {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __arg = kotlin.native.internal.ref.createRetainedExternalRCRef(arg)
    val __result = Parent_objectFunc__TypesOfArguments__overrides_Child____reverse_swift(__self, __arg)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as Parent
}

@ImportedBridge("Parent_objectOptionalFunc__TypesOfArguments__overrides_Child____reverse_swift")
internal external fun Parent_objectOptionalFunc__TypesOfArguments__overrides_Child____reverse_swift(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Parent::class, "objectOptionalFunc")
public fun Parent_objectOptionalFunc__TypesOfArguments__overrides_Child____reverse(self: Parent, arg: Child): Parent? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __arg = kotlin.native.internal.ref.createRetainedExternalRCRef(arg)
    val __result = Parent_objectOptionalFunc__TypesOfArguments__overrides_Child____reverse_swift(__self, __arg)
    return if (__result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as Parent
}

@ImportedBridge("Parent_overrideChainFunc__reverse_swift")
internal external fun Parent_overrideChainFunc__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(Parent::class, "overrideChainFunc")
public fun Parent_overrideChainFunc__reverse(self: Parent): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = Parent_overrideChainFunc__reverse_swift(__self)
    return run<Unit> { __result }
}

@ImportedBridge("Parent_primitiveTypeFunc__TypesOfArguments__Swift_Int32____reverse_swift")
internal external fun Parent_primitiveTypeFunc__TypesOfArguments__Swift_Int32____reverse_swift(self: kotlin.native.internal.NativePtr, arg: Int): Int

@BindReverseBridgeToMethod(Parent::class, "primitiveTypeFunc")
public fun Parent_primitiveTypeFunc__TypesOfArguments__Swift_Int32____reverse(self: Parent, arg: Int): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = Parent_primitiveTypeFunc__TypesOfArguments__Swift_Int32____reverse_swift(__self, arg)
    return __result
}

@ImportedBridge("Parent_subtypeObjectFunc__TypesOfArguments__overrides_Child____reverse_swift")
internal external fun Parent_subtypeObjectFunc__TypesOfArguments__overrides_Child____reverse_swift(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Parent::class, "subtypeObjectFunc")
public fun Parent_subtypeObjectFunc__TypesOfArguments__overrides_Child____reverse(self: Parent, arg: Child): Parent {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __arg = kotlin.native.internal.ref.createRetainedExternalRCRef(arg)
    val __result = Parent_subtypeObjectFunc__TypesOfArguments__overrides_Child____reverse_swift(__self, __arg)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as Parent
}

@ImportedBridge("Parent_subtypeOptionalObjectFunc__reverse_swift")
internal external fun Parent_subtypeOptionalObjectFunc__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Parent::class, "subtypeOptionalObjectFunc")
public fun Parent_subtypeOptionalObjectFunc__reverse(self: Parent): Parent? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = Parent_subtypeOptionalObjectFunc__reverse_swift(__self)
    return if (__result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as Parent
}

@ImportedBridge("Parent_subtypeOptionalPrimitiveFunc__reverse_swift")
internal external fun Parent_subtypeOptionalPrimitiveFunc__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Parent::class, "subtypeOptionalPrimitiveFunc")
public fun Parent_subtypeOptionalPrimitiveFunc__reverse(self: Parent): Int? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = Parent_subtypeOptionalPrimitiveFunc__reverse_swift(__self)
    return if (__result == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<Int>(__result)
}

@ExportedBridge("AbstractBase_abstractFun1")
public fun AbstractBase_abstractFun1(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AbstractBase
    val _result = run { __self.abstractFun1() }
    return run { _result; true }
}

@ExportedBridge("AbstractBase_abstractFun2")
public fun AbstractBase_abstractFun2(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AbstractBase
    val _result = run { __self.abstractFun2() }
    return run { _result; true }
}

@ExportedBridge("AbstractBase_abstractVal_get")
public fun AbstractBase_abstractVal_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AbstractBase
    val _result = run { __self.abstractVal }
    return _result
}

@ExportedBridge("AbstractDerived2_abstractFun1")
public fun AbstractDerived2_abstractFun1(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AbstractDerived2
    val _result = run { __self.abstractFun1() }
    return run { _result; true }
}

@ExportedBridge("Child_actuallyOverride__TypesOfArguments__Swift_Optional_Swift_Int32__overrides_Parent_Swift_Optional_overrides_Parent___")
public fun Child_actuallyOverride__TypesOfArguments__Swift_Optional_Swift_Int32__overrides_Parent_Swift_Optional_overrides_Parent___(self: kotlin.native.internal.NativePtr, nullable: kotlin.native.internal.NativePtr, poly: kotlin.native.internal.NativePtr, nullablePoly: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val __nullable = if (nullable == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<Int>(nullable)
    val __poly = kotlin.native.internal.ref.dereferenceExternalRCRef(poly) as Parent
    val __nullablePoly = if (nullablePoly == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(nullablePoly) as Parent
    val _result = run { __self.actuallyOverride(__nullable, __poly, __nullablePoly) }
    return run { _result; true }
}

@ExportedBridge("Child_contains__TypesOfArguments__Swift_Int32__")
public fun Child_contains__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, element: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val __element = element
    val _result = run { __self.contains(__element) }
    return _result
}

@ExportedBridge("Child_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun Child_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, to: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val __to = if (to == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(to) as kotlin.Any
    val _result = run { __self.equals(__to) }
    return _result
}

@ExportedBridge("Child_finalOverrideFunc")
public fun Child_finalOverrideFunc(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = run { __self.finalOverrideFunc() }
    return run { _result; true }
}

@ExportedBridge("Child_genericReturnTypeFunc")
public fun Child_genericReturnTypeFunc(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = run { __self.genericReturnTypeFunc() }
    return _result.objcPtr()
}

@ExportedBridge("Child_nonoverride")
public fun Child_nonoverride(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = run { __self.nonoverride() }
    return _result
}

@ExportedBridge("Child_objectFunc__TypesOfArguments__overrides_Child__")
public fun Child_objectFunc__TypesOfArguments__overrides_Child__(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as Child
    val _result = run { __self.objectFunc(__arg) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Child_objectOptionalFunc__TypesOfArguments__overrides_Child__")
public fun Child_objectOptionalFunc__TypesOfArguments__overrides_Child__(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as Child
    val _result = run { __self.objectOptionalFunc(__arg) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Child_objectOptionalVar_get")
public fun Child_objectOptionalVar_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = run { __self.objectOptionalVar }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Child_objectVar_get")
public fun Child_objectVar_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = run { __self.objectVar }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Child_overrideChainFunc")
public fun Child_overrideChainFunc(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = run { __self.overrideChainFunc() }
    return run { _result; true }
}

@ExportedBridge("Child_primitiveTypeFunc__TypesOfArguments__Swift_Int32__")
public fun Child_primitiveTypeFunc__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, arg: Int): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val __arg = arg
    val _result = run { __self.primitiveTypeFunc(__arg) }
    return _result
}

@ExportedBridge("Child_primitiveTypeVar_get")
public fun Child_primitiveTypeVar_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = run { __self.primitiveTypeVar }
    return _result
}

@ExportedBridge("Child_subtypeObjectFunc__TypesOfArguments__overrides_Child__")
public fun Child_subtypeObjectFunc__TypesOfArguments__overrides_Child__(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as Child
    val _result = run { __self.subtypeObjectFunc(__arg) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Child_subtypeObjectVar_get")
public fun Child_subtypeObjectVar_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = run { __self.subtypeObjectVar }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Child_subtypeOptionalObjectFunc")
public fun Child_subtypeOptionalObjectFunc(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = run { __self.subtypeOptionalObjectFunc() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Child_subtypeOptionalObjectVar_get")
public fun Child_subtypeOptionalObjectVar_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = run { __self.subtypeOptionalObjectVar }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Child_subtypeOptionalPrimitiveFunc")
public fun Child_subtypeOptionalPrimitiveFunc(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = run { __self.subtypeOptionalPrimitiveFunc() }
    return _result
}

@ExportedBridge("Child_subtypeOptionalPrimitiveVar_get")
public fun Child_subtypeOptionalPrimitiveVar_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Child
    val _result = run { __self.subtypeOptionalPrimitiveVar }
    return _result
}

@ExportedBridge("GrandChild_finalOverrideHopFunc")
public fun GrandChild_finalOverrideHopFunc(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as GrandChild
    val _result = run { __self.finalOverrideHopFunc() }
    return run { _result; true }
}

@ExportedBridge("GrandChild_hopFunc")
public fun GrandChild_hopFunc(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as GrandChild
    val _result = run { __self.hopFunc() }
    return run { _result; true }
}

@ExportedBridge("GrandChild_overrideChainFunc")
public fun GrandChild_overrideChainFunc(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as GrandChild
    val _result = run { __self.overrideChainFunc() }
    return run { _result; true }
}

@ExportedBridge("OpenDerived1_abstractFun1")
public fun OpenDerived1_abstractFun1(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OpenDerived1
    val _result = run { __self.abstractFun1() }
    return run { _result; true }
}

@ExportedBridge("OpenDerived1_abstractFun2")
public fun OpenDerived1_abstractFun2(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OpenDerived1
    val _result = run { __self.abstractFun2() }
    return run { _result; true }
}

@ExportedBridge("OpenDerived1_abstractVal_get")
public fun OpenDerived1_abstractVal_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OpenDerived1
    val _result = run { __self.abstractVal }
    return _result
}

@ExportedBridge("Parent_actuallyOverride__TypesOfArguments__Swift_Int32_overrides_Child_overrides_Child__")
public fun Parent_actuallyOverride__TypesOfArguments__Swift_Int32_overrides_Child_overrides_Child__(self: kotlin.native.internal.NativePtr, nullable: Int, poly: kotlin.native.internal.NativePtr, nullablePoly: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val __nullable = nullable
    val __poly = kotlin.native.internal.ref.dereferenceExternalRCRef(poly) as Child
    val __nullablePoly = kotlin.native.internal.ref.dereferenceExternalRCRef(nullablePoly) as Child
    val _result = run { __self.actuallyOverride(__nullable, __poly, __nullablePoly) }
    return run { _result; true }
}

@ExportedBridge("Parent_contains__TypesOfArguments__Swift_Int32__")
public fun Parent_contains__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, element: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val __element = element
    val _result = run { __self.contains(__element) }
    return _result
}

@ExportedBridge("Parent_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun Parent_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, to: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val __to = if (to == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(to) as kotlin.Any
    val _result = run { __self.equals(__to) }
    return _result
}

@ExportedBridge("Parent_finalOverrideFunc")
public fun Parent_finalOverrideFunc(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = run { __self.finalOverrideFunc() }
    return run { _result; true }
}

@ExportedBridge("Parent_finalOverrideHopFunc")
public fun Parent_finalOverrideHopFunc(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = run { __self.finalOverrideHopFunc() }
    return run { _result; true }
}

@ExportedBridge("Parent_genericReturnTypeFunc")
public fun Parent_genericReturnTypeFunc(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = run { __self.genericReturnTypeFunc() }
    return _result.objcPtr()
}

@ExportedBridge("Parent_hopFunc")
public fun Parent_hopFunc(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = run { __self.hopFunc() }
    return run { _result; true }
}

@ExportedBridge("Parent_nonoverride")
public fun Parent_nonoverride(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = run { __self.nonoverride() }
    return _result
}

@ExportedBridge("Parent_objectFunc__TypesOfArguments__overrides_Child__")
public fun Parent_objectFunc__TypesOfArguments__overrides_Child__(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as Child
    val _result = run { __self.objectFunc(__arg) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Parent_objectOptionalFunc__TypesOfArguments__overrides_Child__")
public fun Parent_objectOptionalFunc__TypesOfArguments__overrides_Child__(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as Child
    val _result = run { __self.objectOptionalFunc(__arg) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Parent_objectOptionalVar_get")
public fun Parent_objectOptionalVar_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = run { __self.objectOptionalVar }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Parent_objectVar_get")
public fun Parent_objectVar_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = run { __self.objectVar }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Parent_overrideChainFunc")
public fun Parent_overrideChainFunc(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = run { __self.overrideChainFunc() }
    return run { _result; true }
}

@ExportedBridge("Parent_primitiveTypeFunc__TypesOfArguments__Swift_Int32__")
public fun Parent_primitiveTypeFunc__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, arg: Int): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val __arg = arg
    val _result = run { __self.primitiveTypeFunc(__arg) }
    return _result
}

@ExportedBridge("Parent_primitiveTypeVar_get")
public fun Parent_primitiveTypeVar_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = run { __self.primitiveTypeVar }
    return _result
}

@ExportedBridge("Parent_subtypeObjectFunc__TypesOfArguments__overrides_Child__")
public fun Parent_subtypeObjectFunc__TypesOfArguments__overrides_Child__(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as Child
    val _result = run { __self.subtypeObjectFunc(__arg) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Parent_subtypeObjectVar_get")
public fun Parent_subtypeObjectVar_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = run { __self.subtypeObjectVar }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Parent_subtypeOptionalObjectFunc")
public fun Parent_subtypeOptionalObjectFunc(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = run { __self.subtypeOptionalObjectFunc() }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Parent_subtypeOptionalObjectVar_get")
public fun Parent_subtypeOptionalObjectVar_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = run { __self.subtypeOptionalObjectVar }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Parent_subtypeOptionalPrimitiveFunc")
public fun Parent_subtypeOptionalPrimitiveFunc(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = run { __self.subtypeOptionalPrimitiveFunc() }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else _result.objcPtr()
}

@ExportedBridge("Parent_subtypeOptionalPrimitiveVar_get")
public fun Parent_subtypeOptionalPrimitiveVar_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = run { __self.subtypeOptionalPrimitiveVar }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else _result.objcPtr()
}

@ExportedBridge("Parent_value_get")
public fun Parent_value_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Parent
    val _result = run { __self.value }
    return _result.objcPtr()
}

@ExportedBridge("__root___Child_init_allocate")
public fun __root___Child_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<Child>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Child_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__")
public fun __root___Child_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, value: Int): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __value = value
    val _result = run { kotlin.native.internal.initInstance(____kt, Child(__value)) }
    return run { _result; true }
}

@ExportedBridge("__root___Child_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_overrides_Parent_overrides_Parent__")
public fun __root___Child_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_overrides_Parent_overrides_Parent__(__kt: kotlin.native.internal.NativePtr, nullable: Int, poly: kotlin.native.internal.NativePtr, nullablePoly: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __nullable = nullable
    val __poly = kotlin.native.internal.ref.dereferenceExternalRCRef(poly) as Parent
    val __nullablePoly = kotlin.native.internal.ref.dereferenceExternalRCRef(nullablePoly) as Parent
    val _result = run { kotlin.native.internal.initInstance(____kt, Child(__nullable, __poly, __nullablePoly)) }
    return run { _result; true }
}

@ExportedBridge("__root___Child_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__")
public fun __root___Child_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(__kt: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __value = interpretObjCPointer<kotlin.String>(value)
    val _result = run { kotlin.native.internal.initInstance(____kt, Child(__value)) }
    return run { _result; true }
}

@ExportedBridge("__root___GrandChild_init_allocate")
public fun __root___GrandChild_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<GrandChild>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___GrandChild_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__")
public fun __root___GrandChild_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, value: Int): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __value = value
    val _result = run { kotlin.native.internal.initInstance(____kt, GrandChild(__value)) }
    return run { _result; true }
}

@ExportedBridge("__root___OpenDerived1_init_allocate")
public fun __root___OpenDerived1_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<OpenDerived1>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___OpenDerived1_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___OpenDerived1_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, OpenDerived1()) }
    return run { _result; true }
}

@ExportedBridge("__root___OpenDerived1_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__")
public fun __root___OpenDerived1_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, x: Int): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __x = x
    val _result = run { kotlin.native.internal.initInstance(____kt, OpenDerived1(__x)) }
    return run { _result; true }
}

@ExportedBridge("__root___Parent_init_allocate")
public fun __root___Parent_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<Parent>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Parent_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__")
public fun __root___Parent_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(__kt: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __value = interpretObjCPointer<kotlin.String>(value)
    val _result = run { kotlin.native.internal.initInstance(____kt, Parent(__value)) }
    return run { _result; true }
}

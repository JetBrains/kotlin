@file:OptIn(org.kotlin.foo.OptInA::class, org.kotlin.foo.OptInB::class)
@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.ClassC::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE6ClassCC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.ClassD::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE6ClassDC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.ClassE::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE6ClassEC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.DeprecatedErrorSubClass::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE23DeprecatedErrorSubClassC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.DeprecatedWarningSubClass::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE25DeprecatedWarningSubClassC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.MyClassA::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE8MyClassAC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.MyClassA.Inner::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE8MyClassAC5InnerC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.MyClassB::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE8MyClassBC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.MyClassB.Inner::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE8MyClassBC5InnerC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.MySealedClass::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE13MySealedClassC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.NonDeprecatedSubClassA::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE22NonDeprecatedSubClassAC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.NonDeprecatedSubClassB::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE22NonDeprecatedSubClassBC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.NonSealedNonOptInClassA::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE23NonSealedNonOptInClassAC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.NonSealedNonOptInClassB::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE23NonSealedNonOptInClassBC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.NonSealedOptInClass::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE19NonSealedOptInClassC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.SealedClassA::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE12SealedClassAC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.SealedClassB::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE12SealedClassBC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.SealedClassDeprecatedError::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE26SealedClassDeprecatedErrorC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.SealedClassDeprecatedWarning::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE28SealedClassDeprecatedWarningC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.SealedClassNonDeprecated::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE24SealedClassNonDeprecatedC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.SealedNonOptInClass::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE19SealedNonOptInClassC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.SealedOptInClass::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO6commonE16SealedOptInClassC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.InterfaceC::class, "_InterfaceC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.QueryResult::class, "_QueryResult")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.SealedInterfaceA::class, "_SealedInterfaceA")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.SealedInterfaceB::class, "_SealedInterfaceB")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.QueryResult.AsyncValue::class, "6common61_ExportedKotlinPackages_org_kotlin_foo_QueryResult_AsyncValueC")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.QueryResult.Value::class, "6common56_ExportedKotlinPackages_org_kotlin_foo_QueryResult_ValueC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("org_kotlin_foo_ClassC_init_allocate")
public fun org_kotlin_foo_ClassC_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<org.kotlin.foo.ClassC>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_kotlin_foo_ClassC_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun org_kotlin_foo_ClassC_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, org.kotlin.foo.ClassC()) }
    return run { _result; true }
}

@ExportedBridge("org_kotlin_foo_ClassD_init_allocate")
public fun org_kotlin_foo_ClassD_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<org.kotlin.foo.ClassD>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_kotlin_foo_ClassD_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun org_kotlin_foo_ClassD_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, org.kotlin.foo.ClassD()) }
    return run { _result; true }
}

@ExportedBridge("org_kotlin_foo_ClassE_init_allocate")
public fun org_kotlin_foo_ClassE_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<org.kotlin.foo.ClassE>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_kotlin_foo_ClassE_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun org_kotlin_foo_ClassE_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, org.kotlin.foo.ClassE()) }
    return run { _result; true }
}

@ExportedBridge("org_kotlin_foo_DeprecatedErrorSubClass_init_allocate")
public fun org_kotlin_foo_DeprecatedErrorSubClass_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<org.kotlin.foo.DeprecatedErrorSubClass>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_kotlin_foo_DeprecatedErrorSubClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun org_kotlin_foo_DeprecatedErrorSubClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, org.kotlin.foo.DeprecatedErrorSubClass()) }
    return run { _result; true }
}

@ExportedBridge("org_kotlin_foo_DeprecatedWarningSubClass_init_allocate")
public fun org_kotlin_foo_DeprecatedWarningSubClass_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<org.kotlin.foo.DeprecatedWarningSubClass>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_kotlin_foo_DeprecatedWarningSubClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun org_kotlin_foo_DeprecatedWarningSubClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, org.kotlin.foo.DeprecatedWarningSubClass()) }
    return run { _result; true }
}

@ExportedBridge("org_kotlin_foo_MyClassA_init_allocate")
public fun org_kotlin_foo_MyClassA_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<org.kotlin.foo.MyClassA>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_kotlin_foo_MyClassA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun org_kotlin_foo_MyClassA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, org.kotlin.foo.MyClassA()) }
    return run { _result; true }
}

@ExportedBridge("org_kotlin_foo_MyClassB_init_allocate")
public fun org_kotlin_foo_MyClassB_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<org.kotlin.foo.MyClassB>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_kotlin_foo_MyClassB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun org_kotlin_foo_MyClassB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, org.kotlin.foo.MyClassB()) }
    return run { _result; true }
}

@ExportedBridge("org_kotlin_foo_NonDeprecatedSubClassA_init_allocate")
public fun org_kotlin_foo_NonDeprecatedSubClassA_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<org.kotlin.foo.NonDeprecatedSubClassA>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_kotlin_foo_NonDeprecatedSubClassA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun org_kotlin_foo_NonDeprecatedSubClassA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, org.kotlin.foo.NonDeprecatedSubClassA()) }
    return run { _result; true }
}

@ExportedBridge("org_kotlin_foo_NonDeprecatedSubClassB_init_allocate")
public fun org_kotlin_foo_NonDeprecatedSubClassB_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<org.kotlin.foo.NonDeprecatedSubClassB>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_kotlin_foo_NonDeprecatedSubClassB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun org_kotlin_foo_NonDeprecatedSubClassB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, org.kotlin.foo.NonDeprecatedSubClassB()) }
    return run { _result; true }
}

@ExportedBridge("org_kotlin_foo_NonSealedNonOptInClassA_init_allocate")
public fun org_kotlin_foo_NonSealedNonOptInClassA_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<org.kotlin.foo.NonSealedNonOptInClassA>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_kotlin_foo_NonSealedNonOptInClassA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun org_kotlin_foo_NonSealedNonOptInClassA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, org.kotlin.foo.NonSealedNonOptInClassA()) }
    return run { _result; true }
}

@ExportedBridge("org_kotlin_foo_NonSealedNonOptInClassB_init_allocate")
@OptIn(org.kotlin.foo.OptInA::class)
public fun org_kotlin_foo_NonSealedNonOptInClassB_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<org.kotlin.foo.NonSealedNonOptInClassB>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_kotlin_foo_NonSealedNonOptInClassB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
@OptIn(org.kotlin.foo.OptInA::class)
public fun org_kotlin_foo_NonSealedNonOptInClassB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, org.kotlin.foo.NonSealedNonOptInClassB()) }
    return run { _result; true }
}

@ExportedBridge("org_kotlin_foo_NonSealedOptInClass_init_allocate")
@OptIn(org.kotlin.foo.OptInA::class, org.kotlin.foo.OptInB::class)
public fun org_kotlin_foo_NonSealedOptInClass_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<org.kotlin.foo.NonSealedOptInClass>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_kotlin_foo_NonSealedOptInClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
@OptIn(org.kotlin.foo.OptInA::class, org.kotlin.foo.OptInB::class)
public fun org_kotlin_foo_NonSealedOptInClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, org.kotlin.foo.NonSealedOptInClass()) }
    return run { _result; true }
}

@ExportedBridge("org_kotlin_foo_QueryResult_AsyncValue_init_allocate")
public fun org_kotlin_foo_QueryResult_AsyncValue_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<org.kotlin.foo.QueryResult.AsyncValue<kotlin.Any?>>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_kotlin_foo_QueryResult_AsyncValue_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun org_kotlin_foo_QueryResult_AsyncValue_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(__kt: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.Any
    val _result = run { kotlin.native.internal.initInstance(____kt, org.kotlin.foo.QueryResult.AsyncValue<kotlin.Any?>(__value)) }
    return run { _result; true }
}

@ExportedBridge("org_kotlin_foo_QueryResult_AsyncValue_value_get")
public fun org_kotlin_foo_QueryResult_AsyncValue_value_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as org.kotlin.foo.QueryResult.AsyncValue<kotlin.Any?>
    val _result = run { __self.value }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_kotlin_foo_QueryResult_Value_init_allocate")
public fun org_kotlin_foo_QueryResult_Value_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<org.kotlin.foo.QueryResult.Value<kotlin.Any?>>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_kotlin_foo_QueryResult_Value_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun org_kotlin_foo_QueryResult_Value_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(__kt: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.Any
    val _result = run { kotlin.native.internal.initInstance(____kt, org.kotlin.foo.QueryResult.Value<kotlin.Any?>(__value)) }
    return run { _result; true }
}

@ExportedBridge("org_kotlin_foo_QueryResult_Value_value_get")
public fun org_kotlin_foo_QueryResult_Value_value_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as org.kotlin.foo.QueryResult.Value<kotlin.Any?>
    val _result = run { __self.value }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

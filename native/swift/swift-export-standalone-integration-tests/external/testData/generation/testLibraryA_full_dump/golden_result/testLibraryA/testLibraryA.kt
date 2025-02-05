@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.jetbrains.a.MyLibraryA::class, "22ExportedKotlinPackages3orgO9jetbrainsO1aO12testLibraryAE10MyLibraryAC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("org_jetbrains_a_MyLibraryA_init_allocate")
public fun org_jetbrains_a_MyLibraryA_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<org.jetbrains.a.MyLibraryA>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_jetbrains_a_MyLibraryA_init_initialize__TypesOfArguments__Swift_UInt__")
public fun org_jetbrains_a_MyLibraryA_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, org.jetbrains.a.MyLibraryA())
}

@ExportedBridge("org_jetbrains_a_MyLibraryA_returnInt")
public fun org_jetbrains_a_MyLibraryA_returnInt(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as org.jetbrains.a.MyLibraryA
    val _result = __self.returnInt()
    return _result
}

@ExportedBridge("org_jetbrains_a_MyLibraryA_returnMe")
public fun org_jetbrains_a_MyLibraryA_returnMe(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as org.jetbrains.a.MyLibraryA
    val _result = __self.returnMe()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_jetbrains_a_topLevelFunction")
public fun org_jetbrains_a_topLevelFunction(): Int {
    val _result = org.jetbrains.a.topLevelFunction()
    return _result
}

@ExportedBridge("org_jetbrains_a_topLevelProperty_get")
public fun org_jetbrains_a_topLevelProperty_get(): Int {
    val _result = org.jetbrains.a.topLevelProperty
    return _result
}

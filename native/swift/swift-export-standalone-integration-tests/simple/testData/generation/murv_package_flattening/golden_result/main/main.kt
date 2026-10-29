@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(org.kotlin.foo.Clazz::class, "22ExportedKotlinPackages3orgO6kotlinO3fooO4mainE5ClazzC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import org.kotlin.foo.checkableExtension as org_kotlin_foo_checkableExtension
import org.kotlin.foo.discardableExtension as org_kotlin_foo_discardableExtension
import org.kotlin.foo.extensionProperty as org_kotlin_foo_extensionProperty

@ExportedBridge("org_kotlin_bar_checkableOutsideOfRoot")
public fun org_kotlin_bar_checkableOutsideOfRoot(): Int {
    val _result = org.kotlin.bar.checkableOutsideOfRoot()
    return _result
}

@ExportedBridge("org_kotlin_bar_discardableOutsideOfRoot")
public fun org_kotlin_bar_discardableOutsideOfRoot(): Int {
    val _result = org.kotlin.bar.discardableOutsideOfRoot()
    return _result
}

@ExportedBridge("org_kotlin_foo_Clazz_init_allocate")
public fun org_kotlin_foo_Clazz_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<org.kotlin.foo.Clazz>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("org_kotlin_foo_Clazz_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun org_kotlin_foo_Clazz_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, org.kotlin.foo.Clazz())
    return true
}

@ExportedBridge("org_kotlin_foo_checkable")
public fun org_kotlin_foo_checkable(): Int {
    val _result = org.kotlin.foo.checkable()
    return _result
}

@ExportedBridge("org_kotlin_foo_checkableExtension__TypesOfArgumentsE__Swift_Int32__")
public fun org_kotlin_foo_checkableExtension__TypesOfArgumentsE__Swift_Int32__(`receiver`: Int): Int {
    val __receiver = `receiver`
    val _result = __receiver.org_kotlin_foo_checkableExtension()
    return _result
}

@ExportedBridge("org_kotlin_foo_discardable")
public fun org_kotlin_foo_discardable(): Int {
    val _result = org.kotlin.foo.discardable()
    return _result
}

@ExportedBridge("org_kotlin_foo_discardableExtension__TypesOfArgumentsE__Swift_Int32__")
public fun org_kotlin_foo_discardableExtension__TypesOfArgumentsE__Swift_Int32__(`receiver`: Int): Int {
    val __receiver = `receiver`
    val _result = __receiver.org_kotlin_foo_discardableExtension()
    return _result
}

@ExportedBridge("org_kotlin_foo_explicitlyIgnorable")
public fun org_kotlin_foo_explicitlyIgnorable(): Int {
    val _result = org.kotlin.foo.explicitlyIgnorable()
    return _result
}

@ExportedBridge("org_kotlin_foo_extensionProperty_get__TypesOfArgumentsE__Swift_Int32__")
public fun org_kotlin_foo_extensionProperty_get__TypesOfArgumentsE__Swift_Int32__(`receiver`: Int): Int {
    val __receiver = `receiver`
    val _result = __receiver.org_kotlin_foo_extensionProperty
    return _result
}

@ExportedBridge("org_kotlin_foo_ignorableInMarkedFile")
public fun org_kotlin_foo_ignorableInMarkedFile(): Int {
    val _result = org.kotlin.foo.ignorableInMarkedFile()
    return _result
}

@ExportedBridge("org_kotlin_foo_nothingFunction")
public fun org_kotlin_foo_nothingFunction(): Boolean {
    val _result = org.kotlin.foo.nothingFunction()
    return _result
}

@ExportedBridge("org_kotlin_foo_unitFunction")
public fun org_kotlin_foo_unitFunction(): Boolean {
    org.kotlin.foo.unitFunction()
    return true
}

@ExportedBridge("org_kotlin_foo_variable_get")
public fun org_kotlin_foo_variable_get(): Int {
    val _result = org.kotlin.foo.variable
    return _result
}

@ExportedBridge("org_kotlin_foo_variable_set__TypesOfArguments__Swift_Int32__")
public fun org_kotlin_foo_variable_set__TypesOfArguments__Swift_Int32__(newValue: Int): Boolean {
    val __newValue = newValue
    org.kotlin.foo.variable = __newValue
    return true
}

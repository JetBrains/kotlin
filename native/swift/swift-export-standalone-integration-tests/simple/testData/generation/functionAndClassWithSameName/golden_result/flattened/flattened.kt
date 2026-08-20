@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(flattenedPackage.FlattenedPackageClass::class, "22ExportedKotlinPackages16flattenedPackageO9flattenedE21FlattenedPackageClassC")
@file:kotlin.native.internal.objc.BindClassToObjCName(test.factory.suffix.BasicFoo::class, "22ExportedKotlinPackages4testO7factoryO6suffixO9flattenedE8BasicFooC")
@file:kotlin.native.internal.objc.BindClassToObjCName(test.factory.suffix.Foo::class, "22ExportedKotlinPackages4testO7factoryO6suffixO9flattenedE3FooC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("flattenedPackage_FlattenedPackageClass__TypesOfArguments__Swift_Int32__")
public fun flattenedPackage_FlattenedPackageClass__TypesOfArguments__Swift_Int32__(i: Int): kotlin.native.internal.NativePtr {
    val __i = i
    val _result = run { flattenedPackage.FlattenedPackageClass(__i) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("flattenedPackage_FlattenedPackageClass_init_allocate")
public fun flattenedPackage_FlattenedPackageClass_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<flattenedPackage.FlattenedPackageClass>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("flattenedPackage_FlattenedPackageClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun flattenedPackage_FlattenedPackageClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, flattenedPackage.FlattenedPackageClass()) }
    return run { _result; true }
}

@ExportedBridge("test_factory_suffix_BasicFoo")
public fun test_factory_suffix_BasicFoo(): kotlin.native.internal.NativePtr {
    val _result = run { test.factory.suffix.BasicFoo() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

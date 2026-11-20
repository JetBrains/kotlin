@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(dependency.DependencyBase::class, "22ExportedKotlinPackages10dependencyO10dependencyE14DependencyBaseC")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

@ImportedBridge("dependency_DependencyBase_checked__reverse_swift")
internal external fun dependency_DependencyBase_checked__reverse_swift(self: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(dependency.DependencyBase::class, "checked")
public fun dependency_DependencyBase_checked__reverse(self: dependency.DependencyBase): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = dependency_DependencyBase_checked__reverse_swift(__self)
    return _result
}

@ImportedBridge("dependency_DependencyBase_ignorable__reverse_swift")
internal external fun dependency_DependencyBase_ignorable__reverse_swift(self: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(dependency.DependencyBase::class, "ignorable")
public fun dependency_DependencyBase_ignorable__reverse(self: dependency.DependencyBase): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = dependency_DependencyBase_ignorable__reverse_swift(__self)
    return _result
}

@ExportedBridge("dependency_DependencyBase_checked")
public fun dependency_DependencyBase_checked(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as dependency.DependencyBase
    val _result = __self.checked()
    return _result
}

@ExportedBridge("dependency_DependencyBase_checked_direct", nonVirtualTargetMethod = "checked")
public fun dependency_DependencyBase_checked_direct(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as dependency.DependencyBase
    val _result = __self.checked()
    return _result
}

@ExportedBridge("dependency_DependencyBase_ignorable")
public fun dependency_DependencyBase_ignorable(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as dependency.DependencyBase
    val _result = __self.ignorable()
    return _result
}

@ExportedBridge("dependency_DependencyBase_ignorable_direct", nonVirtualTargetMethod = "ignorable")
public fun dependency_DependencyBase_ignorable_direct(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as dependency.DependencyBase
    val _result = __self.ignorable()
    return _result
}

@ExportedBridge("dependency_DependencyBase_init_allocate")
public fun dependency_DependencyBase_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<dependency.DependencyBase>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("dependency_DependencyBase_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun dependency_DependencyBase_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, dependency.DependencyBase())
    return true
}

@ExportedBridge("dependency_dependencyFunction")
public fun dependency_dependencyFunction(): Int {
    val _result = dependency.dependencyFunction()
    return _result
}

@ExportedBridge("dependency_ignorableDependencyFunction")
public fun dependency_ignorableDependencyFunction(): Int {
    val _result = dependency.ignorableDependencyFunction()
    return _result
}

@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Container::class, "4main9ContainerC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ImplementsHiddenInterface::class, "4main25ImplementsHiddenInterfaceC")
@file:kotlin.native.internal.objc.BindClassToObjCName(InheritsAndImplements::class, "4main21InheritsAndImplementsC")
@file:kotlin.native.internal.objc.BindClassToObjCName(InheritsHiddenClass::class, "4main19InheritsHiddenClassC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("Container_untouched_member__TypesOfArguments__Swift_Int32__")
public fun Container_untouched_member__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, arg: Int): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Container
    val __arg = arg
    val _result = run { __self.untouched_member(__arg) }
    return _result
}

@ExportedBridge("__root___Container_init_allocate")
public fun __root___Container_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<Container>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Container_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___Container_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, Container()) }
    return run { _result; true }
}

@ExportedBridge("__root___ImplementsHiddenInterface_init_allocate")
public fun __root___ImplementsHiddenInterface_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<ImplementsHiddenInterface>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ImplementsHiddenInterface_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___ImplementsHiddenInterface_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, ImplementsHiddenInterface()) }
    return run { _result; true }
}

@ExportedBridge("__root___InheritsAndImplements_init_allocate")
public fun __root___InheritsAndImplements_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<InheritsAndImplements>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___InheritsAndImplements_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___InheritsAndImplements_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, InheritsAndImplements()) }
    return run { _result; true }
}

@ExportedBridge("__root___InheritsHiddenClass_init_allocate")
public fun __root___InheritsHiddenClass_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<InheritsHiddenClass>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___InheritsHiddenClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___InheritsHiddenClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, InheritsHiddenClass()) }
    return run { _result; true }
}

@ExportedBridge("__root___untouched_function__TypesOfArguments__Swift_Int32__")
public fun __root___untouched_function__TypesOfArguments__Swift_Int32__(arg: Int): Int {
    val __arg = arg
    val _result = run { untouched_function(__arg) }
    return _result
}

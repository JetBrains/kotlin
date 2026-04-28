@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(AbstractBase::class, "4main12AbstractBaseC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Base::class, "4main4BaseC")
@file:kotlin.native.internal.objc.BindClassToObjCName(GreeterBase::class, "4main11GreeterBaseC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Greeter::class, "_Greeter")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ImportedBridge("AbstractBase_abstractMethod__reverse_swift")
internal external fun AbstractBase_abstractMethod__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(AbstractBase::class, "abstractMethod")
public fun AbstractBase_abstractMethod__reverse(self: AbstractBase): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = AbstractBase_abstractMethod__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(__result)
}

@ImportedBridge("AbstractBase_concreteMethod__reverse_swift")
internal external fun AbstractBase_concreteMethod__reverse_swift(self: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(AbstractBase::class, "concreteMethod")
public fun AbstractBase_concreteMethod__reverse(self: AbstractBase): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = AbstractBase_concreteMethod__reverse_swift(__self)
    return __result
}

@ImportedBridge("Base_count__reverse_swift")
internal external fun Base_count__reverse_swift(self: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(Base::class, "count")
public fun Base_count__reverse(self: Base): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = Base_count__reverse_swift(__self)
    return __result
}

@ImportedBridge("Base_greet__TypesOfArguments__Swift_String____reverse_swift")
internal external fun Base_greet__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Base::class, "greet")
public fun Base_greet__TypesOfArguments__Swift_String____reverse(self: Base, name: kotlin.String): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __name = name.objcPtr()
    val __result = Base_greet__TypesOfArguments__Swift_String____reverse_swift(__self, __name)
    return interpretObjCPointer<kotlin.String>(__result)
}

@ImportedBridge("GreeterBase_greet__TypesOfArguments__Swift_String____reverse_swift")
internal external fun GreeterBase_greet__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(GreeterBase::class, "greet")
public fun GreeterBase_greet__TypesOfArguments__Swift_String____reverse(self: GreeterBase, name: kotlin.String): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __name = name.objcPtr()
    val __result = GreeterBase_greet__TypesOfArguments__Swift_String____reverse_swift(__self, __name)
    return interpretObjCPointer<kotlin.String>(__result)
}

@ImportedBridge("GreeterBase_salutation__reverse_swift")
internal external fun GreeterBase_salutation__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(GreeterBase::class, "salutation")
public fun GreeterBase_salutation__reverse(self: GreeterBase): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = GreeterBase_salutation__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(__result)
}

@ImportedBridge("Greeter_greet__TypesOfArguments__Swift_String____reverse_swift")
internal external fun Greeter_greet__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Greeter::class, "greet")
public fun Greeter_greet__TypesOfArguments__Swift_String____reverse(self: Greeter, name: kotlin.String): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __name = name.objcPtr()
    val __result = Greeter_greet__TypesOfArguments__Swift_String____reverse_swift(__self, __name)
    return interpretObjCPointer<kotlin.String>(__result)
}

@ImportedBridge("Greeter_salutation__reverse_swift")
internal external fun Greeter_salutation__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Greeter::class, "salutation")
public fun Greeter_salutation__reverse(self: Greeter): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = Greeter_salutation__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(__result)
}

@ExportedBridge("AbstractBase_abstractMethod")
public fun AbstractBase_abstractMethod(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AbstractBase
    val _result = run { __self.abstractMethod() }
    return _result.objcPtr()
}

@ExportedBridge("AbstractBase_concreteMethod")
public fun AbstractBase_concreteMethod(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as AbstractBase
    val _result = run { __self.concreteMethod() }
    return _result
}

@ExportedBridge("Base_count")
public fun Base_count(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Base
    val _result = run { __self.count() }
    return _result
}

@ExportedBridge("Base_greet__TypesOfArguments__Swift_String__")
public fun Base_greet__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Base
    val __name = interpretObjCPointer<kotlin.String>(name)
    val _result = run { __self.greet(__name) }
    return _result.objcPtr()
}

@ExportedBridge("Base_notOpen")
public fun Base_notOpen(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Base
    val _result = run { __self.notOpen() }
    return _result.objcPtr()
}

@ExportedBridge("GreeterBase_greet__TypesOfArguments__Swift_String__")
public fun GreeterBase_greet__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as GreeterBase
    val __name = interpretObjCPointer<kotlin.String>(name)
    val _result = run { __self.greet(__name) }
    return _result.objcPtr()
}

@ExportedBridge("GreeterBase_salutation")
public fun GreeterBase_salutation(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as GreeterBase
    val _result = run { __self.salutation() }
    return _result.objcPtr()
}

@ExportedBridge("Greeter_greet__TypesOfArguments__Swift_String__")
public fun Greeter_greet__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Greeter
    val __name = interpretObjCPointer<kotlin.String>(name)
    val _result = run { __self.greet(__name) }
    return _result.objcPtr()
}

@ExportedBridge("Greeter_salutation")
public fun Greeter_salutation(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Greeter
    val _result = run { __self.salutation() }
    return _result.objcPtr()
}

@ExportedBridge("__root___Base_init_allocate")
public fun __root___Base_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<Base>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Base_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___Base_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, Base()) }
    return run { _result; true }
}

@ExportedBridge("__root___GreeterBase_init_allocate")
public fun __root___GreeterBase_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<GreeterBase>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___GreeterBase_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___GreeterBase_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, GreeterBase()) }
    return run { _result; true }
}

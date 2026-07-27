@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(AbstractBase::class, "4main12AbstractBaseC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Base::class, "4main4BaseC")
@file:kotlin.native.internal.objc.BindClassToObjCName(GreeterBase::class, "4main11GreeterBaseC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Overloaded::class, "4main10OverloadedC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ThrowingMembers::class, "4main15ThrowingMembersC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Boxed::class, "_main_Boxed")
@file:kotlin.native.internal.objc.BindClassToObjCName(Defaulter::class, "_main_Defaulter")
@file:kotlin.native.internal.objc.BindClassToObjCName(Greeter::class, "_main_Greeter")
@file:kotlin.native.internal.objc.BindClassToObjCName(OverloadedInterface::class, "_main_OverloadedInterface")

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
    val _result = AbstractBase_abstractMethod__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("AbstractBase_concreteMethod__reverse_swift")
internal external fun AbstractBase_concreteMethod__reverse_swift(self: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(AbstractBase::class, "concreteMethod")
public fun AbstractBase_concreteMethod__reverse(self: AbstractBase): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = AbstractBase_concreteMethod__reverse_swift(__self)
    return _result
}

@ImportedBridge("Base_count__reverse_swift")
internal external fun Base_count__reverse_swift(self: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(Base::class, "count")
public fun Base_count__reverse(self: Base): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = Base_count__reverse_swift(__self)
    return _result
}

@ImportedBridge("Base_greet__TypesOfArguments__Swift_String____reverse_swift")
internal external fun Base_greet__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Base::class, "greet")
public fun Base_greet__TypesOfArguments__Swift_String____reverse(self: Base, name: kotlin.String): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __name = name.objcPtr()
    val _result = Base_greet__TypesOfArguments__Swift_String____reverse_swift(__self, __name)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("Base_name_get__reverse_swift")
internal external fun Base_name_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Base::class, "<get-name>")
public fun Base_name_get__reverse(self: Base): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = Base_name_get__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("Base_name_set__TypesOfArguments__Swift_String____reverse_swift")
internal external fun Base_name_set__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(Base::class, "<set-name>")
public fun Base_name_set__TypesOfArguments__Swift_String____reverse(self: Base, newValue: kotlin.String): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __newValue = newValue.objcPtr()
    val _result = Base_name_set__TypesOfArguments__Swift_String____reverse_swift(__self, __newValue)
    return run<Unit> { _result }
}

@ImportedBridge("Base_size_get__reverse_swift")
internal external fun Base_size_get__reverse_swift(self: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(Base::class, "<get-size>")
public fun Base_size_get__reverse(self: Base): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = Base_size_get__reverse_swift(__self)
    return _result
}

@ImportedBridge("Boxed_boxLabel_get__reverse_swift")
internal external fun Boxed_boxLabel_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Boxed::class, "<get-boxLabel>")
public fun Boxed_boxLabel_get__reverse(self: Boxed<kotlin.Any?>): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = Boxed_boxLabel_get__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("Boxed_label__reverse_swift")
internal external fun Boxed_label__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Boxed::class, "label")
public fun Boxed_label__reverse(self: Boxed<kotlin.Any?>): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = Boxed_label__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("Boxed_unbox__reverse_swift")
internal external fun Boxed_unbox__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Boxed::class, "unbox")
public fun Boxed_unbox__reverse(self: Boxed<kotlin.Any?>): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = Boxed_unbox__reverse_swift(__self)
    return if (_result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.Any
}

@ImportedBridge("Defaulter_describe__reverse_swift")
internal external fun Defaulter_describe__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Defaulter::class, "describe")
public fun Defaulter_describe__reverse(self: Defaulter): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = Defaulter_describe__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("Defaulter_kind_get__reverse_swift")
internal external fun Defaulter_kind_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Defaulter::class, "<get-kind>")
public fun Defaulter_kind_get__reverse(self: Defaulter): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = Defaulter_kind_get__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("Defaulter_tag__reverse_swift")
internal external fun Defaulter_tag__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Defaulter::class, "tag")
public fun Defaulter_tag__reverse(self: Defaulter): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = Defaulter_tag__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("GreeterBase_greet__TypesOfArguments__Swift_String____reverse_swift")
internal external fun GreeterBase_greet__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(GreeterBase::class, "greet")
public fun GreeterBase_greet__TypesOfArguments__Swift_String____reverse(self: GreeterBase, name: kotlin.String): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __name = name.objcPtr()
    val _result = GreeterBase_greet__TypesOfArguments__Swift_String____reverse_swift(__self, __name)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("GreeterBase_mood_get__reverse_swift")
internal external fun GreeterBase_mood_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(GreeterBase::class, "<get-mood>")
public fun GreeterBase_mood_get__reverse(self: GreeterBase): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = GreeterBase_mood_get__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("GreeterBase_mood_set__TypesOfArguments__Swift_String____reverse_swift")
internal external fun GreeterBase_mood_set__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(GreeterBase::class, "<set-mood>")
public fun GreeterBase_mood_set__TypesOfArguments__Swift_String____reverse(self: GreeterBase, newValue: kotlin.String): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __newValue = newValue.objcPtr()
    val _result = GreeterBase_mood_set__TypesOfArguments__Swift_String____reverse_swift(__self, __newValue)
    return run<Unit> { _result }
}

@ImportedBridge("GreeterBase_salutation__reverse_swift")
internal external fun GreeterBase_salutation__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(GreeterBase::class, "salutation")
public fun GreeterBase_salutation__reverse(self: GreeterBase): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = GreeterBase_salutation__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("Greeter_greet__TypesOfArguments__Swift_String____reverse_swift")
internal external fun Greeter_greet__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Greeter::class, "greet")
public fun Greeter_greet__TypesOfArguments__Swift_String____reverse(self: Greeter, name: kotlin.String): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __name = name.objcPtr()
    val _result = Greeter_greet__TypesOfArguments__Swift_String____reverse_swift(__self, __name)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("Greeter_mood_get__reverse_swift")
internal external fun Greeter_mood_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Greeter::class, "<get-mood>")
public fun Greeter_mood_get__reverse(self: Greeter): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = Greeter_mood_get__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("Greeter_mood_set__TypesOfArguments__Swift_String____reverse_swift")
internal external fun Greeter_mood_set__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(Greeter::class, "<set-mood>")
public fun Greeter_mood_set__TypesOfArguments__Swift_String____reverse(self: Greeter, newValue: kotlin.String): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __newValue = newValue.objcPtr()
    val _result = Greeter_mood_set__TypesOfArguments__Swift_String____reverse_swift(__self, __newValue)
    return run<Unit> { _result }
}

@ImportedBridge("Greeter_salutation__reverse_swift")
internal external fun Greeter_salutation__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Greeter::class, "salutation")
public fun Greeter_salutation__reverse(self: Greeter): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = Greeter_salutation__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("OverloadedInterface_say__TypesOfArguments__Swift_Int32____reverse_swift")
internal external fun OverloadedInterface_say__TypesOfArguments__Swift_Int32____reverse_swift(self: kotlin.native.internal.NativePtr, times: Int): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(OverloadedInterface::class, "say")
public fun OverloadedInterface_say__TypesOfArguments__Swift_Int32____reverse(self: OverloadedInterface, times: Int): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = OverloadedInterface_say__TypesOfArguments__Swift_Int32____reverse_swift(__self, times)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("OverloadedInterface_say__reverse_swift")
internal external fun OverloadedInterface_say__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(OverloadedInterface::class, "say")
public fun OverloadedInterface_say__reverse(self: OverloadedInterface): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = OverloadedInterface_say__reverse_swift(__self)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("Overloaded_nullable__TypesOfArguments__Swift_Optional_Swift_String_____reverse_swift")
internal external fun Overloaded_nullable__TypesOfArguments__Swift_Optional_Swift_String_____reverse_swift(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Overloaded::class, "nullable")
public fun Overloaded_nullable__TypesOfArguments__Swift_Optional_Swift_String_____reverse(self: Overloaded, arg: kotlin.String?): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __arg = if (arg == null) kotlin.native.internal.NativePtr.NULL else arg.objcPtr()
    val _result = Overloaded_nullable__TypesOfArguments__Swift_Optional_Swift_String_____reverse_swift(__self, __arg)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("Overloaded_nullable__TypesOfArguments__Swift_String____reverse_swift")
internal external fun Overloaded_nullable__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Overloaded::class, "nullable")
public fun Overloaded_nullable__TypesOfArguments__Swift_String____reverse(self: Overloaded, arg: kotlin.String): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __arg = arg.objcPtr()
    val _result = Overloaded_nullable__TypesOfArguments__Swift_String____reverse_swift(__self, __arg)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("Overloaded_pick__TypesOfArguments__Swift_String_Swift_Int32____reverse_swift")
internal external fun Overloaded_pick__TypesOfArguments__Swift_String_Swift_Int32____reverse_swift(self: kotlin.native.internal.NativePtr, arg1: kotlin.native.internal.NativePtr, arg2: Int): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Overloaded::class, "pick")
public fun Overloaded_pick__TypesOfArguments__Swift_String_Swift_Int32____reverse(self: Overloaded, arg1: kotlin.String, arg2: Int): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __arg1 = arg1.objcPtr()
    val _result = Overloaded_pick__TypesOfArguments__Swift_String_Swift_Int32____reverse_swift(__self, __arg1, arg2)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("Overloaded_pick__TypesOfArguments__Swift_String____reverse_swift")
internal external fun Overloaded_pick__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, arg1: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Overloaded::class, "pick")
public fun Overloaded_pick__TypesOfArguments__Swift_String____reverse(self: Overloaded, arg1: kotlin.String): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __arg1 = arg1.objcPtr()
    val _result = Overloaded_pick__TypesOfArguments__Swift_String____reverse_swift(__self, __arg1)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("Overloaded_same__TypesOfArguments__Swift_Int32____reverse_swift")
internal external fun Overloaded_same__TypesOfArguments__Swift_Int32____reverse_swift(self: kotlin.native.internal.NativePtr, arg: Int): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Overloaded::class, "same")
public fun Overloaded_same__TypesOfArguments__Swift_Int32____reverse(self: Overloaded, arg: Int): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = Overloaded_same__TypesOfArguments__Swift_Int32____reverse_swift(__self, arg)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("Overloaded_same__TypesOfArguments__Swift_String____reverse_swift")
internal external fun Overloaded_same__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(Overloaded::class, "same")
public fun Overloaded_same__TypesOfArguments__Swift_String____reverse(self: Overloaded, arg: kotlin.String): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __arg = arg.objcPtr()
    val _result = Overloaded_same__TypesOfArguments__Swift_String____reverse_swift(__self, __arg)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("ThrowingMembers_compute__TypesOfArguments__Swift_Int32____reverse_swift")
internal external fun ThrowingMembers_compute__TypesOfArguments__Swift_Int32____reverse_swift(self: kotlin.native.internal.NativePtr, x: Int, _out_error: kotlinx.cinterop.CPointer<kotlinx.cinterop.COpaquePointerVar>): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(ThrowingMembers::class, "compute")
public fun ThrowingMembers_compute__TypesOfArguments__Swift_Int32____reverse(self: ThrowingMembers, x: Int): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    return kotlinx.cinterop.memScoped {
        val _out_error = alloc<kotlinx.cinterop.COpaquePointerVar>()
        val _result = ThrowingMembers_compute__TypesOfArguments__Swift_Int32____reverse_swift(__self, x, _out_error.ptr)
        throwErrorFromReverseBridge(_out_error.value)
        interpretObjCPointer<kotlin.String>(_result)
    }
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

@ExportedBridge("AbstractBase_concreteMethod_direct", nonVirtualTargetMethod = "concreteMethod")
public fun AbstractBase_concreteMethod_direct(self: kotlin.native.internal.NativePtr): Int {
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

@ExportedBridge("Base_count_direct", nonVirtualTargetMethod = "count")
public fun Base_count_direct(self: kotlin.native.internal.NativePtr): Int {
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

@ExportedBridge("Base_greet__TypesOfArguments__Swift_String___direct", nonVirtualTargetMethod = "greet")
public fun Base_greet__TypesOfArguments__Swift_String___direct(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Base
    val __name = interpretObjCPointer<kotlin.String>(name)
    val _result = run { __self.greet(__name) }
    return _result.objcPtr()
}

@ExportedBridge("Base_name_get")
public fun Base_name_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Base
    val _result = run { __self.name }
    return _result.objcPtr()
}

@ExportedBridge("Base_name_get_direct", nonVirtualTargetMethod = "<get-name>")
public fun Base_name_get_direct(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Base
    val _result = run { __self.name }
    return _result.objcPtr()
}

@ExportedBridge("Base_name_set__TypesOfArguments__Swift_String__")
public fun Base_name_set__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Base
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    val _result = run { __self.name = __newValue }
    return run { _result; true }
}

@ExportedBridge("Base_name_set__TypesOfArguments__Swift_String___direct", nonVirtualTargetMethod = "<set-name>")
public fun Base_name_set__TypesOfArguments__Swift_String___direct(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Base
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    val _result = run { __self.name = __newValue }
    return run { _result; true }
}

@ExportedBridge("Base_notOpen")
public fun Base_notOpen(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Base
    val _result = run { __self.notOpen() }
    return _result.objcPtr()
}

@ExportedBridge("Base_notOpenValue_get")
public fun Base_notOpenValue_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Base
    val _result = run { __self.notOpenValue }
    return _result.objcPtr()
}

@ExportedBridge("Base_size_get")
public fun Base_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Base
    val _result = run { __self.size }
    return _result
}

@ExportedBridge("Base_size_get_direct", nonVirtualTargetMethod = "<get-size>")
public fun Base_size_get_direct(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Base
    val _result = run { __self.size }
    return _result
}

@ExportedBridge("Boxed_boxLabel_get")
public fun Boxed_boxLabel_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Boxed<kotlin.Any?>
    val _result = run { __self.boxLabel }
    return _result.objcPtr()
}

@ExportedBridge("Boxed_boxLabel_get_direct", nonVirtualTargetMethod = "<get-boxLabel>")
public fun Boxed_boxLabel_get_direct(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Boxed<kotlin.Any?>
    val _result = run { __self.boxLabel }
    return _result.objcPtr()
}

@ExportedBridge("Boxed_label")
public fun Boxed_label(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Boxed<kotlin.Any?>
    val _result = run { __self.label() }
    return _result.objcPtr()
}

@ExportedBridge("Boxed_label_direct", nonVirtualTargetMethod = "label")
public fun Boxed_label_direct(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Boxed<kotlin.Any?>
    val _result = run { __self.label() }
    return _result.objcPtr()
}

@ExportedBridge("Boxed_unbox")
public fun Boxed_unbox(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Boxed<kotlin.Any?>
    val _result = run { __self.unbox() }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Defaulter_describe")
public fun Defaulter_describe(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Defaulter
    val _result = run { __self.describe() }
    return _result.objcPtr()
}

@ExportedBridge("Defaulter_describe_direct", nonVirtualTargetMethod = "describe")
public fun Defaulter_describe_direct(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Defaulter
    val _result = run { __self.describe() }
    return _result.objcPtr()
}

@ExportedBridge("Defaulter_kind_get")
public fun Defaulter_kind_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Defaulter
    val _result = run { __self.kind }
    return _result.objcPtr()
}

@ExportedBridge("Defaulter_kind_get_direct", nonVirtualTargetMethod = "<get-kind>")
public fun Defaulter_kind_get_direct(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Defaulter
    val _result = run { __self.kind }
    return _result.objcPtr()
}

@ExportedBridge("Defaulter_tag")
public fun Defaulter_tag(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Defaulter
    val _result = run { __self.tag() }
    return _result.objcPtr()
}

@ExportedBridge("GreeterBase_greet__TypesOfArguments__Swift_String__")
public fun GreeterBase_greet__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as GreeterBase
    val __name = interpretObjCPointer<kotlin.String>(name)
    val _result = run { __self.greet(__name) }
    return _result.objcPtr()
}

@ExportedBridge("GreeterBase_greet__TypesOfArguments__Swift_String___direct", nonVirtualTargetMethod = "greet")
public fun GreeterBase_greet__TypesOfArguments__Swift_String___direct(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as GreeterBase
    val __name = interpretObjCPointer<kotlin.String>(name)
    val _result = run { __self.greet(__name) }
    return _result.objcPtr()
}

@ExportedBridge("GreeterBase_mood_get")
public fun GreeterBase_mood_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as GreeterBase
    val _result = run { __self.mood }
    return _result.objcPtr()
}

@ExportedBridge("GreeterBase_mood_get_direct", nonVirtualTargetMethod = "<get-mood>")
public fun GreeterBase_mood_get_direct(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as GreeterBase
    val _result = run { __self.mood }
    return _result.objcPtr()
}

@ExportedBridge("GreeterBase_mood_set__TypesOfArguments__Swift_String__")
public fun GreeterBase_mood_set__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as GreeterBase
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    val _result = run { __self.mood = __newValue }
    return run { _result; true }
}

@ExportedBridge("GreeterBase_mood_set__TypesOfArguments__Swift_String___direct", nonVirtualTargetMethod = "<set-mood>")
public fun GreeterBase_mood_set__TypesOfArguments__Swift_String___direct(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as GreeterBase
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    val _result = run { __self.mood = __newValue }
    return run { _result; true }
}

@ExportedBridge("GreeterBase_salutation")
public fun GreeterBase_salutation(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as GreeterBase
    val _result = run { __self.salutation() }
    return _result.objcPtr()
}

@ExportedBridge("GreeterBase_salutation_direct", nonVirtualTargetMethod = "salutation")
public fun GreeterBase_salutation_direct(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
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

@ExportedBridge("Greeter_mood_get")
public fun Greeter_mood_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Greeter
    val _result = run { __self.mood }
    return _result.objcPtr()
}

@ExportedBridge("Greeter_mood_set__TypesOfArguments__Swift_String__")
public fun Greeter_mood_set__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Greeter
    val __newValue = interpretObjCPointer<kotlin.String>(newValue)
    val _result = run { __self.mood = __newValue }
    return run { _result; true }
}

@ExportedBridge("Greeter_salutation")
public fun Greeter_salutation(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Greeter
    val _result = run { __self.salutation() }
    return _result.objcPtr()
}

@ExportedBridge("OverloadedInterface_say")
public fun OverloadedInterface_say(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OverloadedInterface
    val _result = run { __self.say() }
    return _result.objcPtr()
}

@ExportedBridge("OverloadedInterface_say__TypesOfArguments__Swift_Int32__")
public fun OverloadedInterface_say__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, times: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as OverloadedInterface
    val __times = times
    val _result = run { __self.say(__times) }
    return _result.objcPtr()
}

@ExportedBridge("Overloaded_nullable__TypesOfArguments__Swift_String__")
public fun Overloaded_nullable__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Overloaded
    val __arg = interpretObjCPointer<kotlin.String>(arg)
    val _result = run { __self.nullable(__arg) }
    return _result.objcPtr()
}

@ExportedBridge("Overloaded_nullable__TypesOfArguments__Swift_Optional_Swift_String___")
public fun Overloaded_nullable__TypesOfArguments__Swift_Optional_Swift_String___(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Overloaded
    val __arg = if (arg == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<kotlin.String>(arg)
    val _result = run { __self.nullable(__arg) }
    return _result.objcPtr()
}

@ExportedBridge("Overloaded_nullable__TypesOfArguments__Swift_String___direct", nonVirtualTargetMethod = "nullable")
public fun Overloaded_nullable__TypesOfArguments__Swift_String___direct(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Overloaded
    val __arg = interpretObjCPointer<kotlin.String>(arg)
    val _result = run { __self.nullable(__arg) }
    return _result.objcPtr()
}

@ExportedBridge("Overloaded_nullable__TypesOfArguments__Swift_Optional_Swift_String____direct", nonVirtualTargetMethod = "nullable")
public fun Overloaded_nullable__TypesOfArguments__Swift_Optional_Swift_String____direct(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Overloaded
    val __arg = if (arg == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<kotlin.String>(arg)
    val _result = run { __self.nullable(__arg) }
    return _result.objcPtr()
}

@ExportedBridge("Overloaded_pick")
public fun Overloaded_pick(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Overloaded
    val _result = run { __self.pick() }
    return _result.objcPtr()
}

@ExportedBridge("Overloaded_pick__TypesOfArguments__Swift_String__")
public fun Overloaded_pick__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, arg1: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Overloaded
    val __arg1 = interpretObjCPointer<kotlin.String>(arg1)
    val _result = run { __self.pick(__arg1) }
    return _result.objcPtr()
}

@ExportedBridge("Overloaded_pick__TypesOfArguments__Swift_String_Swift_Int32__")
public fun Overloaded_pick__TypesOfArguments__Swift_String_Swift_Int32__(self: kotlin.native.internal.NativePtr, arg1: kotlin.native.internal.NativePtr, arg2: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Overloaded
    val __arg1 = interpretObjCPointer<kotlin.String>(arg1)
    val __arg2 = arg2
    val _result = run { __self.pick(__arg1, __arg2) }
    return _result.objcPtr()
}

@ExportedBridge("Overloaded_pick__TypesOfArguments__Swift_String___direct", nonVirtualTargetMethod = "pick")
public fun Overloaded_pick__TypesOfArguments__Swift_String___direct(self: kotlin.native.internal.NativePtr, arg1: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Overloaded
    val __arg1 = interpretObjCPointer<kotlin.String>(arg1)
    val _result = run { __self.pick(__arg1) }
    return _result.objcPtr()
}

@ExportedBridge("Overloaded_pick__TypesOfArguments__Swift_String_Swift_Int32___direct", nonVirtualTargetMethod = "pick")
public fun Overloaded_pick__TypesOfArguments__Swift_String_Swift_Int32___direct(self: kotlin.native.internal.NativePtr, arg1: kotlin.native.internal.NativePtr, arg2: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Overloaded
    val __arg1 = interpretObjCPointer<kotlin.String>(arg1)
    val __arg2 = arg2
    val _result = run { __self.pick(__arg1, __arg2) }
    return _result.objcPtr()
}

@ExportedBridge("Overloaded_same__TypesOfArguments__Swift_String__")
public fun Overloaded_same__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Overloaded
    val __arg = interpretObjCPointer<kotlin.String>(arg)
    val _result = run { __self.same(__arg) }
    return _result.objcPtr()
}

@ExportedBridge("Overloaded_same__TypesOfArguments__Swift_Int32__")
public fun Overloaded_same__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, arg: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Overloaded
    val __arg = arg
    val _result = run { __self.same(__arg) }
    return _result.objcPtr()
}

@ExportedBridge("Overloaded_same__TypesOfArguments__Swift_String___direct", nonVirtualTargetMethod = "same")
public fun Overloaded_same__TypesOfArguments__Swift_String___direct(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Overloaded
    val __arg = interpretObjCPointer<kotlin.String>(arg)
    val _result = run { __self.same(__arg) }
    return _result.objcPtr()
}

@ExportedBridge("Overloaded_same__TypesOfArguments__Swift_Int32___direct", nonVirtualTargetMethod = "same")
public fun Overloaded_same__TypesOfArguments__Swift_Int32___direct(self: kotlin.native.internal.NativePtr, arg: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Overloaded
    val __arg = arg
    val _result = run { __self.same(__arg) }
    return _result.objcPtr()
}

@ExportedBridge("ThrowingMembers_compute__TypesOfArguments__Swift_Int32__")
public fun ThrowingMembers_compute__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, x: Int, _out_error: kotlinx.cinterop.COpaquePointerVar): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as ThrowingMembers
    val __x = x
    val ___out_error = _out_error
    try {
        val _result = run { __self.compute(__x) }
        return _result.objcPtr()
    } catch (error: Throwable) {
        ___out_error.value = StableRef.create(error).asCPointer()
        return kotlin.native.internal.NativePtr.NULL
    }
}

@ExportedBridge("ThrowingMembers_compute__TypesOfArguments__Swift_Int32___direct", nonVirtualTargetMethod = "compute")
public fun ThrowingMembers_compute__TypesOfArguments__Swift_Int32___direct(self: kotlin.native.internal.NativePtr, x: Int, _out_error: kotlinx.cinterop.COpaquePointerVar): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as ThrowingMembers
    val __x = x
    val ___out_error = _out_error
    try {
        val _result = run { __self.compute(__x) }
        return _result.objcPtr()
    } catch (error: Throwable) {
        ___out_error.value = StableRef.create(error).asCPointer()
        return kotlin.native.internal.NativePtr.NULL
    }
}

@ExportedBridge("__root___AbstractBase_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__", nonVirtualTargetMethod = "<init>")
public fun __root___AbstractBase_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, AbstractBase()) }
    return run { _result; true }
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

@ExportedBridge("__root___Overloaded_init_allocate")
public fun __root___Overloaded_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<Overloaded>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Overloaded_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___Overloaded_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, Overloaded()) }
    return run { _result; true }
}

@ExportedBridge("__root___ThrowingMembers_init_allocate")
public fun __root___ThrowingMembers_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<ThrowingMembers>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ThrowingMembers_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___ThrowingMembers_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, ThrowingMembers()) }
    return run { _result; true }
}

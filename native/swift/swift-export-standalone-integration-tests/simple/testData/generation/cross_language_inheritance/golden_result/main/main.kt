@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(AbstractBase::class, "4main12AbstractBaseC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Base::class, "4main4BaseC")
@file:kotlin.native.internal.objc.BindClassToObjCName(GreeterBase::class, "4main11GreeterBaseC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Boxed::class, "_Boxed")
@file:kotlin.native.internal.objc.BindClassToObjCName(Defaulter::class, "_Defaulter")
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

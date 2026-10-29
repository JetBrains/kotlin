@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(ClassInMarkedFile::class, "4main17ClassInMarkedFileC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Greeter::class, "4main7GreeterC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Greeter.Companion::class, "4main7GreeterC9CompanionC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Greeter.Nested::class, "4main7GreeterC6NestedC")
@file:kotlin.native.internal.objc.BindClassToObjCName(LoudGreeter::class, "4main11LoudGreeterC")
@file:kotlin.native.internal.objc.BindClassToObjCName(LouderGreeter::class, "4main13LouderGreeterC")
@file:kotlin.native.internal.objc.BindClassToObjCName(MarkedObject::class, "4main12MarkedObjectC")
@file:kotlin.native.internal.objc.BindClassToObjCName(MarkedOperators::class, "4main15MarkedOperatorsC")
@file:kotlin.native.internal.objc.BindClassToObjCName(MarkedOverridesUnmarked::class, "4main23MarkedOverridesUnmarkedC")
@file:kotlin.native.internal.objc.BindClassToObjCName(SilentGreeter::class, "4main13SilentGreeterC")
@file:kotlin.native.internal.objc.BindClassToObjCName(UnmarkedClass::class, "4main13UnmarkedClassC")
@file:kotlin.native.internal.objc.BindClassToObjCName(UnmarkedOperators::class, "4main17UnmarkedOperatorsC")
@file:kotlin.native.internal.objc.BindClassToObjCName(MarkedGreeter::class, "_main_MarkedGreeter")
@file:kotlin.native.internal.objc.BindClassToObjCName(UnmarkedBase::class, "_main_UnmarkedBase")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

@ImportedBridge("LoudGreeter_greetIgnorable__TypesOfArguments__Swift_String____reverse_swift")
internal external fun LoudGreeter_greetIgnorable__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(LoudGreeter::class, "greetIgnorable")
public fun LoudGreeter_greetIgnorable__TypesOfArguments__Swift_String____reverse(self: LoudGreeter, name: kotlin.String): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __name = name.objcPtr()
    val _result = LoudGreeter_greetIgnorable__TypesOfArguments__Swift_String____reverse_swift(__self, __name)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("LoudGreeter_greet__TypesOfArguments__Swift_String____reverse_swift")
internal external fun LoudGreeter_greet__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(LoudGreeter::class, "greet")
public fun LoudGreeter_greet__TypesOfArguments__Swift_String____reverse(self: LoudGreeter, name: kotlin.String): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __name = name.objcPtr()
    val _result = LoudGreeter_greet__TypesOfArguments__Swift_String____reverse_swift(__self, __name)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("MarkedGreeter_greetIgnorable__TypesOfArguments__Swift_String____reverse_swift")
internal external fun MarkedGreeter_greetIgnorable__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(MarkedGreeter::class, "greetIgnorable")
public fun MarkedGreeter_greetIgnorable__TypesOfArguments__Swift_String____reverse(self: MarkedGreeter, name: kotlin.String): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __name = name.objcPtr()
    val _result = MarkedGreeter_greetIgnorable__TypesOfArguments__Swift_String____reverse_swift(__self, __name)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("MarkedGreeter_greet__TypesOfArguments__Swift_String____reverse_swift")
internal external fun MarkedGreeter_greet__TypesOfArguments__Swift_String____reverse_swift(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(MarkedGreeter::class, "greet")
public fun MarkedGreeter_greet__TypesOfArguments__Swift_String____reverse(self: MarkedGreeter, name: kotlin.String): kotlin.String {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __name = name.objcPtr()
    val _result = MarkedGreeter_greet__TypesOfArguments__Swift_String____reverse_swift(__self, __name)
    return interpretObjCPointer<kotlin.String>(_result)
}

@ImportedBridge("UnmarkedBase_unmarked__reverse_swift")
internal external fun UnmarkedBase_unmarked__reverse_swift(self: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(UnmarkedBase::class, "unmarked")
public fun UnmarkedBase_unmarked__reverse(self: UnmarkedBase): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = UnmarkedBase_unmarked__reverse_swift(__self)
    return _result
}

@ExportedBridge("ClassInMarkedFile_member")
public fun ClassInMarkedFile_member(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as ClassInMarkedFile
    val _result = __self.member()
    return _result
}

@ExportedBridge("Greeter_Companion_companionMember")
public fun Greeter_Companion_companionMember(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Greeter.Companion
    val _result = __self.companionMember()
    return _result
}

@ExportedBridge("Greeter_Companion_get")
public fun Greeter_Companion_get(): kotlin.native.internal.NativePtr {
    val _result = Greeter.Companion
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Greeter_Nested_init_allocate")
public fun Greeter_Nested_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Greeter.Nested>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Greeter_Nested_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun Greeter_Nested_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, Greeter.Nested())
    return true
}

@ExportedBridge("Greeter_Nested_nested")
public fun Greeter_Nested_nested(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Greeter.Nested
    val _result = __self.nested()
    return _result
}

@ExportedBridge("Greeter_greet__TypesOfArguments__Swift_String__")
public fun Greeter_greet__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Greeter
    val __name = interpretObjCPointer<kotlin.String>(name)
    val _result = __self.greet(__name)
    return _result.objcPtr()
}

@ExportedBridge("Greeter_greetIgnorable__TypesOfArguments__Swift_String__")
public fun Greeter_greetIgnorable__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Greeter
    val __name = interpretObjCPointer<kotlin.String>(name)
    val _result = __self.greetIgnorable(__name)
    return _result.objcPtr()
}

@ExportedBridge("Greeter_greetUnit__TypesOfArguments__Swift_String__")
public fun Greeter_greetUnit__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Greeter
    val __name = interpretObjCPointer<kotlin.String>(name)
    __self.greetUnit(__name)
    return true
}

@ExportedBridge("Greeter_property_get")
public fun Greeter_property_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Greeter
    val _result = __self.`property`
    return _result.objcPtr()
}

@ExportedBridge("LoudGreeter_greet__TypesOfArguments__Swift_String__")
public fun LoudGreeter_greet__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as LoudGreeter
    val __name = interpretObjCPointer<kotlin.String>(name)
    val _result = __self.greet(__name)
    return _result.objcPtr()
}

@ExportedBridge("LoudGreeter_greetIgnorable__TypesOfArguments__Swift_String__")
public fun LoudGreeter_greetIgnorable__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as LoudGreeter
    val __name = interpretObjCPointer<kotlin.String>(name)
    val _result = __self.greetIgnorable(__name)
    return _result.objcPtr()
}

@ExportedBridge("LoudGreeter_greetIgnorable__TypesOfArguments__Swift_String___direct", nonVirtualTargetMethod = "greetIgnorable")
public fun LoudGreeter_greetIgnorable__TypesOfArguments__Swift_String___direct(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as LoudGreeter
    val __name = interpretObjCPointer<kotlin.String>(name)
    val _result = __self.greetIgnorable(__name)
    return _result.objcPtr()
}

@ExportedBridge("LoudGreeter_greet__TypesOfArguments__Swift_String___direct", nonVirtualTargetMethod = "greet")
public fun LoudGreeter_greet__TypesOfArguments__Swift_String___direct(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as LoudGreeter
    val __name = interpretObjCPointer<kotlin.String>(name)
    val _result = __self.greet(__name)
    return _result.objcPtr()
}

@ExportedBridge("LouderGreeter_greet__TypesOfArguments__Swift_String__")
public fun LouderGreeter_greet__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as LouderGreeter
    val __name = interpretObjCPointer<kotlin.String>(name)
    val _result = __self.greet(__name)
    return _result.objcPtr()
}

@ExportedBridge("MarkedGreeter_greet__TypesOfArguments__Swift_String__")
public fun MarkedGreeter_greet__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MarkedGreeter
    val __name = interpretObjCPointer<kotlin.String>(name)
    val _result = __self.greet(__name)
    return _result.objcPtr()
}

@ExportedBridge("MarkedGreeter_greetIgnorable__TypesOfArguments__Swift_String__")
public fun MarkedGreeter_greetIgnorable__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MarkedGreeter
    val __name = interpretObjCPointer<kotlin.String>(name)
    val _result = __self.greetIgnorable(__name)
    return _result.objcPtr()
}

@ExportedBridge("MarkedObject_member")
public fun MarkedObject_member(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MarkedObject
    val _result = __self.member()
    return _result
}

@ExportedBridge("MarkedOperators_compareTo__TypesOfArguments__main_MarkedOperators__")
public fun MarkedOperators_compareTo__TypesOfArguments__main_MarkedOperators__(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MarkedOperators
    val __other = kotlin.native.internal.ref.dereferenceExternalRCRef(other) as MarkedOperators
    val _result = __self.compareTo(__other)
    return _result
}

@ExportedBridge("MarkedOperators_get__TypesOfArguments__Swift_Int32__")
public fun MarkedOperators_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MarkedOperators
    val __index = index
    val _result = __self.`get`(__index)
    return _result
}

@ExportedBridge("MarkedOperators_invoke")
public fun MarkedOperators_invoke(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MarkedOperators
    val _result = __self.invoke()
    return _result
}

@ExportedBridge("MarkedOperators_plus__TypesOfArguments__main_MarkedOperators__")
public fun MarkedOperators_plus__TypesOfArguments__main_MarkedOperators__(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MarkedOperators
    val __other = kotlin.native.internal.ref.dereferenceExternalRCRef(other) as MarkedOperators
    val _result = __self.plus(__other)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("MarkedOperators_value_get")
public fun MarkedOperators_value_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MarkedOperators
    val _result = __self.value
    return _result
}

@ExportedBridge("MarkedOverridesUnmarked_unmarked")
public fun MarkedOverridesUnmarked_unmarked(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MarkedOverridesUnmarked
    val _result = __self.unmarked()
    return _result
}

@ExportedBridge("SilentGreeter_greet__TypesOfArguments__Swift_String__")
public fun SilentGreeter_greet__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as SilentGreeter
    val __name = interpretObjCPointer<kotlin.String>(name)
    val _result = __self.greet(__name)
    return _result.objcPtr()
}

@ExportedBridge("SilentGreeter_greetIgnorable__TypesOfArguments__Swift_String__")
public fun SilentGreeter_greetIgnorable__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, name: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as SilentGreeter
    val __name = interpretObjCPointer<kotlin.String>(name)
    val _result = __self.greetIgnorable(__name)
    return _result.objcPtr()
}

@ExportedBridge("UnmarkedBase_unmarked")
public fun UnmarkedBase_unmarked(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as UnmarkedBase
    val _result = __self.unmarked()
    return _result
}

@ExportedBridge("UnmarkedClass_member")
public fun UnmarkedClass_member(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as UnmarkedClass
    val _result = __self.member()
    return _result
}

@ExportedBridge("UnmarkedOperators_compareTo__TypesOfArguments__main_UnmarkedOperators__")
public fun UnmarkedOperators_compareTo__TypesOfArguments__main_UnmarkedOperators__(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as UnmarkedOperators
    val __other = kotlin.native.internal.ref.dereferenceExternalRCRef(other) as UnmarkedOperators
    val _result = __self.compareTo(__other)
    return _result
}

@ExportedBridge("UnmarkedOperators_get__TypesOfArguments__Swift_Int32__")
public fun UnmarkedOperators_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as UnmarkedOperators
    val __index = index
    val _result = __self.`get`(__index)
    return _result
}

@ExportedBridge("UnmarkedOperators_invoke")
public fun UnmarkedOperators_invoke(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as UnmarkedOperators
    val _result = __self.invoke()
    return _result
}

@ExportedBridge("UnmarkedOperators_plus__TypesOfArguments__main_UnmarkedOperators__")
public fun UnmarkedOperators_plus__TypesOfArguments__main_UnmarkedOperators__(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as UnmarkedOperators
    val __other = kotlin.native.internal.ref.dereferenceExternalRCRef(other) as UnmarkedOperators
    val _result = __self.plus(__other)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("UnmarkedOperators_value_get")
public fun UnmarkedOperators_value_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as UnmarkedOperators
    val _result = __self.value
    return _result
}

@ExportedBridge("__root___ClassInMarkedFile_init_allocate")
public fun __root___ClassInMarkedFile_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<ClassInMarkedFile>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ClassInMarkedFile_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___ClassInMarkedFile_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, ClassInMarkedFile())
    return true
}

@ExportedBridge("__root___Greeter_init_allocate")
public fun __root___Greeter_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Greeter>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Greeter_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___Greeter_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, Greeter())
    return true
}

@ExportedBridge("__root___LoudGreeter_init_allocate")
public fun __root___LoudGreeter_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<LoudGreeter>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___LoudGreeter_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___LoudGreeter_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, LoudGreeter())
    return true
}

@ExportedBridge("__root___LouderGreeter_init_allocate")
public fun __root___LouderGreeter_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<LouderGreeter>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___LouderGreeter_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___LouderGreeter_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, LouderGreeter())
    return true
}

@ExportedBridge("__root___MarkedObject_get")
public fun __root___MarkedObject_get(): kotlin.native.internal.NativePtr {
    val _result = MarkedObject
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___MarkedOperators_init_allocate")
public fun __root___MarkedOperators_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<MarkedOperators>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___MarkedOperators_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__")
public fun __root___MarkedOperators_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, value: Int): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __value = value
    kotlin.native.internal.initInstance(____kt, MarkedOperators(__value))
    return true
}

@ExportedBridge("__root___MarkedOverridesUnmarked_init_allocate")
public fun __root___MarkedOverridesUnmarked_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<MarkedOverridesUnmarked>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___MarkedOverridesUnmarked_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___MarkedOverridesUnmarked_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, MarkedOverridesUnmarked())
    return true
}

@ExportedBridge("__root___SilentGreeter_get")
public fun __root___SilentGreeter_get(): kotlin.native.internal.NativePtr {
    val _result = SilentGreeter
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___UnmarkedClass_init_allocate")
public fun __root___UnmarkedClass_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<UnmarkedClass>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___UnmarkedClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___UnmarkedClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, UnmarkedClass())
    return true
}

@ExportedBridge("__root___UnmarkedOperators_init_allocate")
public fun __root___UnmarkedOperators_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<UnmarkedOperators>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___UnmarkedOperators_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__")
public fun __root___UnmarkedOperators_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, value: Int): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __value = value
    kotlin.native.internal.initInstance(____kt, UnmarkedOperators(__value))
    return true
}

@ExportedBridge("__root___addAndIgnoreResult__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_collections_MutableList_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun __root___addAndIgnoreResult__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_collections_MutableList_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(`receiver`: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __receiver = kotlin.native.internal.ref.dereferenceExternalRCRef(`receiver`) as kotlin.collections.MutableList<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = __receiver.addAndIgnoreResult<kotlin.Any?>(__element)
    return _result
}

@ExportedBridge("__root___ignorableInMarkedFile")
public fun __root___ignorableInMarkedFile(): Int {
    val _result = ignorableInMarkedFile()
    return _result
}

@ExportedBridge("__root___markedByFile")
public fun __root___markedByFile(): Int {
    val _result = markedByFile()
    return _result
}

@ExportedBridge("__root___markedExtension__TypesOfArgumentsE__Swift_Int32__")
public fun __root___markedExtension__TypesOfArgumentsE__Swift_Int32__(`receiver`: Int): Int {
    val __receiver = `receiver`
    val _result = __receiver.markedExtension()
    return _result
}

@ExportedBridge("__root___markedNothing")
public fun __root___markedNothing(): Boolean {
    val _result = markedNothing()
    return _result
}

@ExportedBridge("__root___markedNullableNothing")
public fun __root___markedNullableNothing(): Boolean {
    val _result = markedNullableNothing()
    return run { _result; true }
}

@ExportedBridge("__root___markedNullableUnit")
public fun __root___markedNullableUnit(): Boolean {
    val _result = markedNullableUnit()
    return (_result != null)
}

@ExportedBridge("__root___markedUnit")
public fun __root___markedUnit(): Boolean {
    markedUnit()
    return true
}

@ExportedBridge("__root___unmarked")
public fun __root___unmarked(): Int {
    val _result = unmarked()
    return _result
}

@ExportedBridge("__root___unmarkedNothing")
public fun __root___unmarkedNothing(): Boolean {
    val _result = unmarkedNothing()
    return _result
}

@ExportedBridge("__root___unmarkedNullableNothing")
public fun __root___unmarkedNullableNothing(): Boolean {
    val _result = unmarkedNullableNothing()
    return run { _result; true }
}

@ExportedBridge("__root___unmarkedUnit")
public fun __root___unmarkedUnit(): Boolean {
    unmarkedUnit()
    return true
}

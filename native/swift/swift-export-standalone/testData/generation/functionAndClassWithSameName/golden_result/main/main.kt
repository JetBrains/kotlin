@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(test.factory.ClassWithFactoryInAPackage::class, "22ExportedKotlinPackages4testO7factoryO4mainE26ClassWithFactoryInAPackageC")
@file:kotlin.native.internal.objc.BindClassToObjCName(test.factory.Outer::class, "22ExportedKotlinPackages4testO7factoryO4mainE5OuterC")
@file:kotlin.native.internal.objc.BindClassToObjCName(test.factory.Outer.Nested::class, "22ExportedKotlinPackages4testO7factoryO4mainE5OuterC6NestedC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ClassWithFactoryWithoutParameters::class, "4main33ClassWithFactoryWithoutParametersC")
@file:kotlin.native.internal.objc.BindClassToObjCName(EnumWithFactory::class, "4main15EnumWithFactoryC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ObjectWithFactory::class, "4main17ObjectWithFactoryC")
@file:kotlin.native.internal.objc.BindClassToObjCName(UtcOffset::class, "4main9UtcOffsetC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("ClassWithFactoryWithoutParameters_value_get")
public fun ClassWithFactoryWithoutParameters_value_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as ClassWithFactoryWithoutParameters
    val _result = __self.value
    return _result
}

@ExportedBridge("EnumWithFactory_ONE_get")
public fun EnumWithFactory_ONE_get(): kotlin.native.internal.NativePtr {
    val _result = EnumWithFactory.ONE
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("EnumWithFactory_entries_get")
public fun EnumWithFactory_entries_get(): kotlin.native.internal.NativePtr {
    val _result = EnumWithFactory.entries
    return _result.objcPtr()
}

@ExportedBridge("EnumWithFactory_valueOf__TypesOfArguments__Swift_String__")
public fun EnumWithFactory_valueOf__TypesOfArguments__Swift_String__(value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __value = interpretObjCPointer<kotlin.String>(value)
    val _result = EnumWithFactory.valueOf(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ClassWithFactoryWithoutParameters")
public fun __root___ClassWithFactoryWithoutParameters(): kotlin.native.internal.NativePtr {
    val _result = ClassWithFactoryWithoutParameters()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ClassWithFactoryWithoutParameters_init_allocate")
public fun __root___ClassWithFactoryWithoutParameters_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<ClassWithFactoryWithoutParameters>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ClassWithFactoryWithoutParameters_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__")
public fun __root___ClassWithFactoryWithoutParameters_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, value: Int): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    val __value = value
    kotlin.native.internal.initInstance(____kt, ClassWithFactoryWithoutParameters(__value))
}

@ExportedBridge("__root___EnumWithFactory__TypesOfArguments__Swift_Int32__")
public fun __root___EnumWithFactory__TypesOfArguments__Swift_Int32__(x: Int): kotlin.native.internal.NativePtr {
    val __x = x
    val _result = EnumWithFactory(__x)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___FlattenedPackageClass__TypesOfArguments__Swift_Float__")
public fun __root___FlattenedPackageClass__TypesOfArguments__Swift_Float__(f: Float): kotlin.native.internal.NativePtr {
    val __f = f
    val _result = FlattenedPackageClass(__f)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ObjectWithFactory")
public fun __root___ObjectWithFactory(): kotlin.native.internal.NativePtr {
    val _result = ObjectWithFactory()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ObjectWithFactory_get")
public fun __root___ObjectWithFactory_get(): kotlin.native.internal.NativePtr {
    val _result = ObjectWithFactory
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___UtcOffset__TypesOfArguments__Swift_Int32__")
public fun __root___UtcOffset__TypesOfArguments__Swift_Int32__(x: Int): kotlin.native.internal.NativePtr {
    val __x = x
    val _result = UtcOffset(__x)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___UtcOffset_init_allocate")
public fun __root___UtcOffset_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<UtcOffset>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___UtcOffset_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___UtcOffset_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, UtcOffset())
}

@ExportedBridge("test_factory_ClassWithFactoryInAPackage__TypesOfArguments__KotlinRuntime_KotlinBase__")
public fun test_factory_ClassWithFactoryInAPackage__TypesOfArguments__KotlinRuntime_KotlinBase__(arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as kotlin.Any
    val _result = test.factory.ClassWithFactoryInAPackage(__arg)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("test_factory_ClassWithFactoryInAPackage_init_allocate")
public fun test_factory_ClassWithFactoryInAPackage_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<test.factory.ClassWithFactoryInAPackage>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("test_factory_ClassWithFactoryInAPackage_init_initialize__TypesOfArguments__Swift_UInt__")
public fun test_factory_ClassWithFactoryInAPackage_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, test.factory.ClassWithFactoryInAPackage())
}

@ExportedBridge("test_factory_Nested")
public fun test_factory_Nested(): kotlin.native.internal.NativePtr {
    val _result = test.factory.Nested()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("test_factory_Outer_ClassWithFactoryInAPackage__TypesOfArguments__KotlinRuntime_KotlinBase__")
public fun test_factory_Outer_ClassWithFactoryInAPackage__TypesOfArguments__KotlinRuntime_KotlinBase__(self: kotlin.native.internal.NativePtr, arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as test.factory.Outer
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as kotlin.Any
    val _result = __self.ClassWithFactoryInAPackage(__arg)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("test_factory_Outer_Nested__TypesOfArguments__KotlinRuntime_KotlinBase__")
public fun test_factory_Outer_Nested__TypesOfArguments__KotlinRuntime_KotlinBase__(self: kotlin.native.internal.NativePtr, x: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as test.factory.Outer
    val __x = kotlin.native.internal.ref.dereferenceExternalRCRef(x) as kotlin.Any
    val _result = __self.Nested(__x)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("test_factory_Outer_Nested_init_allocate")
public fun test_factory_Outer_Nested_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<test.factory.Outer.Nested>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("test_factory_Outer_Nested_init_initialize__TypesOfArguments__Swift_UInt__")
public fun test_factory_Outer_Nested_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, test.factory.Outer.Nested())
}

@ExportedBridge("test_factory_Outer_init_allocate")
public fun test_factory_Outer_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<test.factory.Outer>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("test_factory_Outer_init_initialize__TypesOfArguments__Swift_UInt__")
public fun test_factory_Outer_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, test.factory.Outer())
}

@ExportedBridge("test_factory_modules_ClassFromDependency__TypesOfArguments__KotlinRuntime_KotlinBase__")
public fun test_factory_modules_ClassFromDependency__TypesOfArguments__KotlinRuntime_KotlinBase__(arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as kotlin.Any
    val _result = test.factory.modules.ClassFromDependency(__arg)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("test_not_factory_ClassWithFactoryInAPackage__TypesOfArguments__KotlinRuntime_KotlinBase__")
public fun test_not_factory_ClassWithFactoryInAPackage__TypesOfArguments__KotlinRuntime_KotlinBase__(arg: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __arg = kotlin.native.internal.ref.dereferenceExternalRCRef(arg) as kotlin.Any
    val _result = test.not.factory.ClassWithFactoryInAPackage(__arg)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("typealiases_TypealiasWithFactoryWithoutParameters")
public fun typealiases_TypealiasWithFactoryWithoutParameters(): kotlin.native.internal.NativePtr {
    val _result = typealiases.TypealiasWithFactoryWithoutParameters()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}


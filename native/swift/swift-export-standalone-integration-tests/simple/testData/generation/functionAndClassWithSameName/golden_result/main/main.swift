@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport
import dep
import flattened

public final class ClassWithFactoryWithoutParameters: KotlinRuntime.KotlinBase {
    public var value: Swift.Int32 {
        get {
            return ClassWithFactoryWithoutParameters_value_get(self.__externalRCRef())
        }
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public init(
        value: Swift.Int32
    ) {
        let __kt = __root___ClassWithFactoryWithoutParameters_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___ClassWithFactoryWithoutParameters_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt, value)
    }
}
public final class EnumWithFactory: KotlinRuntime.KotlinBase, Swift.CaseIterable {
    public static var ONE: main.EnumWithFactory {
        get {
            return main.EnumWithFactory(__externalRCRef: EnumWithFactory_ONE_get())
        }
    }
    public static var allCases: [main.EnumWithFactory] {
        get {
            return EnumWithFactory_entries_get() as! Swift.Array<main.EnumWithFactory>
        }
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public static func valueOf(
        value: Swift.String
    ) -> main.EnumWithFactory {
        return main.EnumWithFactory(__externalRCRef: EnumWithFactory_valueOf__TypesOfArguments__Swift_String__(value))
    }
}
public final class ObjectWithFactory: KotlinRuntime.KotlinBase {
    public static var shared: main.ObjectWithFactory {
        get {
            return main.ObjectWithFactory(__externalRCRef: __root___ObjectWithFactory_get())
        }
    }
    private override init() {
        fatalError()
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public final class UtcOffset: KotlinRuntime.KotlinBase {
    public override init() {
        let __kt = __root___UtcOffset_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___UtcOffset_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public func FlattenedPackageClass(
    f: Swift.Float
) -> ExportedKotlinPackages.flattenedPackage.FlattenedPackageClass {
    return ExportedKotlinPackages.flattenedPackage.FlattenedPackageClass(__externalRCRef: __root___FlattenedPackageClass__TypesOfArguments__Swift_Float__(f))
}
public func annotationWithFactory(
    arg: KotlinRuntime.KotlinBase
) -> Swift.Never {
    fatalError()
}
public func classWithFactoryWithoutParameters() -> main.ClassWithFactoryWithoutParameters {
    return main.ClassWithFactoryWithoutParameters(__externalRCRef: __root___ClassWithFactoryWithoutParameters())
}
public func enumWithFactory(
    x: Swift.Int32
) -> main.EnumWithFactory {
    return main.EnumWithFactory(__externalRCRef: __root___EnumWithFactory__TypesOfArguments__Swift_Int32__(x))
}
public func interfaceWithFactory() -> Swift.Never {
    fatalError()
}
public func interfaceWithFactory(
    arg: KotlinRuntime.KotlinBase
) -> Swift.Never {
    fatalError()
}
public func objectWithFactory() -> main.ObjectWithFactory {
    return main.ObjectWithFactory(__externalRCRef: __root___ObjectWithFactory())
}
public func utcOffset(
    x: Swift.Int32
) -> main.UtcOffset {
    return main.UtcOffset(__externalRCRef: __root___UtcOffset__TypesOfArguments__Swift_Int32__(x))
}
public extension ExportedKotlinPackages.test.factory {
    public final class ClassWithFactoryInAPackage: KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = test_factory_ClassWithFactoryInAPackage_init_allocate()
            super.init(__externalRCRef: __kt)
            test_factory_ClassWithFactoryInAPackage_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public final class Outer: KotlinRuntime.KotlinBase {
        public final class Nested: KotlinRuntime.KotlinBase {
            public override init() {
                let __kt = test_factory_Outer_Nested_init_allocate()
                super.init(__externalRCRef: __kt)
                test_factory_Outer_Nested_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
        }
        public override init() {
            let __kt = test_factory_Outer_init_allocate()
            super.init(__externalRCRef: __kt)
            test_factory_Outer_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public func ClassWithFactoryInAPackage(
            arg: KotlinRuntime.KotlinBase
        ) -> ExportedKotlinPackages.test.factory.ClassWithFactoryInAPackage {
            return ExportedKotlinPackages.test.factory.ClassWithFactoryInAPackage(__externalRCRef: test_factory_Outer_ClassWithFactoryInAPackage__TypesOfArguments__KotlinRuntime_KotlinBase__(self.__externalRCRef(), arg.__externalRCRef()))
        }
        public func Nested(
            x: KotlinRuntime.KotlinBase
        ) -> ExportedKotlinPackages.test.factory.Outer.Nested {
            return ExportedKotlinPackages.test.factory.Outer.Nested(__externalRCRef: test_factory_Outer_Nested__TypesOfArguments__KotlinRuntime_KotlinBase__(self.__externalRCRef(), x.__externalRCRef()))
        }
    }
    public static func Nested() -> ExportedKotlinPackages.test.factory.Outer.Nested {
        return ExportedKotlinPackages.test.factory.Outer.Nested(__externalRCRef: test_factory_Nested())
    }
    public static func classWithFactoryInAPackage(
        arg: KotlinRuntime.KotlinBase
    ) -> ExportedKotlinPackages.test.factory.ClassWithFactoryInAPackage {
        return ExportedKotlinPackages.test.factory.ClassWithFactoryInAPackage(__externalRCRef: test_factory_ClassWithFactoryInAPackage__TypesOfArguments__KotlinRuntime_KotlinBase__(arg.__externalRCRef()))
    }
}
public extension ExportedKotlinPackages.test.not.factory {
    public static func ClassWithFactoryInAPackage(
        arg: KotlinRuntime.KotlinBase
    ) -> ExportedKotlinPackages.test.factory.ClassWithFactoryInAPackage {
        return ExportedKotlinPackages.test.factory.ClassWithFactoryInAPackage(__externalRCRef: test_not_factory_ClassWithFactoryInAPackage__TypesOfArguments__KotlinRuntime_KotlinBase__(arg.__externalRCRef()))
    }
}
public extension ExportedKotlinPackages.test.factory.modules {
    public static func classFromDependency(
        arg: KotlinRuntime.KotlinBase
    ) -> ExportedKotlinPackages.test.factory.modules.ClassFromDependency {
        return ExportedKotlinPackages.test.factory.modules.ClassFromDependency(__externalRCRef: test_factory_modules_ClassFromDependency__TypesOfArguments__KotlinRuntime_KotlinBase__(arg.__externalRCRef()))
    }
}
public extension ExportedKotlinPackages.typealiases {
    public typealias TypealiasWithFactoryWithoutParameters = main.ClassWithFactoryWithoutParameters
    public typealias TypealiasWithFactoryWithoutParameters2 = main.ClassWithFactoryWithoutParameters
    public static func typealiasWithFactoryWithoutParameters() -> ExportedKotlinPackages.typealiases.TypealiasWithFactoryWithoutParameters {
        return main.ClassWithFactoryWithoutParameters(__externalRCRef: typealiases_TypealiasWithFactoryWithoutParameters())
    }
}

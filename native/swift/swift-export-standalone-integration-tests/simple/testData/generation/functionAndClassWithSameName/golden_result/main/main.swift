@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport
import dep
import flattened

public protocol InterfaceWithFactory: KotlinRuntime.KotlinBase {
}
@objc(_InterfaceWithFactory)
protocol _InterfaceWithFactory {
}
public final class ClassWithFactoryWithoutParameters: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public var value: Swift.Int32 {
        get {
            return ClassWithFactoryWithoutParameters_value_get(self.__externalRCRef())
        }
    }
    public init(
        value: Swift.Int32
    ) {
        precondition(Self.self == main.ClassWithFactoryWithoutParameters.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.ClassWithFactoryWithoutParameters ")
        let __kt = __root___ClassWithFactoryWithoutParameters_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___ClassWithFactoryWithoutParameters_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt, value)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class EnumWithFactory: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged, Swift.CaseIterable {
    public static var ONE: main.EnumWithFactory {
        get {
            return main.EnumWithFactory.__create(externalRCRef: EnumWithFactory_ONE_get())
        }
    }
    public static var allCases: [main.EnumWithFactory] {
        get {
            return EnumWithFactory_entries_get() as! Swift.Array<main.EnumWithFactory>
        }
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    public static func valueOf(
        value: Swift.String
    ) -> main.EnumWithFactory {
        return main.EnumWithFactory.__create(externalRCRef: EnumWithFactory_valueOf__TypesOfArguments__Swift_String__(value))
    }
}
public final class ObjectWithFactory: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public static var shared: main.ObjectWithFactory {
        get {
            return main.ObjectWithFactory.__create(externalRCRef: __root___ObjectWithFactory_get())
        }
    }
    private init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class UtcOffset: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public init() {
        precondition(Self.self == main.UtcOffset.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.UtcOffset ")
        let __kt = __root___UtcOffset_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___UtcOffset_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public func FlattenedPackageClass(
    f: Swift.Float
) -> ExportedKotlinPackages.flattenedPackage.FlattenedPackageClass {
    return ExportedKotlinPackages.flattenedPackage.FlattenedPackageClass.__create(externalRCRef: __root___FlattenedPackageClass__TypesOfArguments__Swift_Float__(f))
}
public func annotationWithFactory(
    arg: KotlinRuntime.KotlinBase
) -> Swift.Never {
    fatalError()
}
public func classWithFactoryWithoutParameters() -> main.ClassWithFactoryWithoutParameters {
    return main.ClassWithFactoryWithoutParameters.__create(externalRCRef: __root___ClassWithFactoryWithoutParameters())
}
public func enumWithFactory(
    x: Swift.Int32
) -> main.EnumWithFactory {
    return main.EnumWithFactory.__create(externalRCRef: __root___EnumWithFactory__TypesOfArguments__Swift_Int32__(x))
}
public func interfaceWithFactory() -> any main.InterfaceWithFactory {
    return KotlinRuntime.KotlinBase.__create(externalRCRef: __root___InterfaceWithFactory()) as! any main.InterfaceWithFactory
}
public func interfaceWithFactory(
    arg: KotlinRuntime.KotlinBase
) -> any main.InterfaceWithFactory {
    return KotlinRuntime.KotlinBase.__create(externalRCRef: __root___InterfaceWithFactory__TypesOfArguments__KotlinRuntime_KotlinBase__(arg.__externalRCRef())) as! any main.InterfaceWithFactory
}
public func objectWithFactory() -> main.ObjectWithFactory {
    return main.ObjectWithFactory.__create(externalRCRef: __root___ObjectWithFactory())
}
public func utcOffset(
    x: Swift.Int32
) -> main.UtcOffset {
    return main.UtcOffset.__create(externalRCRef: __root___UtcOffset__TypesOfArguments__Swift_Int32__(x))
}
public extension main.InterfaceWithFactory where Self : KotlinRuntimeSupport._KotlinBridged {
}
extension KotlinRuntimeSupport._KotlinExistential: main.InterfaceWithFactory where Wrapped : main._InterfaceWithFactory {
}
public extension ExportedKotlinPackages.test.factory {
    public final class ClassWithFactoryInAPackage: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public init() {
            precondition(Self.self == ExportedKotlinPackages.test.factory.ClassWithFactoryInAPackage.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.test.factory.ClassWithFactoryInAPackage ")
            let __kt = test_factory_ClassWithFactoryInAPackage_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            test_factory_ClassWithFactoryInAPackage_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class Outer: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public final class Nested: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public init() {
                precondition(Self.self == ExportedKotlinPackages.test.factory.Outer.Nested.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.test.factory.Outer.Nested ")
                let __kt = test_factory_Outer_Nested_init_allocate()
                super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
                test_factory_Outer_Nested_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
        }
        public init() {
            precondition(Self.self == ExportedKotlinPackages.test.factory.Outer.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.test.factory.Outer ")
            let __kt = test_factory_Outer_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            test_factory_Outer_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public func ClassWithFactoryInAPackage(
            arg: KotlinRuntime.KotlinBase
        ) -> ExportedKotlinPackages.test.factory.ClassWithFactoryInAPackage {
            return ExportedKotlinPackages.test.factory.ClassWithFactoryInAPackage.__create(externalRCRef: test_factory_Outer_ClassWithFactoryInAPackage__TypesOfArguments__KotlinRuntime_KotlinBase__(self.__externalRCRef(), arg.__externalRCRef()))
        }
        public func Nested(
            x: KotlinRuntime.KotlinBase
        ) -> ExportedKotlinPackages.test.factory.Outer.Nested {
            return ExportedKotlinPackages.test.factory.Outer.Nested.__create(externalRCRef: test_factory_Outer_Nested__TypesOfArguments__KotlinRuntime_KotlinBase__(self.__externalRCRef(), x.__externalRCRef()))
        }
    }
    public static func Nested() -> ExportedKotlinPackages.test.factory.Outer.Nested {
        return ExportedKotlinPackages.test.factory.Outer.Nested.__create(externalRCRef: test_factory_Nested())
    }
    public static func classWithFactoryInAPackage(
        arg: KotlinRuntime.KotlinBase
    ) -> ExportedKotlinPackages.test.factory.ClassWithFactoryInAPackage {
        return ExportedKotlinPackages.test.factory.ClassWithFactoryInAPackage.__create(externalRCRef: test_factory_ClassWithFactoryInAPackage__TypesOfArguments__KotlinRuntime_KotlinBase__(arg.__externalRCRef()))
    }
}
public extension ExportedKotlinPackages.test.not.factory {
    public static func ClassWithFactoryInAPackage(
        arg: KotlinRuntime.KotlinBase
    ) -> ExportedKotlinPackages.test.factory.ClassWithFactoryInAPackage {
        return ExportedKotlinPackages.test.factory.ClassWithFactoryInAPackage.__create(externalRCRef: test_not_factory_ClassWithFactoryInAPackage__TypesOfArguments__KotlinRuntime_KotlinBase__(arg.__externalRCRef()))
    }
}
public extension ExportedKotlinPackages.test.factory.modules {
    public static func classFromDependency(
        arg: KotlinRuntime.KotlinBase
    ) -> ExportedKotlinPackages.test.factory.modules.ClassFromDependency {
        return ExportedKotlinPackages.test.factory.modules.ClassFromDependency.__create(externalRCRef: test_factory_modules_ClassFromDependency__TypesOfArguments__KotlinRuntime_KotlinBase__(arg.__externalRCRef()))
    }
}
public extension ExportedKotlinPackages.typealiases {
    public typealias TypealiasWithFactoryWithoutParameters = main.ClassWithFactoryWithoutParameters
    public typealias TypealiasWithFactoryWithoutParameters2 = main.ClassWithFactoryWithoutParameters
    public static func typealiasWithFactoryWithoutParameters() -> ExportedKotlinPackages.typealiases.TypealiasWithFactoryWithoutParameters {
        return main.ClassWithFactoryWithoutParameters.__create(externalRCRef: typealiases_TypealiasWithFactoryWithoutParameters())
    }
}

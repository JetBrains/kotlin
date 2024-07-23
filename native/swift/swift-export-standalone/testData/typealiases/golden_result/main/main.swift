@_exported import ExportedKotlinPackages
import KotlinRuntime
@_implementationOnly import KotlinBridges_main

public typealias DefaultInteger = main.RegularInteger
public typealias RegularInteger = Swift.Int32
public typealias ShouldHaveNoAnnotation = Swift.Int32
public typealias dataObjectWithPackage = main.DATA_OBJECT_WITH_PACKAGE
public typealias never = Swift.Never
public class DATA_OBJECT_WITH_PACKAGE : KotlinRuntime.KotlinBase {
    public static var shared: main.DATA_OBJECT_WITH_PACKAGE {
        get {
            return main.DATA_OBJECT_WITH_PACKAGE(__externalRCRef: __root___DATA_OBJECT_WITH_PACKAGE_get())
        }
    }
    public var value: Swift.Int32 {
        get {
            return DATA_OBJECT_WITH_PACKAGE_value_get(self.__externalRCRef())
        }
    }
    public var variable: Swift.Int32 {
        get {
            return DATA_OBJECT_WITH_PACKAGE_variable_get(self.__externalRCRef())
        }
        set {
            return DATA_OBJECT_WITH_PACKAGE_variable_set__TypesOfArguments__int32_t__(self.__externalRCRef(), newValue)
        }
    }
    private override init() {
        fatalError()
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public func foo() -> Swift.Int32 {
        return DATA_OBJECT_WITH_PACKAGE_foo(self.__externalRCRef())
    }
    public func hashCode() -> Swift.Int32 {
        return DATA_OBJECT_WITH_PACKAGE_hashCode(self.__externalRCRef())
    }
    public func toString() -> Swift.String {
        return DATA_OBJECT_WITH_PACKAGE_toString(self.__externalRCRef())
    }
}
public func increment(
    integer: main.DefaultInteger
) -> main.RegularInteger {
    return __root___increment__TypesOfArguments__int32_t__(integer)
}
public extension ExportedKotlinPackages.typealiases.inner {
    public typealias Foo = ExportedKotlinPackages.typealiases.Foo
    public typealias LargeInteger = Swift.Int64
    public class Bar : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = typealiases_inner_Bar_init_allocate()
            super.init(__externalRCRef: __kt)
            typealiases_inner_Bar_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
}
public extension ExportedKotlinPackages.typealiases {
    public typealias Bar = ExportedKotlinPackages.typealiases.inner.Bar
    public typealias SmallInteger = Swift.Int16
    public class Foo : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = typealiases_Foo_init_allocate()
            super.init(__externalRCRef: __kt)
            typealiases_Foo_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
}

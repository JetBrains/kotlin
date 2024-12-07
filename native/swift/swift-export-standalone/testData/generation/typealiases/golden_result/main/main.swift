@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime

public typealias DefaultInteger = main.RegularInteger
public typealias RegularInteger = Swift.Int32
public typealias ShouldHaveNoAnnotation = Swift.Int32
public typealias abstractClss = main.ABSTRACT_CLASS
public typealias dataClass = main.DATA_CLASS
public typealias dataClassWithRef = main.DATA_CLASS_WITH_REF
public typealias dataObjectWithPackage = main.DATA_OBJECT_WITH_PACKAGE
public typealias enumClass = main.ENUM
public typealias inheritanceSingleClass = main.INHERITANCE_SINGLE_CLASS
public typealias never = Swift.Never
public typealias nullable_class = ExportedKotlinPackages.typealiases.Foo?
public typealias nullable_primitive = Swift.Int32?
public typealias objectWithClassInheritance = main.OBJECT_WITH_CLASS_INHERITANCE
public typealias openClass = main.OPEN_CLASS
public typealias sealedClass = main.SEALED
open class ABSTRACT_CLASS: KotlinRuntime.KotlinBase {
    package override init() {
        fatalError()
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public final class DATA_CLASS: KotlinRuntime.KotlinBase {
    public var a: Swift.Int32 {
        get {
            return DATA_CLASS_a_get(self.__externalRCRef())
        }
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public init(
        a: Swift.Int32
    ) {
        let __kt = __root___DATA_CLASS_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___DATA_CLASS_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt, a)
    }
    public func copy(
        a: Swift.Int32
    ) -> main.DATA_CLASS {
        return main.DATA_CLASS(__externalRCRef: DATA_CLASS_copy__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), a))
    }
    public func hashCode() -> Swift.Int32 {
        return DATA_CLASS_hashCode(self.__externalRCRef())
    }
    public func toString() -> Swift.String {
        return DATA_CLASS_toString(self.__externalRCRef())
    }
}
public final class DATA_CLASS_WITH_REF: KotlinRuntime.KotlinBase {
    public var o: KotlinRuntime.KotlinBase {
        get {
            return KotlinRuntime.KotlinBase(__externalRCRef: DATA_CLASS_WITH_REF_o_get(self.__externalRCRef()))
        }
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public init(
        o: KotlinRuntime.KotlinBase
    ) {
        let __kt = __root___DATA_CLASS_WITH_REF_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___DATA_CLASS_WITH_REF_init_initialize__TypesOfArguments__Swift_UInt_KotlinRuntime_KotlinBase__(__kt, o.__externalRCRef())
    }
    public func copy(
        o: KotlinRuntime.KotlinBase
    ) -> main.DATA_CLASS_WITH_REF {
        return main.DATA_CLASS_WITH_REF(__externalRCRef: DATA_CLASS_WITH_REF_copy__TypesOfArguments__KotlinRuntime_KotlinBase__(self.__externalRCRef(), o.__externalRCRef()))
    }
    public func hashCode() -> Swift.Int32 {
        return DATA_CLASS_WITH_REF_hashCode(self.__externalRCRef())
    }
    public func toString() -> Swift.String {
        return DATA_CLASS_WITH_REF_toString(self.__externalRCRef())
    }
}
public final class DATA_OBJECT_WITH_PACKAGE: KotlinRuntime.KotlinBase {
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
            return DATA_OBJECT_WITH_PACKAGE_variable_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue)
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
public final class ENUM: KotlinRuntime.KotlinBase, Swift.CaseIterable {
    public final class INSIDE_ENUM: KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = ENUM_INSIDE_ENUM_init_allocate()
            super.init(__externalRCRef: __kt)
            ENUM_INSIDE_ENUM_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public static var A: main.ENUM {
        get {
            return main.ENUM(__externalRCRef: ENUM_A_get())
        }
    }
    public static var B: main.ENUM {
        get {
            return main.ENUM(__externalRCRef: ENUM_B_get())
        }
    }
    public static var C: main.ENUM {
        get {
            return main.ENUM(__externalRCRef: ENUM_C_get())
        }
    }
    public static var allCases: [main.ENUM] {
        get {
            return ENUM_entries_get() as! Swift.Array<main.ENUM>
        }
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public static func valueOf(
        value: Swift.String
    ) -> main.ENUM {
        return main.ENUM(__externalRCRef: ENUM_valueOf__TypesOfArguments__Swift_String__(value))
    }
}
public final class INHERITANCE_SINGLE_CLASS: main.OPEN_CLASS {
    public override init() {
        let __kt = __root___INHERITANCE_SINGLE_CLASS_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___INHERITANCE_SINGLE_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public final class OBJECT_WITH_CLASS_INHERITANCE: main.OPEN_CLASS {
    public static var shared: main.OBJECT_WITH_CLASS_INHERITANCE {
        get {
            return main.OBJECT_WITH_CLASS_INHERITANCE(__externalRCRef: __root___OBJECT_WITH_CLASS_INHERITANCE_get())
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
open class OPEN_CLASS: KotlinRuntime.KotlinBase {
    public override init() {
        let __kt = __root___OPEN_CLASS_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___OPEN_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
open class SEALED: KotlinRuntime.KotlinBase {
    public final class O: main.SEALED {
        public static var shared: main.SEALED.O {
            get {
                return main.SEALED.O(__externalRCRef: SEALED_O_get())
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
    package override init() {
        fatalError()
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public func increment(
    integer: main.DefaultInteger
) -> main.RegularInteger {
    return __root___increment__TypesOfArguments__Swift_Int32__(integer)
}
public extension ExportedKotlinPackages.typealiases.inner {
    public typealias Foo = ExportedKotlinPackages.typealiases.Foo
    public typealias LargeInteger = Swift.Int64
    public final class Bar: KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = typealiases_inner_Bar_init_allocate()
            super.init(__externalRCRef: __kt)
            typealiases_inner_Bar_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
}
public extension ExportedKotlinPackages.typealiases {
    public typealias Bar = ExportedKotlinPackages.typealiases.inner.Bar
    public typealias SmallInteger = Swift.Int16
    public final class Foo: KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = typealiases_Foo_init_allocate()
            super.init(__externalRCRef: __kt)
            typealiases_Foo_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
}

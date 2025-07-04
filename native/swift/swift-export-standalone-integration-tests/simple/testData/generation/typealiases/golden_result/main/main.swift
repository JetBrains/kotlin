@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public typealias DefaultInteger = main.RegularInteger
public typealias RegularInteger = Swift.Int32
public typealias ShouldHaveNoAnnotation = Swift.Int32
public typealias abstractClss = main.ABSTRACT_CLASS
public typealias closure = () -> Swift.Void
public typealias dataClass = main.DATA_CLASS
public typealias dataClassWithRef = main.DATA_CLASS_WITH_REF
public typealias dataObjectWithPackage = main.DATA_OBJECT_WITH_PACKAGE
public typealias deeper_closure_typealias = main.closure
public typealias enumClass = main.ENUM
public typealias generic = main.GENERIC_CLASS
public typealias inheritanceSingleClass = main.INHERITANCE_SINGLE_CLASS
public typealias never = Swift.Never
public typealias nullable_class = ExportedKotlinPackages.typealiases.Foo?
public typealias nullable_primitive = Swift.Int32?
public typealias objectWithClassInheritance = main.OBJECT_WITH_CLASS_INHERITANCE
public typealias objectWithGenericInheritance = main.OBJECT_WITH_GENERIC_INHERITANCE
public typealias objectWithInterfaceInheritance = main.OBJECT_WITH_INTERFACE_INHERITANCE
public typealias openClass = main.OPEN_CLASS
public typealias outerInterface = any main.OUTSIDE_PROTO
public typealias sealedClass = main.SEALED
public protocol OUTSIDE_PROTO: KotlinRuntime.KotlinBase {
}
@objc(_OUTSIDE_PROTO)
protocol _OUTSIDE_PROTO {
}
open class ABSTRACT_CLASS: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    package init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class DATA_CLASS: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public var a: Swift.Int32 {
        get {
            return DATA_CLASS_a_get(self.__externalRCRef())
        }
    }
    public init(
        a: Swift.Int32
    ) {
        if Self.self != main.DATA_CLASS.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.DATA_CLASS ") }
        let __kt = __root___DATA_CLASS_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___DATA_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt, a)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    public static func ==(
        this: main.DATA_CLASS,
        other: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        this.equals(other: other)
    }
    public func copy(
        a: Swift.Int32
    ) -> main.DATA_CLASS {
        return main.DATA_CLASS.__createClassWrapper(externalRCRef: DATA_CLASS_copy__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), a))
    }
    public func equals(
        other: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        return DATA_CLASS_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
    }
    public func hashCode() -> Swift.Int32 {
        return DATA_CLASS_hashCode(self.__externalRCRef())
    }
    public func toString() -> Swift.String {
        return DATA_CLASS_toString(self.__externalRCRef())
    }
}
public final class DATA_CLASS_WITH_REF: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public var o: KotlinRuntime.KotlinBase {
        get {
            return KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: DATA_CLASS_WITH_REF_o_get(self.__externalRCRef()))
        }
    }
    public init(
        o: KotlinRuntime.KotlinBase
    ) {
        if Self.self != main.DATA_CLASS_WITH_REF.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.DATA_CLASS_WITH_REF ") }
        let __kt = __root___DATA_CLASS_WITH_REF_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___DATA_CLASS_WITH_REF_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_KotlinRuntime_KotlinBase__(__kt, o.__externalRCRef())
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    public static func ==(
        this: main.DATA_CLASS_WITH_REF,
        other: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        this.equals(other: other)
    }
    public func copy(
        o: KotlinRuntime.KotlinBase
    ) -> main.DATA_CLASS_WITH_REF {
        return main.DATA_CLASS_WITH_REF.__createClassWrapper(externalRCRef: DATA_CLASS_WITH_REF_copy__TypesOfArguments__KotlinRuntime_KotlinBase__(self.__externalRCRef(), o.__externalRCRef()))
    }
    public func equals(
        other: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        return DATA_CLASS_WITH_REF_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
    }
    public func hashCode() -> Swift.Int32 {
        return DATA_CLASS_WITH_REF_hashCode(self.__externalRCRef())
    }
    public func toString() -> Swift.String {
        return DATA_CLASS_WITH_REF_toString(self.__externalRCRef())
    }
}
public final class DATA_OBJECT_WITH_PACKAGE: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public static var shared: main.DATA_OBJECT_WITH_PACKAGE {
        get {
            return main.DATA_OBJECT_WITH_PACKAGE.__createClassWrapper(externalRCRef: __root___DATA_OBJECT_WITH_PACKAGE_get())
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
    private init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    public static func ==(
        this: main.DATA_OBJECT_WITH_PACKAGE,
        other: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        this.equals(other: other)
    }
    public func equals(
        other: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        return DATA_OBJECT_WITH_PACKAGE_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
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
public final class ENUM: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged, Swift.CaseIterable {
    public final class INSIDE_ENUM: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public init() {
            if Self.self != main.ENUM.INSIDE_ENUM.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.ENUM.INSIDE_ENUM ") }
            let __kt = ENUM_INSIDE_ENUM_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            ENUM_INSIDE_ENUM_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public static var A: main.ENUM {
        get {
            return main.ENUM.__createClassWrapper(externalRCRef: ENUM_A_get())
        }
    }
    public static var B: main.ENUM {
        get {
            return main.ENUM.__createClassWrapper(externalRCRef: ENUM_B_get())
        }
    }
    public static var C: main.ENUM {
        get {
            return main.ENUM.__createClassWrapper(externalRCRef: ENUM_C_get())
        }
    }
    public static var allCases: [main.ENUM] {
        get {
            return ENUM_entries_get() as! Swift.Array<main.ENUM>
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
    ) -> main.ENUM {
        return main.ENUM.__createClassWrapper(externalRCRef: ENUM_valueOf__TypesOfArguments__Swift_String__(value))
    }
}
public final class GENERIC_CLASS: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public init() {
        if Self.self != main.GENERIC_CLASS.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.GENERIC_CLASS ") }
        let __kt = __root___GENERIC_CLASS_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___GENERIC_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class INHERITANCE_SINGLE_CLASS: main.OPEN_CLASS {
    public override init() {
        if Self.self != main.INHERITANCE_SINGLE_CLASS.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.INHERITANCE_SINGLE_CLASS ") }
        let __kt = __root___INHERITANCE_SINGLE_CLASS_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___INHERITANCE_SINGLE_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class OBJECT_WITH_CLASS_INHERITANCE: main.OPEN_CLASS {
    public static var shared: main.OBJECT_WITH_CLASS_INHERITANCE {
        get {
            return main.OBJECT_WITH_CLASS_INHERITANCE.__createClassWrapper(externalRCRef: __root___OBJECT_WITH_CLASS_INHERITANCE_get())
        }
    }
    private override init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class OBJECT_WITH_GENERIC_INHERITANCE: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public static var shared: main.OBJECT_WITH_GENERIC_INHERITANCE {
        get {
            return main.OBJECT_WITH_GENERIC_INHERITANCE.__createClassWrapper(externalRCRef: __root___OBJECT_WITH_GENERIC_INHERITANCE_get())
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
    public func hasNext() -> Swift.Bool {
        return OBJECT_WITH_GENERIC_INHERITANCE_hasNext(self.__externalRCRef())
    }
    public func hasPrevious() -> Swift.Bool {
        return OBJECT_WITH_GENERIC_INHERITANCE_hasPrevious(self.__externalRCRef())
    }
    public func next() -> Swift.Never {
        return OBJECT_WITH_GENERIC_INHERITANCE_next(self.__externalRCRef())
    }
    public func nextIndex() -> Swift.Int32 {
        return OBJECT_WITH_GENERIC_INHERITANCE_nextIndex(self.__externalRCRef())
    }
    public func previous() -> Swift.Never {
        return OBJECT_WITH_GENERIC_INHERITANCE_previous(self.__externalRCRef())
    }
    public func previousIndex() -> Swift.Int32 {
        return OBJECT_WITH_GENERIC_INHERITANCE_previousIndex(self.__externalRCRef())
    }
}
public final class OBJECT_WITH_INTERFACE_INHERITANCE: KotlinRuntime.KotlinBase, main.OUTSIDE_PROTO, main._OUTSIDE_PROTO, KotlinRuntimeSupport._KotlinBridged {
    public static var shared: main.OBJECT_WITH_INTERFACE_INHERITANCE {
        get {
            return main.OBJECT_WITH_INTERFACE_INHERITANCE.__createClassWrapper(externalRCRef: __root___OBJECT_WITH_INTERFACE_INHERITANCE_get())
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
open class OPEN_CLASS: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public init() {
        if Self.self != main.OPEN_CLASS.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.OPEN_CLASS ") }
        let __kt = __root___OPEN_CLASS_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___OPEN_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
open class SEALED: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public final class O: main.SEALED {
        public static var shared: main.SEALED.O {
            get {
                return main.SEALED.O.__createClassWrapper(externalRCRef: SEALED_O_get())
            }
        }
        private override init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    package init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public var block: main.closure {
    get {
        return {
            let nativeBlock = __root___block_get()
            return { nativeBlock() }
        }()
    }
    set {
        return __root___block_set__TypesOfArguments__U2829202D_U20Swift_Void__({
            let originalBlock = newValue
            return { return originalBlock() }
        }())
    }
}
public func consume_closure(
    block: @escaping main.closure
) -> Swift.Void {
    return __root___consume_closure__TypesOfArguments__U2829202D_U20Swift_Void__({
        let originalBlock = block
        return { return originalBlock() }
    }())
}
public func deeper_closure_typealiase(
    block: @escaping main.deeper_closure_typealias
) -> main.deeper_closure_typealias {
    return {
        let nativeBlock = __root___deeper_closure_typealiase__TypesOfArguments__U2829202D_U20Swift_Void__({
        let originalBlock = block
        return { return originalBlock() }
    }())
        return { nativeBlock() }
    }()
}
public func increment(
    integer: main.DefaultInteger
) -> main.RegularInteger {
    return __root___increment__TypesOfArguments__Swift_Int32__(integer)
}
public func produce_closure() -> main.closure {
    return {
        let nativeBlock = __root___produce_closure()
        return { nativeBlock() }
    }()
}
public extension main.OUTSIDE_PROTO where Self : KotlinRuntimeSupport._KotlinBridged {
}
extension KotlinRuntimeSupport._KotlinExistential: main.OUTSIDE_PROTO where Wrapped : main._OUTSIDE_PROTO {
}
public extension ExportedKotlinPackages.typealiases.inner {
    public typealias Foo = ExportedKotlinPackages.typealiases.Foo
    public typealias LargeInteger = Swift.Int64
    public final class Bar: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public init() {
            if Self.self != ExportedKotlinPackages.typealiases.inner.Bar.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.typealiases.inner.Bar ") }
            let __kt = typealiases_inner_Bar_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            typealiases_inner_Bar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
}
public extension ExportedKotlinPackages.typealiases {
    public typealias Bar = ExportedKotlinPackages.typealiases.inner.Bar
    public typealias SmallInteger = Swift.Int16
    public final class Foo: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public init() {
            if Self.self != ExportedKotlinPackages.typealiases.Foo.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.typealiases.Foo ") }
            let __kt = typealiases_Foo_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            typealiases_Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
}

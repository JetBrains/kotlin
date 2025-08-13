@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public protocol Barable: KotlinRuntime.KotlinBase, main.Foeble {
    var baz: any main.Foeble {
        get
    }
    func bar(
        arg: any main.Foeble
    ) -> any main.Barable
}
public protocol Bazzable: KotlinRuntime.KotlinBase {
}
public protocol ContainerProtocol: KotlinRuntime.KotlinBase {
    typealias NestedClass = main._ContainerProtocol_NestedClass
    typealias NestedProtocol = main._ContainerProtocol_NestedProtocol
}
public protocol Foeble: KotlinRuntime.KotlinBase {
    var baz: any main.Foeble {
        get
    }
    func bar(
        arg: any main.Foeble
    ) -> any main.Foeble
}
public protocol SealedBazzable: KotlinRuntime.KotlinBase, main.SealedFoeble {
}
public protocol SealedFoeble: KotlinRuntime.KotlinBase {
    typealias SealedBarable = main._SealedFoeble_SealedBarable
    typealias SomeBarable = main._SealedFoeble_SomeBarable
    typealias SomeFoeble = main._SealedFoeble_SomeFoeble
}
public protocol SiblingProtocol: KotlinRuntime.KotlinBase {
    typealias NestedClass = main._SiblingProtocol_NestedClass
}
@objc(_Barable)
package protocol _Barable: main._Foeble {
}
@objc(_Bazzable)
package protocol _Bazzable {
}
@objc(_ContainerProtocol)
package protocol _ContainerProtocol {
}
public protocol _ContainerProtocol_NestedProtocol: KotlinRuntime.KotlinBase {
    typealias NestedClass = main.__ContainerProtocol_NestedProtocol_NestedClass
}
public protocol _ExportedKotlinPackages_packagewithprotocols_ContainerProtocol_NestedProtocol: KotlinRuntime.KotlinBase {
    typealias NestedClass = main.__ExportedKotlinPackages_packagewithprotocols_ContainerProtocol_NestedProtocol_NestedClass
}
@objc(_Foeble)
package protocol _Foeble {
}
@objc(_SealedBazzable)
package protocol _SealedBazzable: main._SealedFoeble {
}
@objc(_SealedFoeble)
package protocol _SealedFoeble {
}
public protocol _SealedFoeble_SealedBarable: KotlinRuntime.KotlinBase, main.SealedFoeble {
}
@objc(_SiblingProtocol)
package protocol _SiblingProtocol {
}
@objc(__ContainerProtocol_NestedProtocol)
package protocol __ContainerProtocol_NestedProtocol {
}
@objc(__ExportedKotlinPackages_packagewithprotocols_ContainerProtocol_NestedProtocol)
package protocol __ExportedKotlinPackages_packagewithprotocols_ContainerProtocol_NestedProtocol {
}
@objc(__SealedFoeble_SealedBarable)
package protocol __SealedFoeble_SealedBarable: main._SealedFoeble {
}
public final class Bar: KotlinRuntime.KotlinBase, main.Barable, main._Barable, main.Foeble, main._Foeble, main.Bazzable, main._Bazzable {
    public var baz: main.Bar {
        get {
            return main.Bar.__createClassWrapper(externalRCRef: Bar_baz_get(self.__externalRCRef()))
        }
    }
    public init() {
        if Self.self != main.Bar.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Bar ") }
        let __kt = __root___Bar_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___Bar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    public func bar(
        arg: any main.Foeble
    ) -> main.Bar {
        return main.Bar.__createClassWrapper(externalRCRef: Bar_bar__TypesOfArguments__anyU20main_Foeble__(self.__externalRCRef(), arg.__externalRCRef()))
    }
}
public final class Foo: KotlinRuntime.KotlinBase, main.Foeble, main._Foeble {
    public var baz: any main.Foeble {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: Foo_baz_get(self.__externalRCRef())) as! any main.Foeble
        }
    }
    public init() {
        if Self.self != main.Foo.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Foo ") }
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    public func bar(
        arg: any main.Foeble
    ) -> main.Foo {
        return main.Foo.__createClassWrapper(externalRCRef: Foo_bar__TypesOfArguments__anyU20main_Foeble__(self.__externalRCRef(), arg.__externalRCRef()))
    }
}
public final class MyObject: KotlinRuntime.KotlinBase {
    public static var shared: main.MyObject {
        get {
            return main.MyObject.__createClassWrapper(externalRCRef: __root___MyObject_get())
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
public final class SomeBazzable: KotlinRuntime.KotlinBase, main.SealedBazzable, main._SealedBazzable {
    public static var shared: main.SomeBazzable {
        get {
            return main.SomeBazzable.__createClassWrapper(externalRCRef: __root___SomeBazzable_get())
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
open class _ContainerProtocol_NestedClass: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != main._ContainerProtocol_NestedClass.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main._ContainerProtocol_NestedClass ") }
        let __kt = ContainerProtocol_NestedClass_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        ContainerProtocol_NestedClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
open class _ExportedKotlinPackages_packagewithprotocols_ContainerProtocol_NestedClass: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != main._ExportedKotlinPackages_packagewithprotocols_ContainerProtocol_NestedClass.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main._ExportedKotlinPackages_packagewithprotocols_ContainerProtocol_NestedClass ") }
        let __kt = packagewithprotocols_ContainerProtocol_NestedClass_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        packagewithprotocols_ContainerProtocol_NestedClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class _ExportedKotlinPackages_packagewithprotocols_SiblingProtocol_NestedClass: KotlinRuntime.KotlinBase {
    public final class NestedClass: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != main._ExportedKotlinPackages_packagewithprotocols_SiblingProtocol_NestedClass.NestedClass.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main._ExportedKotlinPackages_packagewithprotocols_SiblingProtocol_NestedClass.NestedClass ") }
            let __kt = packagewithprotocols_SiblingProtocol_NestedClass_NestedClass_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            packagewithprotocols_SiblingProtocol_NestedClass_NestedClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public init() {
        if Self.self != main._ExportedKotlinPackages_packagewithprotocols_SiblingProtocol_NestedClass.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main._ExportedKotlinPackages_packagewithprotocols_SiblingProtocol_NestedClass ") }
        let __kt = packagewithprotocols_SiblingProtocol_NestedClass_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        packagewithprotocols_SiblingProtocol_NestedClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class _SealedFoeble_SomeBarable: KotlinRuntime.KotlinBase, main._SealedFoeble_SealedBarable, main.__SealedFoeble_SealedBarable {
    public static var shared: main._SealedFoeble_SomeBarable {
        get {
            return main._SealedFoeble_SomeBarable.__createClassWrapper(externalRCRef: SealedFoeble_SomeBarable_get())
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
public final class _SealedFoeble_SomeFoeble: KotlinRuntime.KotlinBase, main.SealedFoeble, main._SealedFoeble {
    public static var shared: main._SealedFoeble_SomeFoeble {
        get {
            return main._SealedFoeble_SomeFoeble.__createClassWrapper(externalRCRef: SealedFoeble_SomeFoeble_get())
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
public final class _SiblingProtocol_NestedClass: KotlinRuntime.KotlinBase {
    public final class NestedClass: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != main._SiblingProtocol_NestedClass.NestedClass.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main._SiblingProtocol_NestedClass.NestedClass ") }
            let __kt = SiblingProtocol_NestedClass_NestedClass_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            SiblingProtocol_NestedClass_NestedClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public init() {
        if Self.self != main._SiblingProtocol_NestedClass.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main._SiblingProtocol_NestedClass ") }
        let __kt = SiblingProtocol_NestedClass_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        SiblingProtocol_NestedClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
open class __ContainerProtocol_NestedProtocol_NestedClass: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != main.__ContainerProtocol_NestedProtocol_NestedClass.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.__ContainerProtocol_NestedProtocol_NestedClass ") }
        let __kt = ContainerProtocol_NestedProtocol_NestedClass_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        ContainerProtocol_NestedProtocol_NestedClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
open class __ExportedKotlinPackages_packagewithprotocols_ContainerProtocol_NestedProtocol_NestedClass: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != main.__ExportedKotlinPackages_packagewithprotocols_ContainerProtocol_NestedProtocol_NestedClass.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.__ExportedKotlinPackages_packagewithprotocols_ContainerProtocol_NestedProtocol_NestedClass ") }
        let __kt = packagewithprotocols_ContainerProtocol_NestedProtocol_NestedClass_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        packagewithprotocols_ContainerProtocol_NestedProtocol_NestedClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public var list: [any main.Foeble] {
    get {
        return __root___list_get() as! Swift.Array<any main.Foeble>
    }
    set {
        return __root___list_set__TypesOfArguments__Swift_Array_anyU20main_Foeble___(newValue)
    }
}
public var normal: any main.Foeble {
    get {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___normal_get()) as! any main.Foeble
    }
    set {
        return __root___normal_set__TypesOfArguments__anyU20main_Foeble__(newValue.__externalRCRef())
    }
}
public var nullable: (any main.Foeble)? {
    get {
        return { switch __root___nullable_get() { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res) as! any main.Foeble; } }()
    }
    set {
        return __root___nullable_set__TypesOfArguments__Swift_Optional_anyU20main_Foeble___(newValue.map { it in it.__externalRCRef() } ?? nil)
    }
}
public func foo(
    _ receiver: any main.ContainerProtocol
) -> Swift.Void {
    return __root___foo__TypesOfArguments__anyU20main_ContainerProtocol__(receiver.__externalRCRef())
}
public func foo(
    _ receiver: main.__ContainerProtocol_NestedProtocol_NestedClass
) -> Swift.Void {
    return __root___foo__TypesOfArguments__main___ContainerProtocol_NestedProtocol_NestedClass__(receiver.__externalRCRef())
}
public func foo(
    _ receiver: main._SiblingProtocol_NestedClass
) -> Swift.Void {
    return __root___foo__TypesOfArguments__main__SiblingProtocol_NestedClass__(receiver.__externalRCRef())
}
public func foo(
    _ receiver: any main._ContainerProtocol_NestedProtocol
) -> Swift.Void {
    return __root___foo__TypesOfArguments__anyU20main__ContainerProtocol_NestedProtocol__(receiver.__externalRCRef())
}
public func list(
    value: [any main.Foeble]
) -> [any main.Foeble] {
    return __root___list__TypesOfArguments__Swift_Array_anyU20main_Foeble___(value) as! Swift.Array<any main.Foeble>
}
public func normal(
    value: any main.Foeble
) -> any main.Foeble {
    return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___normal__TypesOfArguments__anyU20main_Foeble__(value.__externalRCRef())) as! any main.Foeble
}
public func nullable(
    value: (any main.Foeble)?
) -> (any main.Foeble)? {
    return { switch __root___nullable__TypesOfArguments__Swift_Optional_anyU20main_Foeble___(value.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res) as! any main.Foeble; } }()
}
extension main.Barable where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var baz: any main.Foeble {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: Barable_baz_get(self.__externalRCRef())) as! any main.Foeble
        }
    }
    public func bar(
        arg: any main.Foeble
    ) -> any main.Barable {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: Barable_bar__TypesOfArguments__anyU20main_Foeble__(self.__externalRCRef(), arg.__externalRCRef())) as! any main.Barable
    }
}
extension ExportedKotlinPackages.repeating_conformances.Barable where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension main.Bazzable where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension main.ContainerProtocol where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension ExportedKotlinPackages.packagewithprotocols.ContainerProtocol where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension main.Foeble where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var baz: any main.Foeble {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: Foeble_baz_get(self.__externalRCRef())) as! any main.Foeble
        }
    }
    public func bar(
        arg: any main.Foeble
    ) -> any main.Foeble {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: Foeble_bar__TypesOfArguments__anyU20main_Foeble__(self.__externalRCRef(), arg.__externalRCRef())) as! any main.Foeble
    }
}
extension ExportedKotlinPackages.repeating_conformances.Foeble where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension main.SealedBazzable where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension main.SealedFoeble where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension main.SiblingProtocol where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension ExportedKotlinPackages.packagewithprotocols.SiblingProtocol where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension main._ContainerProtocol_NestedProtocol where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension main._ExportedKotlinPackages_packagewithprotocols_ContainerProtocol_NestedProtocol where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension KotlinRuntimeSupport._KotlinExistential: main.ContainerProtocol where Wrapped : main._ContainerProtocol {
}
extension KotlinRuntimeSupport._KotlinExistential: main.SiblingProtocol where Wrapped : main._SiblingProtocol {
}
extension KotlinRuntimeSupport._KotlinExistential: main.Foeble where Wrapped : main._Foeble {
}
extension KotlinRuntimeSupport._KotlinExistential: main.Barable where Wrapped : main._Barable {
}
extension KotlinRuntimeSupport._KotlinExistential: main.Bazzable where Wrapped : main._Bazzable {
}
extension KotlinRuntimeSupport._KotlinExistential: main.SealedFoeble where Wrapped : main._SealedFoeble {
}
extension KotlinRuntimeSupport._KotlinExistential: main.SealedBazzable where Wrapped : main._SealedBazzable {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.packagewithprotocols.ContainerProtocol where Wrapped : ExportedKotlinPackages.packagewithprotocols._ContainerProtocol {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.packagewithprotocols.SiblingProtocol where Wrapped : ExportedKotlinPackages.packagewithprotocols._SiblingProtocol {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.repeating_conformances.Foeble where Wrapped : ExportedKotlinPackages.repeating_conformances._Foeble {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.repeating_conformances.Barable where Wrapped : ExportedKotlinPackages.repeating_conformances._Barable {
}
extension KotlinRuntimeSupport._KotlinExistential: main._ContainerProtocol_NestedProtocol where Wrapped : main.__ContainerProtocol_NestedProtocol {
}
extension KotlinRuntimeSupport._KotlinExistential: main._SealedFoeble_SealedBarable where Wrapped : main.__SealedFoeble_SealedBarable {
}
extension KotlinRuntimeSupport._KotlinExistential: main._ExportedKotlinPackages_packagewithprotocols_ContainerProtocol_NestedProtocol where Wrapped : main.__ExportedKotlinPackages_packagewithprotocols_ContainerProtocol_NestedProtocol {
}
extension main._SealedFoeble_SealedBarable where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension ExportedKotlinPackages.packagewithprotocols {
    public enum ENUM_WITH_INTERFACE_INHERITANCE: KotlinRuntimeSupport._KotlinBridgeable, Swift.CaseIterable {
        public static var allCases: [ExportedKotlinPackages.packagewithprotocols.ENUM_WITH_INTERFACE_INHERITANCE] {
            get {
                return packagewithprotocols_ENUM_WITH_INTERFACE_INHERITANCE_entries_get() as! Swift.Array<ExportedKotlinPackages.packagewithprotocols.ENUM_WITH_INTERFACE_INHERITANCE>
            }
        }
        public init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer!,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public static func valueOf(
            value: Swift.String
        ) -> ExportedKotlinPackages.packagewithprotocols.ENUM_WITH_INTERFACE_INHERITANCE {
            return ExportedKotlinPackages.packagewithprotocols.ENUM_WITH_INTERFACE_INHERITANCE.__createClassWrapper(externalRCRef: packagewithprotocols_ENUM_WITH_INTERFACE_INHERITANCE_valueOf__TypesOfArguments__Swift_String__(value))
        }
    }
    public protocol ContainerProtocol: KotlinRuntime.KotlinBase {
        typealias NestedClass = main._ExportedKotlinPackages_packagewithprotocols_ContainerProtocol_NestedClass
        typealias NestedProtocol = main._ExportedKotlinPackages_packagewithprotocols_ContainerProtocol_NestedProtocol
    }
    public protocol SiblingProtocol: KotlinRuntime.KotlinBase {
        typealias NestedClass = main._ExportedKotlinPackages_packagewithprotocols_SiblingProtocol_NestedClass
    }
    @objc(_ContainerProtocol)
    package protocol _ContainerProtocol {
    }
    @objc(_SiblingProtocol)
    package protocol _SiblingProtocol {
    }
    public final class INHERITANCE_COUPLE: main._ExportedKotlinPackages_packagewithprotocols_ContainerProtocol_NestedClass, ExportedKotlinPackages.packagewithprotocols.ContainerProtocol, ExportedKotlinPackages.packagewithprotocols._ContainerProtocol {
        public override init() {
            if Self.self != ExportedKotlinPackages.packagewithprotocols.INHERITANCE_COUPLE.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.packagewithprotocols.INHERITANCE_COUPLE ") }
            let __kt = packagewithprotocols_INHERITANCE_COUPLE_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            packagewithprotocols_INHERITANCE_COUPLE_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class INHERITANCE_SINGLE_PROTO: main._ExportedKotlinPackages_packagewithprotocols_ContainerProtocol_NestedClass {
        public override init() {
            if Self.self != ExportedKotlinPackages.packagewithprotocols.INHERITANCE_SINGLE_PROTO.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.packagewithprotocols.INHERITANCE_SINGLE_PROTO ") }
            let __kt = packagewithprotocols_INHERITANCE_SINGLE_PROTO_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            packagewithprotocols_INHERITANCE_SINGLE_PROTO_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class OBJECT_WITH_INTERFACE_INHERITANCE: KotlinRuntime.KotlinBase, ExportedKotlinPackages.packagewithprotocols.ContainerProtocol, ExportedKotlinPackages.packagewithprotocols._ContainerProtocol {
        public static var shared: ExportedKotlinPackages.packagewithprotocols.OBJECT_WITH_INTERFACE_INHERITANCE {
            get {
                return ExportedKotlinPackages.packagewithprotocols.OBJECT_WITH_INTERFACE_INHERITANCE.__createClassWrapper(externalRCRef: packagewithprotocols_OBJECT_WITH_INTERFACE_INHERITANCE_get())
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
    public static func foo(
        _ receiver: any ExportedKotlinPackages.packagewithprotocols.ContainerProtocol
    ) -> Swift.Void {
        return packagewithprotocols_foo__TypesOfArguments__anyU20ExportedKotlinPackages_packagewithprotocols_ContainerProtocol__(receiver.__externalRCRef())
    }
    public static func foo(
        _ receiver: main.__ExportedKotlinPackages_packagewithprotocols_ContainerProtocol_NestedProtocol_NestedClass
    ) -> Swift.Void {
        return packagewithprotocols_foo__TypesOfArguments__main___ExportedKotlinPackages_packagewithprotocols_ContainerProtocol_NestedProtocol_NestedClass__(receiver.__externalRCRef())
    }
    public static func foo(
        _ receiver: main._ExportedKotlinPackages_packagewithprotocols_SiblingProtocol_NestedClass
    ) -> Swift.Void {
        return packagewithprotocols_foo__TypesOfArguments__main__ExportedKotlinPackages_packagewithprotocols_SiblingProtocol_NestedClass__(receiver.__externalRCRef())
    }
    public static func foo(
        _ receiver: any main._ExportedKotlinPackages_packagewithprotocols_ContainerProtocol_NestedProtocol
    ) -> Swift.Void {
        return packagewithprotocols_foo__TypesOfArguments__anyU20main__ExportedKotlinPackages_packagewithprotocols_ContainerProtocol_NestedProtocol__(receiver.__externalRCRef())
    }
}
extension ExportedKotlinPackages.repeating_conformances {
    public protocol Barable: KotlinRuntime.KotlinBase, ExportedKotlinPackages.repeating_conformances.Foeble {
    }
    public protocol Foeble: KotlinRuntime.KotlinBase {
    }
    @objc(_Barable)
    package protocol _Barable: ExportedKotlinPackages.repeating_conformances._Foeble {
    }
    @objc(_Foeble)
    package protocol _Foeble {
    }
    open class Child1: ExportedKotlinPackages.repeating_conformances.Parent1 {
        public override init() {
            if Self.self != ExportedKotlinPackages.repeating_conformances.Child1.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.repeating_conformances.Child1 ") }
            let __kt = repeating_conformances_Child1_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            repeating_conformances_Child1_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class Child2: ExportedKotlinPackages.repeating_conformances.Parent2 {
        public override init() {
            if Self.self != ExportedKotlinPackages.repeating_conformances.Child2.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.repeating_conformances.Child2 ") }
            let __kt = repeating_conformances_Child2_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            repeating_conformances_Child2_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class Child3: ExportedKotlinPackages.repeating_conformances.Parent3 {
        public override init() {
            if Self.self != ExportedKotlinPackages.repeating_conformances.Child3.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.repeating_conformances.Child3 ") }
            let __kt = repeating_conformances_Child3_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            repeating_conformances_Child3_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class Child4: ExportedKotlinPackages.repeating_conformances.Parent4 {
        public override init() {
            if Self.self != ExportedKotlinPackages.repeating_conformances.Child4.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.repeating_conformances.Child4 ") }
            let __kt = repeating_conformances_Child4_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            repeating_conformances_Child4_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class Child5: ExportedKotlinPackages.repeating_conformances.Parent5 {
        public override init() {
            if Self.self != ExportedKotlinPackages.repeating_conformances.Child5.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.repeating_conformances.Child5 ") }
            let __kt = repeating_conformances_Child5_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            repeating_conformances_Child5_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class GrandChild1: ExportedKotlinPackages.repeating_conformances.Child1 {
        public override init() {
            if Self.self != ExportedKotlinPackages.repeating_conformances.GrandChild1.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.repeating_conformances.GrandChild1 ") }
            let __kt = repeating_conformances_GrandChild1_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            repeating_conformances_GrandChild1_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class GrandChild2: ExportedKotlinPackages.repeating_conformances.Child2 {
        public override init() {
            if Self.self != ExportedKotlinPackages.repeating_conformances.GrandChild2.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.repeating_conformances.GrandChild2 ") }
            let __kt = repeating_conformances_GrandChild2_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            repeating_conformances_GrandChild2_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class GrandChild3: ExportedKotlinPackages.repeating_conformances.Child3 {
        public override init() {
            if Self.self != ExportedKotlinPackages.repeating_conformances.GrandChild3.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.repeating_conformances.GrandChild3 ") }
            let __kt = repeating_conformances_GrandChild3_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            repeating_conformances_GrandChild3_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class GrandChild4: ExportedKotlinPackages.repeating_conformances.Child4, ExportedKotlinPackages.repeating_conformances.Barable, ExportedKotlinPackages.repeating_conformances._Barable {
        public override init() {
            if Self.self != ExportedKotlinPackages.repeating_conformances.GrandChild4.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.repeating_conformances.GrandChild4 ") }
            let __kt = repeating_conformances_GrandChild4_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            repeating_conformances_GrandChild4_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class GrandChild5: ExportedKotlinPackages.repeating_conformances.Child5, ExportedKotlinPackages.repeating_conformances.Barable, ExportedKotlinPackages.repeating_conformances._Barable, ExportedKotlinPackages.repeating_conformances.Foeble, ExportedKotlinPackages.repeating_conformances._Foeble {
        public override init() {
            if Self.self != ExportedKotlinPackages.repeating_conformances.GrandChild5.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.repeating_conformances.GrandChild5 ") }
            let __kt = repeating_conformances_GrandChild5_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            repeating_conformances_GrandChild5_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class Parent1: KotlinRuntime.KotlinBase, ExportedKotlinPackages.repeating_conformances.Foeble, ExportedKotlinPackages.repeating_conformances._Foeble {
        public init() {
            if Self.self != ExportedKotlinPackages.repeating_conformances.Parent1.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.repeating_conformances.Parent1 ") }
            let __kt = repeating_conformances_Parent1_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            repeating_conformances_Parent1_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class Parent2: KotlinRuntime.KotlinBase, ExportedKotlinPackages.repeating_conformances.Foeble, ExportedKotlinPackages.repeating_conformances._Foeble {
        public init() {
            if Self.self != ExportedKotlinPackages.repeating_conformances.Parent2.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.repeating_conformances.Parent2 ") }
            let __kt = repeating_conformances_Parent2_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            repeating_conformances_Parent2_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class Parent3: KotlinRuntime.KotlinBase, ExportedKotlinPackages.repeating_conformances.Barable, ExportedKotlinPackages.repeating_conformances._Barable {
        public init() {
            if Self.self != ExportedKotlinPackages.repeating_conformances.Parent3.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.repeating_conformances.Parent3 ") }
            let __kt = repeating_conformances_Parent3_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            repeating_conformances_Parent3_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class Parent4: KotlinRuntime.KotlinBase, ExportedKotlinPackages.repeating_conformances.Foeble, ExportedKotlinPackages.repeating_conformances._Foeble {
        public init() {
            if Self.self != ExportedKotlinPackages.repeating_conformances.Parent4.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.repeating_conformances.Parent4 ") }
            let __kt = repeating_conformances_Parent4_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            repeating_conformances_Parent4_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class Parent5: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != ExportedKotlinPackages.repeating_conformances.Parent5.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.repeating_conformances.Parent5 ") }
            let __kt = repeating_conformances_Parent5_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            repeating_conformances_Parent5_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
}

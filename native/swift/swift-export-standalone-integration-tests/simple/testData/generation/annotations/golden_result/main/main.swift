@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

@available(*, deprecated, message: "Deprecated")
public typealias deprecatedA = Swift.Void
@available(*, deprecated, message: "Deprecated")
public typealias deprecatedImplicitlyA = Swift.Void
@available(*, unavailable, message: "Obsoleted")
public typealias obsoletedA = Swift.Void
@available(*, deprecated, message: "Deprecated. Replacement: renamed")
public typealias renamedA = Swift.Void
@available(*, unavailable, message: "Obsoleted")
public protocol DeprecatedInterface: KotlinRuntime.KotlinBase {
    func foo() -> Swift.Void
}
public protocol InterfaceWithDeprecatedMembers: KotlinRuntime.KotlinBase {
    @available(*, deprecated, message: "Deprecated")
    func deprecatedWarningFunction() -> Swift.Void
    func regularFunction() -> Swift.Void
}
public protocol NonDeprecatedInterface: KotlinRuntime.KotlinBase {
    func bar() -> Swift.Void
}
@available(*, unavailable, message: "Obsoleted")
public protocol SubDeprecatedInterface: KotlinRuntime.KotlinBase, main.DeprecatedInterface {
    func baz() -> Swift.Void
}
public protocol SwiftInterfaceC: KotlinRuntime.KotlinBase {
    func kotlinFunE(
        _ kotlinParamE: Swift.String
    ) -> Swift.Void
    func swiftFunD(
        swiftParamD: Swift.String
    ) -> Swift.Void
}
@objc(_DeprecatedInterface)
package protocol _DeprecatedInterface {
}
@objc(_InterfaceWithDeprecatedMembers)
package protocol _InterfaceWithDeprecatedMembers {
}
@objc(_NonDeprecatedInterface)
package protocol _NonDeprecatedInterface: main._DeprecatedInterface {
}
@objc(_SubDeprecatedInterface)
package protocol _SubDeprecatedInterface: main._DeprecatedInterface {
}
@objc(_SwiftInterfaceC)
package protocol _SwiftInterfaceC {
}
@_spi(Barnnotation) @_spi(Foonnotation)
public final class Bar: main.Foo {
    @_spi(Barnnotation) @_spi(Foonnotation)
    public override init() {
        let __kt = __root___Bar_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Bar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class ClassWithDeprecatedMembersFromInterface: KotlinRuntime.KotlinBase, main.InterfaceWithDeprecatedMembers, main._InterfaceWithDeprecatedMembers {
    public init() {
        let __kt = __root___ClassWithDeprecatedMembersFromInterface_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___ClassWithDeprecatedMembersFromInterface_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    @available(*, unavailable, message: "Obsoleted")
    public func deprecatedErrorFunction() -> Swift.Void {
        return { ClassWithDeprecatedMembersFromInterface_deprecatedErrorFunction(self.__externalRCRef()); return () }()
    }
    @available(*, deprecated, message: "Deprecated")
    public func deprecatedWarningFunction() -> Swift.Void {
        return { ClassWithDeprecatedMembersFromInterface_deprecatedWarningFunction(self.__externalRCRef()); return () }()
    }
    public func regularFunction() -> Swift.Void {
        return { ClassWithDeprecatedMembersFromInterface_regularFunction(self.__externalRCRef()); return () }()
    }
}
public final class DeprecatedInterfaceWrapper: KotlinRuntime.KotlinBase {
    @available(*, unavailable, message: "Unavailable type(s): main.DeprecatedInterface")
    public var deprecatedInterface: any main.DeprecatedInterface {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: DeprecatedInterfaceWrapper_deprecatedInterface_get(self.__externalRCRef())) as! any main.DeprecatedInterface
        }
    }
    @available(*, unavailable, message: "Unavailable type(s): main.DeprecatedInterface")
    public init(
        deprecatedInterface: any main.DeprecatedInterface
    ) {
        let __kt = __root___DeprecatedInterfaceWrapper_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___DeprecatedInterfaceWrapper_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20main_DeprecatedInterface__(__kt, deprecatedInterface.__externalRCRef()); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
@_spi(Foonnotation)
open class Foo: KotlinRuntime.KotlinBase {
    @_spi(Foonnotation)
    public init() {
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
@_spi(Foonnotation)
public final class FooObject: KotlinRuntime.KotlinBase {
    @_spi(Foonnotation)
    public var objectProperty: Swift.String {
        @_spi(Foonnotation)
        get {
            return FooObject_objectProperty_get(self.__externalRCRef())
        }
    }
    @_spi(Foonnotation)
    public static var shared: main.FooObject {
        @_spi(Foonnotation)
        get {
            return main.FooObject.__createClassWrapper(externalRCRef: __root___FooObject_get())
        }
    }
    private init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    @_spi(Barnnotation) @_spi(Foonnotation)
    public func objectMethod() -> Swift.Void {
        return { FooObject_objectMethod(self.__externalRCRef()); return () }()
    }
}
public final class HiddenInterfaceWrapper: KotlinRuntime.KotlinBase {
    @available(*, unavailable, message: "Declaration uses unsupported types")
    public var hiddenInterface: Swift.Never {
        get {
            fatalError()
        }
    }
    @available(*, unavailable, message: "Declaration uses unsupported types")
    public init(
        hiddenInterface: Swift.Never
    ) {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class ObjCObjectB: KotlinRuntime.KotlinBase {
    public static var shared: main.ObjCObjectB {
        get {
            return main.ObjCObjectB.__createClassWrapper(externalRCRef: __root___KotlinObjectB_get())
        }
    }
    private init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public func kotlinFunC(
        _ objCParamC: Swift.String
    ) -> Swift.Void {
        return { KotlinObjectB_kotlinFunC__TypesOfArguments__Swift_String__(self.__externalRCRef(), objCParamC); return () }()
    }
    public func objCFunB(
        objCParamB: Swift.String
    ) -> Swift.Void {
        return { KotlinObjectB_kotlinFunB__TypesOfArguments__Swift_String__(self.__externalRCRef(), objCParamB); return () }()
    }
}
public final class OptInConstructor: KotlinRuntime.KotlinBase {
    public var name: Swift.String {
        get {
            return OptInConstructor_name_get(self.__externalRCRef())
        }
    }
    public init() {
        let __kt = __root___OptInConstructor_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___OptInConstructor_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    public init(
        name: Swift.String
    ) {
        let __kt = __root___OptInConstructor_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___OptInConstructor_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(__kt, name); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class PublicClassImplDeprecatedInterface: KotlinRuntime.KotlinBase, main._DeprecatedInterface {
    public init() {
        let __kt = __root___PublicClassImplDeprecatedInterface_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___PublicClassImplDeprecatedInterface_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public func foo() -> Swift.Void {
        return { PublicClassImplDeprecatedInterface_foo(self.__externalRCRef()); return () }()
    }
}
open class PublicClassImplHiddenInterface: KotlinRuntime.KotlinBase {
    public init() {
        let __kt = __root___PublicClassImplHiddenInterface_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___PublicClassImplHiddenInterface_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    open func bar() -> Swift.Void {
        return { PublicClassImplHiddenInterface_bar(self.__externalRCRef()); return () }()
    }
    open func foo() -> Swift.Void {
        return { PublicClassImplHiddenInterface_foo(self.__externalRCRef()); return () }()
    }
}
@available(*, unavailable, message: "Obsoleted")
public final class PublicDeprecatedClassImplDeprecatedInterface: KotlinRuntime.KotlinBase, main.DeprecatedInterface, main._DeprecatedInterface {
    public init() {
        let __kt = __root___PublicDeprecatedClassImplDeprecatedInterface_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___PublicDeprecatedClassImplDeprecatedInterface_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public func foo() -> Swift.Void {
        return { PublicDeprecatedClassImplDeprecatedInterface_foo(self.__externalRCRef()); return () }()
    }
}
public final class PublicSubClassImplHiddenInterface: main.PublicClassImplHiddenInterface {
    public override init() {
        let __kt = __root___PublicSubClassImplHiddenInterface_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___PublicSubClassImplHiddenInterface_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public override func foo() -> Swift.Void {
        return { PublicSubClassImplHiddenInterface_foo(self.__externalRCRef()); return () }()
    }
}
public final class SwiftClassA: KotlinRuntime.KotlinBase {
    public final class ObjCSubClassC: KotlinRuntime.KotlinBase {
        public init() {
            let __kt = KotlinClassA_KotlinSubClassC_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { KotlinClassA_KotlinSubClassC_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class SwiftSubClassA: KotlinRuntime.KotlinBase {
        public init() {
            let __kt = KotlinClassA_KotlinSubClassA_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { KotlinClassA_KotlinSubClassA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class SwiftSubClassB: KotlinRuntime.KotlinBase {
        public init() {
            let __kt = KotlinClassA_KotlinSubClassB_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { KotlinClassA_KotlinSubClassB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class SwiftSubClassD: KotlinRuntime.KotlinBase {
        public init() {
            let __kt = KotlinClassA_KotlinSubClassD_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { KotlinClassA_KotlinSubClassD_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public var swiftPropA: Swift.String {
        get {
            return KotlinClassA_kotlinPropA_get(self.__externalRCRef())
        }
    }
    public var swiftPropB: Swift.String {
        get {
            return KotlinClassA_kotlinPropB_get(self.__externalRCRef())
        }
        set {
            return { KotlinClassA_kotlinPropB_set__TypesOfArguments__Swift_String__(self.__externalRCRef(), newValue); return () }()
        }
    }
    public init() {
        let __kt = __root___KotlinClassA_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___KotlinClassA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public func swiftFunA(
        swiftParamA: Swift.String
    ) -> Swift.Void {
        return { KotlinClassA_kotlinFunA__TypesOfArguments__Swift_String__(self.__externalRCRef(), swiftParamA); return () }()
    }
}
public final class WithCompanion: KotlinRuntime.KotlinBase {
    @_spi(Foonnotation)
    public final class Companion: KotlinRuntime.KotlinBase {
        @_spi(Foonnotation)
        public static var shared: main.WithCompanion.Companion {
            @_spi(Foonnotation)
            get {
                return main.WithCompanion.Companion.__createClassWrapper(externalRCRef: WithCompanion_Companion_get())
            }
        }
        private init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @_spi(Foonnotation)
        public func companionMethod() -> Swift.Void {
            return { WithCompanion_Companion_companionMethod(self.__externalRCRef()); return () }()
        }
    }
    public init() {
        let __kt = __root___WithCompanion_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___WithCompanion_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class deprecatedChildT: main.deprecatedT {
    public var deprecationFurtherReinforcedV: Swift.Void {
        get {
            return { deprecatedChildT_deprecationFurtherReinforcedV_get(self.__externalRCRef()); return () }()
        }
    }
    public var deprecationReinforcedV: Swift.Void {
        get {
            return { deprecatedChildT_deprecationReinforcedV_get(self.__externalRCRef()); return () }()
        }
    }
    public override var deprecationRestatedV: Swift.Void {
        get {
            return { deprecatedChildT_deprecationRestatedV_get(self.__externalRCRef()); return () }()
        }
    }
    public override init() {
        let __kt = __root___deprecatedChildT_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___deprecatedChildT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public func deprecationFurtherReinforcedF() -> Swift.Void {
        return { deprecatedChildT_deprecationFurtherReinforcedF(self.__externalRCRef()); return () }()
    }
    public func deprecationReinforcedF() -> Swift.Void {
        return { deprecatedChildT_deprecationReinforcedF(self.__externalRCRef()); return () }()
    }
    public override func deprecationRestatedF() -> Swift.Void {
        return { deprecatedChildT_deprecationRestatedF(self.__externalRCRef()); return () }()
    }
}
@available(*, deprecated, message: "Deprecated")
open class deprecatedT: KotlinRuntime.KotlinBase {
    open class deprecationInheritedT: KotlinRuntime.KotlinBase {
        public init() {
            let __kt = deprecatedT_deprecationInheritedT_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { deprecatedT_deprecationInheritedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @available(*, unavailable, message: "Obsoleted")
    open class deprecationReinforcedT: KotlinRuntime.KotlinBase {
        public init() {
            let __kt = deprecatedT_deprecationReinforcedT_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { deprecatedT_deprecationReinforcedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @available(*, deprecated, message: "Deprecated")
    open class deprecationRestatedT: KotlinRuntime.KotlinBase {
        public init() {
            let __kt = deprecatedT_deprecationRestatedT_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { deprecatedT_deprecationRestatedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open var deprecationInheritedV: Swift.Void {
        get {
            return { deprecatedT_deprecationInheritedV_get(self.__externalRCRef()); return () }()
        }
    }
    @available(*, unavailable, message: "Obsoleted")
    open var deprecationReinforcedV: Swift.Void {
        get {
            return { deprecatedT_deprecationReinforcedV_get(self.__externalRCRef()); return () }()
        }
    }
    @available(*, deprecated, message: "Deprecated")
    open var deprecationRestatedV: Swift.Void {
        get {
            return { deprecatedT_deprecationRestatedV_get(self.__externalRCRef()); return () }()
        }
    }
    @available(*, deprecated, message: "Deprecated")
    public init() {
        let __kt = __root___deprecatedT_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___deprecatedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    open func deprecationInheritedF() -> Swift.Void {
        return { deprecatedT_deprecationInheritedF(self.__externalRCRef()); return () }()
    }
    @available(*, unavailable, message: "Obsoleted")
    open func deprecationReinforcedF() -> Swift.Void {
        return { deprecatedT_deprecationReinforcedF(self.__externalRCRef()); return () }()
    }
    @available(*, deprecated, message: "Deprecated")
    open func deprecationRestatedF() -> Swift.Void {
        return { deprecatedT_deprecationRestatedF(self.__externalRCRef()); return () }()
    }
}
public final class normalChildT: main.normalT {
    @available(*, deprecated, message: "Deprecated")
    public override var deprecatedInFutureP: Swift.Int32 {
        @available(*, deprecated, message: "Deprecated")
        get {
            return normalChildT_deprecatedInFutureP_get(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Deprecated")
        set {
            return { normalChildT_deprecatedInFutureP_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue); return () }()
        }
    }
    @available(*, deprecated, message: "Deprecated")
    public override var deprecatedInFutureV: Swift.Void {
        get {
            return { normalChildT_deprecatedInFutureV_get(self.__externalRCRef()); return () }()
        }
    }
    public override var deprecatedP: Swift.Int32 {
        @available(*, deprecated, message: "Deprecated")
        get {
            return normalChildT_deprecatedP_get(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Deprecated")
        set {
            return { normalChildT_deprecatedP_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue); return () }()
        }
    }
    public override var deprecatedV: Swift.Void {
        get {
            return { normalChildT_deprecatedV_get(self.__externalRCRef()); return () }()
        }
    }
    public override var normalV: Swift.Void {
        get {
            return { normalChildT_normalV_get(self.__externalRCRef()); return () }()
        }
    }
    @available(*, unavailable, message: "Deprecated")
    public override var obsoletedInFutureP: Swift.Int32 {
        @available(*, unavailable, message: "Deprecated")
        get {
            return normalChildT_obsoletedInFutureP_get(self.__externalRCRef())
        }
        @available(*, unavailable, message: "Deprecated")
        set {
            return { normalChildT_obsoletedInFutureP_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue); return () }()
        }
    }
    @available(*, unavailable, message: "Obsoleted")
    public override var obsoletedInFutureV: Swift.Void {
        get {
            return { normalChildT_obsoletedInFutureV_get(self.__externalRCRef()); return () }()
        }
    }
    public override var obsoletedP: Swift.Int32 {
        @available(*, unavailable, message: "Obsoleted")
        get {
            return normalChildT_obsoletedP_get(self.__externalRCRef())
        }
        @available(*, unavailable, message: "Obsoleted")
        set {
            return { normalChildT_obsoletedP_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue); return () }()
        }
    }
    public var obsoletedV: Swift.Void {
        get {
            return { normalChildT_obsoletedV_get(self.__externalRCRef()); return () }()
        }
    }
    public var removedV: Swift.Void {
        get {
            return { normalChildT_removedV_get(self.__externalRCRef()); return () }()
        }
    }
    public override init() {
        let __kt = __root___normalChildT_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___normalChildT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public override func deprecatedF() -> Swift.Void {
        return { normalChildT_deprecatedF(self.__externalRCRef()); return () }()
    }
    @available(*, deprecated, message: "Deprecated")
    public override func deprecatedInFutureF() -> Swift.Void {
        return { normalChildT_deprecatedInFutureF(self.__externalRCRef()); return () }()
    }
    public override func normalF() -> Swift.Void {
        return { normalChildT_normalF(self.__externalRCRef()); return () }()
    }
    public func obsoletedF() -> Swift.Void {
        return { normalChildT_obsoletedF(self.__externalRCRef()); return () }()
    }
    @available(*, unavailable, message: "Obsoleted")
    public override func obsoletedInFutureF() -> Swift.Void {
        return { normalChildT_obsoletedInFutureF(self.__externalRCRef()); return () }()
    }
    public func removedF() -> Swift.Void {
        return { normalChildT_removedF(self.__externalRCRef()); return () }()
    }
}
open class normalT: KotlinRuntime.KotlinBase {
    @available(*, deprecated, message: "Deprecated")
    open class deprecatedT: KotlinRuntime.KotlinBase {
        @available(*, deprecated, message: "Deprecated")
        public init(
            deprecated: Swift.Int32
        ) {
            let __kt = normalT_deprecatedT_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { normalT_deprecatedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt, deprecated); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class normalT: KotlinRuntime.KotlinBase {
        public init() {
            let __kt = normalT_normalT_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { normalT_normalT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @available(*, unavailable, message: "Obsoleted")
    open class obsoletedT: KotlinRuntime.KotlinBase {
        @available(*, unavailable, message: "Obsoleted")
        public init(
            obsoleted: Swift.Float
        ) {
            let __kt = normalT_obsoletedT_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { normalT_obsoletedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Float__(__kt, obsoleted); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open var deprecatedInFutureP: Swift.Int32 {
        get {
            return normalT_deprecatedInFutureP_get(self.__externalRCRef())
        }
        set {
            return { normalT_deprecatedInFutureP_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue); return () }()
        }
    }
    open var deprecatedInFutureV: Swift.Void {
        get {
            return { normalT_deprecatedInFutureV_get(self.__externalRCRef()); return () }()
        }
    }
    open var deprecatedP: Swift.Int32 {
        @available(*, deprecated, message: "Deprecated")
        get {
            return normalT_deprecatedP_get(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Deprecated")
        set {
            return { normalT_deprecatedP_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue); return () }()
        }
    }
    @available(*, deprecated, message: "Deprecated")
    open var deprecatedV: Swift.Void {
        get {
            return { normalT_deprecatedV_get(self.__externalRCRef()); return () }()
        }
    }
    open var normalP: Swift.Int32 {
        get {
            return normalT_normalP_get(self.__externalRCRef())
        }
        set {
            return { normalT_normalP_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue); return () }()
        }
    }
    open var normalV: Swift.Void {
        get {
            return { normalT_normalV_get(self.__externalRCRef()); return () }()
        }
    }
    open var obsoletedInFutureP: Swift.Int32 {
        get {
            return normalT_obsoletedInFutureP_get(self.__externalRCRef())
        }
        set {
            return { normalT_obsoletedInFutureP_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue); return () }()
        }
    }
    open var obsoletedInFutureV: Swift.Void {
        get {
            return { normalT_obsoletedInFutureV_get(self.__externalRCRef()); return () }()
        }
    }
    open var obsoletedP: Swift.Int32 {
        @available(*, unavailable, message: "Obsoleted")
        get {
            return normalT_obsoletedP_get(self.__externalRCRef())
        }
        @available(*, unavailable, message: "Obsoleted")
        set {
            return { normalT_obsoletedP_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue); return () }()
        }
    }
    @available(*, unavailable, message: "Obsoleted")
    open var obsoletedV: Swift.Void {
        get {
            return { normalT_obsoletedV_get(self.__externalRCRef()); return () }()
        }
    }
    open var removedInFutureP: Swift.Int32 {
        get {
            return normalT_removedInFutureP_get(self.__externalRCRef())
        }
        set {
            return { normalT_removedInFutureP_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue); return () }()
        }
    }
    open var removedInFutureV: Swift.Void {
        get {
            return { normalT_removedInFutureV_get(self.__externalRCRef()); return () }()
        }
    }
    public init() {
        let __kt = __root___normalT_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___normalT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    @available(*, deprecated, message: "Deprecated")
    open func deprecatedF() -> Swift.Void {
        return { normalT_deprecatedF(self.__externalRCRef()); return () }()
    }
    open func deprecatedInFutureF() -> Swift.Void {
        return { normalT_deprecatedInFutureF(self.__externalRCRef()); return () }()
    }
    open func normalF() -> Swift.Void {
        return { normalT_normalF(self.__externalRCRef()); return () }()
    }
    @available(*, unavailable, message: "Obsoleted")
    open func obsoletedF() -> Swift.Void {
        return { normalT_obsoletedF(self.__externalRCRef()); return () }()
    }
    open func obsoletedInFutureF() -> Swift.Void {
        return { normalT_obsoletedInFutureF(self.__externalRCRef()); return () }()
    }
    open func removedInFutureF() -> Swift.Void {
        return { normalT_removedInFutureF(self.__externalRCRef()); return () }()
    }
}
@available(*, unavailable, message: "Unavailable type(s): main.obsoletedT")
open class obsoletedChildT: main.obsoletedT {
    open var deprecationReinforcedV: Swift.Void {
        get {
            return { obsoletedChildT_deprecationReinforcedV_get(self.__externalRCRef()); return () }()
        }
    }
    open override var deprecationRelaxedV: Swift.Void {
        get {
            return { obsoletedChildT_deprecationRelaxedV_get(self.__externalRCRef()); return () }()
        }
    }
    open var deprecationRestatedV: Swift.Void {
        get {
            return { obsoletedChildT_deprecationRestatedV_get(self.__externalRCRef()); return () }()
        }
    }
    public init() {
        let __kt = __root___obsoletedChildT_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___obsoletedChildT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    open func deprecationReinforcedF() -> Swift.Void {
        return { obsoletedChildT_deprecationReinforcedF(self.__externalRCRef()); return () }()
    }
    open override func deprecationRelaxedF() -> Swift.Void {
        return { obsoletedChildT_deprecationRelaxedF(self.__externalRCRef()); return () }()
    }
    open func deprecationRestatedF() -> Swift.Void {
        return { obsoletedChildT_deprecationRestatedF(self.__externalRCRef()); return () }()
    }
}
@available(*, unavailable, message: "Obsoleted")
open class obsoletedT: KotlinRuntime.KotlinBase {
    open class deprecationInheritedT: KotlinRuntime.KotlinBase {
        public init() {
            let __kt = obsoletedT_deprecationInheritedT_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { obsoletedT_deprecationInheritedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @available(*, deprecated, message: "Deprecated")
    open class deprecationRelaxedT: KotlinRuntime.KotlinBase {
        public init() {
            let __kt = obsoletedT_deprecationRelaxedT_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { obsoletedT_deprecationRelaxedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @available(*, unavailable, message: "Obsoleted")
    open class deprecationRestatedT: KotlinRuntime.KotlinBase {
        public init() {
            let __kt = obsoletedT_deprecationRestatedT_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { obsoletedT_deprecationRestatedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open var deprecationInheritedV: Swift.Void {
        get {
            return { obsoletedT_deprecationInheritedV_get(self.__externalRCRef()); return () }()
        }
    }
    @available(*, deprecated, message: "Deprecated")
    open var deprecationRelaxedV: Swift.Void {
        get {
            return { obsoletedT_deprecationRelaxedV_get(self.__externalRCRef()); return () }()
        }
    }
    @available(*, unavailable, message: "Obsoleted")
    open var deprecationRestatedV: Swift.Void {
        get {
            return { obsoletedT_deprecationRestatedV_get(self.__externalRCRef()); return () }()
        }
    }
    @available(*, unavailable, message: "Deprecated")
    public init() {
        let __kt = __root___obsoletedT_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___obsoletedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    open func deprecationInheritedF() -> Swift.Void {
        return { obsoletedT_deprecationInheritedF(self.__externalRCRef()); return () }()
    }
    @available(*, deprecated, message: "Deprecated")
    open func deprecationRelaxedF() -> Swift.Void {
        return { obsoletedT_deprecationRelaxedF(self.__externalRCRef()); return () }()
    }
    @available(*, unavailable, message: "Obsoleted")
    open func deprecationRestatedF() -> Swift.Void {
        return { obsoletedT_deprecationRestatedF(self.__externalRCRef()); return () }()
    }
}
@available(*, deprecated, message: "Deprecated. Replacement: renamed")
public final class renamedT: KotlinRuntime.KotlinBase {
    public init() {
        let __kt = __root___renamedT_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___renamedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public var MESSAGE: Swift.String {
    get {
        return __root___MESSAGE_get()
    }
}
@_spi(Barnnotation) @_spi(Foonnotation)
public var barProperty: main.Bar {
    @_spi(Barnnotation) @_spi(Foonnotation)
    get {
        return main.Bar.__createClassWrapper(externalRCRef: __root___barProperty_get())
    }
    @_spi(Barnnotation) @_spi(Foonnotation)
    set {
        return { __root___barProperty_set__TypesOfArguments__main_Bar__(newValue.__externalRCRef()); return () }()
    }
}
public var classA: main.SwiftClassA {
    get {
        return main.SwiftClassA.__createClassWrapper(externalRCRef: __root___classA_get())
    }
}
@available(*, unavailable, message: "Unavailable type(s): main.DeprecatedInterface")
public var deprecatedInterfaceProperty: any main.DeprecatedInterface {
    get {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___deprecatedInterfaceProperty_get()) as! any main.DeprecatedInterface
    }
    set {
        return { __root___deprecatedInterfaceProperty_set__TypesOfArguments__anyU20main_DeprecatedInterface__(newValue.__externalRCRef()); return () }()
    }
}
@available(*, deprecated, message: "Deprecated")
public var deprecationInheritedImplicitlyV: Swift.Void {
    get {
        return { __root___deprecationInheritedImplicitlyV_get(); return () }()
    }
}
@available(*, deprecated, message: "Deprecated")
public var deprecationInheritedV: Swift.Void {
    get {
        return { __root___deprecationInheritedV_get(); return () }()
    }
}
@_spi(Foonnotation)
public var fooProperty: main.Foo {
    @_spi(Foonnotation)
    get {
        return main.Foo.__createClassWrapper(externalRCRef: __root___fooProperty_get())
    }
}
@_spi(Foonnotation)
public var fooVal: Swift.String {
    @_spi(Foonnotation)
    get {
        return __root___fooVal_get()
    }
}
@available(*, unavailable, message: "Declaration uses unsupported types")
public var hiddenInterfaceProperty: Swift.Never {
    get {
        fatalError()
    }
    set {
        fatalError()
    }
}
public var interfaceC: any main.SwiftInterfaceC {
    get {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___interfaceC_get()) as! any main.SwiftInterfaceC
    }
}
public var objectB: main.ObjCObjectB {
    get {
        return main.ObjCObjectB.__createClassWrapper(externalRCRef: __root___objectB_get())
    }
}
@available(*, unavailable, message: "Obsoleted")
public var obsoletedV: Swift.Void {
    get {
        return { __root___obsoletedV_get(); return () }()
    }
}
public var publicClassImplDeprecatedInterfaceProperty: main.PublicClassImplDeprecatedInterface {
    get {
        return main.PublicClassImplDeprecatedInterface.__createClassWrapper(externalRCRef: __root___publicClassImplDeprecatedInterfaceProperty_get())
    }
    set {
        return { __root___publicClassImplDeprecatedInterfaceProperty_set__TypesOfArguments__main_PublicClassImplDeprecatedInterface__(newValue.__externalRCRef()); return () }()
    }
}
public var publicClassImplHiddenInterfaceProperty: main.PublicClassImplHiddenInterface {
    get {
        return main.PublicClassImplHiddenInterface.__createClassWrapper(externalRCRef: __root___publicClassImplHiddenInterfaceProperty_get())
    }
    set {
        return { __root___publicClassImplHiddenInterfaceProperty_set__TypesOfArguments__main_PublicClassImplHiddenInterface__(newValue.__externalRCRef()); return () }()
    }
}
@available(*, deprecated, message: "Deprecated. Replacement: renamed")
public var renamedV: Swift.Void {
    get {
        return { __root___renamedV_get(); return () }()
    }
}
@available(*, unavailable, message: "Unavailable type(s): main.DeprecatedInterface")
public func acceptDeprecatedInterface(
    arg: any main.DeprecatedInterface
) -> Swift.Void {
    return { __root___acceptDeprecatedInterface__TypesOfArguments__anyU20main_DeprecatedInterface__(arg.__externalRCRef()); return () }()
}
@available(*, unavailable, message: "Declaration uses unsupported types")
public func acceptHiddenChildT(
    arg: Swift.Never
) -> Swift.Void {
    fatalError()
}
@available(*, unavailable, message: "Declaration uses unsupported types")
public func acceptHiddenInterface(
    arg: Swift.Never
) -> Swift.Void {
    fatalError()
}
@available(*, unavailable, message: "Declaration uses unsupported types")
public func acceptHiddenT(
    arg: Swift.Never
) -> Swift.Void {
    fatalError()
}
public func acceptPublicClassImplDeprecatedInterface(
    arg: main.PublicClassImplDeprecatedInterface
) -> Swift.Void {
    return { __root___acceptPublicClassImplDeprecatedInterface__TypesOfArguments__main_PublicClassImplDeprecatedInterface__(arg.__externalRCRef()); return () }()
}
public func acceptPublicClassImplHiddenInterface(
    arg: main.PublicClassImplHiddenInterface
) -> Swift.Void {
    return { __root___acceptPublicClassImplHiddenInterface__TypesOfArguments__main_PublicClassImplHiddenInterface__(arg.__externalRCRef()); return () }()
}
@_spi(Barnnotation) @_spi(Foonnotation)
public func bar() -> main.Bar {
    return main.Bar.__createClassWrapper(externalRCRef: __root___bar())
}
@available(*, deprecated, message: "message")
public func constMessage() -> Swift.Never {
    return { __root___constMessage(); fatalError() }()
}
@available(*, deprecated, message: "Deprecated")
public func deprecatedF() -> Swift.Void {
    return { __root___deprecatedF(); return () }()
}
@available(*, deprecated, message: "Deprecated")
public func deprecatedImplicitlyF() -> Swift.Void {
    return { __root___deprecatedImplicitlyF(); return () }()
}
public func expressionOptIn() -> Swift.Void {
    return { __root___expressionOptIn(); return () }()
}
@_spi(Foonnotation)
public func foo() -> main.Foo {
    return main.Foo.__createClassWrapper(externalRCRef: __root___foo())
}
@available(*, deprecated, message: "->message<-")
public func formattedMessage() -> Swift.Never {
    return { __root___formattedMessage(); fatalError() }()
}
@available(*, unavailable, message: "Unavailable type(s): main.DeprecatedInterface")
public func getDeprecatedInterfacePropertyWithContext(
    _ context: main.normalT
) -> any main.DeprecatedInterface {
    let (_0) = context
    return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___deprecatedInterfacePropertyWithContext_get__TypesOfArgumentsC1__main_normalT__(_0.__externalRCRef())) as! any main.DeprecatedInterface
}
public func localDeclarations() -> Swift.Void {
    return { __root___localDeclarations(); return () }()
}
@available(*, deprecated, message: """

    line1
    message
    line2

""")
public func multilineFormattedMessage() -> Swift.Never {
    return { __root___multilineFormattedMessage(); fatalError() }()
}
@available(*, deprecated, message: """

    line1
    line2

""")
public func multilineMessage() -> Swift.Never {
    return { __root___multilineMessage(); fatalError() }()
}
@available(*, unavailable, message: "Obsoleted")
public func obsoletedF() -> Swift.Void {
    return { __root___obsoletedF(); return () }()
}
@available(*, deprecated, message: ". Replacement: something")
public func renamed(
    x: Swift.Int32,
    y: Swift.Float
) -> Swift.Never {
    return { __root___renamed__TypesOfArguments__Swift_Int32_Swift_Float__(x, y); fatalError() }()
}
@available(*, deprecated, message: "Deprecated. Replacement: renamed")
public func renamedF() -> Swift.Void {
    return { __root___renamedF(); return () }()
}
@available(*, deprecated, message: ". Replacement: something.else")
public func renamedQualified(
    x: Swift.Int32,
    y: Swift.Float
) -> Swift.Never {
    return { __root___renamedQualified__TypesOfArguments__Swift_Int32_Swift_Float__(x, y); fatalError() }()
}
@available(*, deprecated, message: ". Replacement: something.else(x, y)")
public func renamedQualifiedWithArguments(
    x: Swift.Int32,
    y: Swift.Float
) -> Swift.Never {
    return { __root___renamedQualifiedWithArguments__TypesOfArguments__Swift_Int32_Swift_Float__(x, y); fatalError() }()
}
@available(*, deprecated, message: ". Replacement: something(y, x)")
public func renamedWithArguments(
    x: Swift.Int32,
    y: Swift.Float
) -> Swift.Never {
    return { __root___renamedWithArguments__TypesOfArguments__Swift_Int32_Swift_Float__(x, y); fatalError() }()
}
public func returnClassA(
    value: main.SwiftClassA
) -> main.SwiftClassA {
    return main.SwiftClassA.__createClassWrapper(externalRCRef: __root___returnClassA__TypesOfArguments__main_SwiftClassA__(value.__externalRCRef()))
}
@available(*, unavailable, message: "Unavailable type(s): main.DeprecatedInterface")
public func returnDeprecatedInterface() -> any main.DeprecatedInterface {
    return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___returnDeprecatedInterface()) as! any main.DeprecatedInterface
}
@available(*, unavailable, message: "Declaration uses unsupported types")
public func returnHiddenChildT() -> Swift.Never {
    fatalError()
}
@available(*, unavailable, message: "Declaration uses unsupported types")
public func returnHiddenInterface() -> Swift.Never {
    fatalError()
}
@available(*, unavailable, message: "Declaration uses unsupported types")
public func returnHiddenT() -> Swift.Never {
    fatalError()
}
public func returnInterfaceC(
    value: any main.SwiftInterfaceC
) -> any main.SwiftInterfaceC {
    return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___returnInterfaceC__TypesOfArguments__anyU20main_SwiftInterfaceC__(value.__externalRCRef())) as! any main.SwiftInterfaceC
}
public func returnObjectB(
    value: main.ObjCObjectB
) -> main.ObjCObjectB {
    return main.ObjCObjectB.__createClassWrapper(externalRCRef: __root___returnObjectB__TypesOfArguments__main_ObjCObjectB__(value.__externalRCRef()))
}
public func returnPublicClassImplDeprecatedInterface() -> main.PublicClassImplDeprecatedInterface {
    return main.PublicClassImplDeprecatedInterface.__createClassWrapper(externalRCRef: __root___returnPublicClassImplDeprecatedInterface())
}
public func returnPublicClassImplHiddenInterface() -> main.PublicClassImplHiddenInterface {
    return main.PublicClassImplHiddenInterface.__createClassWrapper(externalRCRef: __root___returnPublicClassImplHiddenInterface())
}
@available(*, unavailable, message: "Unavailable type(s): main.DeprecatedInterface")
public func setDeprecatedInterfacePropertyWithContext(
    _ context: main.normalT,
    value: any main.DeprecatedInterface
) -> Swift.Void {
    let (_1) = context
    return { __root___deprecatedInterfacePropertyWithContext_set__TypesOfArgumentsC1__anyU20main_DeprecatedInterface_main_normalT__(value.__externalRCRef(), _1.__externalRCRef()); return () }()
}
@available(*, deprecated, message: ". Replacement: unrenamed")
public func unrenamed() -> Swift.Never {
    return { __root___unrenamed(); fatalError() }()
}
@available(*, unavailable, message: "Unavailable type(s): main.DeprecatedInterface")
extension main.DeprecatedInterface where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func foo() -> Swift.Void {
        return { DeprecatedInterface_foo(self.__externalRCRef()); return () }()
    }
}
@available(*, unavailable, message: "Unavailable type(s): main.DeprecatedInterface")
extension main.DeprecatedInterface {
}
extension main.InterfaceWithDeprecatedMembers where Self : KotlinRuntimeSupport._KotlinBridgeable {
    @available(*, deprecated, message: "Deprecated")
    public func deprecatedWarningFunction() -> Swift.Void {
        return { InterfaceWithDeprecatedMembers_deprecatedWarningFunction(self.__externalRCRef()); return () }()
    }
    public func regularFunction() -> Swift.Void {
        return { InterfaceWithDeprecatedMembers_regularFunction(self.__externalRCRef()); return () }()
    }
}
extension main.InterfaceWithDeprecatedMembers {
}
extension main.NonDeprecatedInterface where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func bar() -> Swift.Void {
        return { NonDeprecatedInterface_bar(self.__externalRCRef()); return () }()
    }
}
extension main.NonDeprecatedInterface {
}
@available(*, unavailable, message: "Unavailable type(s): main.SubDeprecatedInterface")
extension main.SubDeprecatedInterface where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func baz() -> Swift.Void {
        return { SubDeprecatedInterface_baz(self.__externalRCRef()); return () }()
    }
}
@available(*, unavailable, message: "Unavailable type(s): main.SubDeprecatedInterface")
extension main.SubDeprecatedInterface {
}
extension main.SwiftInterfaceC where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func kotlinFunE(
        _ kotlinParamE: Swift.String
    ) -> Swift.Void {
        return { KotlinInterfaceC_kotlinFunE__TypesOfArguments__Swift_String__(self.__externalRCRef(), kotlinParamE); return () }()
    }
    public func swiftFunD(
        swiftParamD: Swift.String
    ) -> Swift.Void {
        return { KotlinInterfaceC_kotlinFunD__TypesOfArguments__Swift_String__(self.__externalRCRef(), swiftParamD); return () }()
    }
}
extension main.SwiftInterfaceC {
}
extension KotlinRuntimeSupport._KotlinExistential: main.InterfaceWithDeprecatedMembers where Wrapped : main._InterfaceWithDeprecatedMembers {
}
@available(*, unavailable, message: "Unavailable type(s): main.DeprecatedInterface")
extension KotlinRuntimeSupport._KotlinExistential: main.DeprecatedInterface where Wrapped : main._DeprecatedInterface {
}
extension KotlinRuntimeSupport._KotlinExistential: main.NonDeprecatedInterface where Wrapped : main._NonDeprecatedInterface {
}
@available(*, unavailable, message: "Unavailable type(s): main.SubDeprecatedInterface")
extension KotlinRuntimeSupport._KotlinExistential: main.SubDeprecatedInterface where Wrapped : main._SubDeprecatedInterface {
}
extension KotlinRuntimeSupport._KotlinExistential: main.SwiftInterfaceC where Wrapped : main._SwiftInterfaceC {
}
@_cdecl("PublicClassImplHiddenInterface_bar__reverse_swift")
public func PublicClassImplHiddenInterface_bar__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = main.PublicClassImplHiddenInterface.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.bar()
    return { _result; return true }()
}

@_cdecl("PublicClassImplHiddenInterface_foo__reverse_swift")
public func PublicClassImplHiddenInterface_foo__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = main.PublicClassImplHiddenInterface.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.foo()
    return { _result; return true }()
}

@available(*, deprecated, message: "Deprecated")
@_cdecl("deprecatedT_deprecationInheritedF__reverse_swift")
public func deprecatedT_deprecationInheritedF__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = main.deprecatedT.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.deprecationInheritedF()
    return { _result; return true }()
}

@available(*, deprecated, message: "Deprecated")
@_cdecl("deprecatedT_deprecationRestatedF__reverse_swift")
public func deprecatedT_deprecationRestatedF__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = main.deprecatedT.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.deprecationRestatedF()
    return { _result; return true }()
}

@available(*, deprecated, message: "Deprecated")
@_cdecl("normalT_deprecatedF__reverse_swift")
public func normalT_deprecatedF__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = main.normalT.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.deprecatedF()
    return { _result; return true }()
}

@_cdecl("normalT_deprecatedInFutureF__reverse_swift")
public func normalT_deprecatedInFutureF__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = main.normalT.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.deprecatedInFutureF()
    return { _result; return true }()
}

@_cdecl("normalT_normalF__reverse_swift")
public func normalT_normalF__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = main.normalT.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.normalF()
    return { _result; return true }()
}

@_cdecl("normalT_obsoletedInFutureF__reverse_swift")
public func normalT_obsoletedInFutureF__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = main.normalT.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.obsoletedInFutureF()
    return { _result; return true }()
}

@_cdecl("normalT_removedInFutureF__reverse_swift")
public func normalT_removedInFutureF__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = main.normalT.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.removedInFutureF()
    return { _result; return true }()
}

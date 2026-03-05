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
public protocol SwiftInterfaceC: KotlinRuntime.KotlinBase {
    func kotlinFunE(
        _ kotlinParamE: Swift.String
    ) -> Swift.Void
    func swiftFunD(
        swiftParamD: Swift.String
    ) -> Swift.Void
}
@objc(_SwiftInterfaceC)
package protocol _SwiftInterfaceC {
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
public final class SwiftClassA: KotlinRuntime.KotlinBase {
    public final class ObjCSubClassC: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != main.SwiftClassA.ObjCSubClassC.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.SwiftClassA.ObjCSubClassC ") }
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
            if Self.self != main.SwiftClassA.SwiftSubClassA.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.SwiftClassA.SwiftSubClassA ") }
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
            if Self.self != main.SwiftClassA.SwiftSubClassB.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.SwiftClassA.SwiftSubClassB ") }
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
            if Self.self != main.SwiftClassA.SwiftSubClassD.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.SwiftClassA.SwiftSubClassD ") }
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
        if Self.self != main.SwiftClassA.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.SwiftClassA ") }
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
        if Self.self != main.deprecatedChildT.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.deprecatedChildT ") }
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
            if Self.self != main.deprecatedT.deprecationInheritedT.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.deprecatedT.deprecationInheritedT ") }
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
    @available(*, deprecated, message: "Deprecated")
    open class deprecationRestatedT: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != main.deprecatedT.deprecationRestatedT.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.deprecatedT.deprecationRestatedT ") }
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
        if Self.self != main.deprecatedT.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.deprecatedT ") }
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
        if Self.self != main.normalChildT.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.normalChildT ") }
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
            if Self.self != main.normalT.deprecatedT.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.normalT.deprecatedT ") }
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
            if Self.self != main.normalT.normalT.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.normalT.normalT ") }
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
        if Self.self != main.normalT.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.normalT ") }
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
@available(*, deprecated, message: "Deprecated. Replacement: renamed")
public final class renamedT: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != main.renamedT.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.renamedT ") }
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
public var classA: main.SwiftClassA {
    get {
        return main.SwiftClassA.__createClassWrapper(externalRCRef: __root___classA_get())
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
@available(*, deprecated, message: "Deprecated. Replacement: renamed")
public var renamedV: Swift.Void {
    get {
        return { __root___renamedV_get(); return () }()
    }
}
@available(*, deprecated, message: "message")
public func constMessage() -> Swift.Never {
    return __root___constMessage()
}
@available(*, deprecated, message: "Deprecated")
public func deprecatedF() -> Swift.Void {
    return { __root___deprecatedF(); return () }()
}
@available(*, deprecated, message: "Deprecated")
public func deprecatedImplicitlyF() -> Swift.Void {
    return { __root___deprecatedImplicitlyF(); return () }()
}
@available(*, deprecated, message: "->message<-")
public func formattedMessage() -> Swift.Never {
    return __root___formattedMessage()
}
@available(*, deprecated, message: """

    line1
    message
    line2

""")
public func multilineFormattedMessage() -> Swift.Never {
    return __root___multilineFormattedMessage()
}
@available(*, deprecated, message: """

    line1
    line2

""")
public func multilineMessage() -> Swift.Never {
    return __root___multilineMessage()
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
    return __root___renamed__TypesOfArguments__Swift_Int32_Swift_Float__(x, y)
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
    return __root___renamedQualified__TypesOfArguments__Swift_Int32_Swift_Float__(x, y)
}
@available(*, deprecated, message: ". Replacement: something.else(x, y)")
public func renamedQualifiedWithArguments(
    x: Swift.Int32,
    y: Swift.Float
) -> Swift.Never {
    return __root___renamedQualifiedWithArguments__TypesOfArguments__Swift_Int32_Swift_Float__(x, y)
}
@available(*, deprecated, message: ". Replacement: something(y, x)")
public func renamedWithArguments(
    x: Swift.Int32,
    y: Swift.Float
) -> Swift.Never {
    return __root___renamedWithArguments__TypesOfArguments__Swift_Int32_Swift_Float__(x, y)
}
public func returnClassA(
    value: main.SwiftClassA
) -> main.SwiftClassA {
    return main.SwiftClassA.__createClassWrapper(externalRCRef: __root___returnClassA__TypesOfArguments__main_SwiftClassA__(value.__externalRCRef()))
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
@available(*, deprecated, message: ". Replacement: unrenamed")
public func unrenamed() -> Swift.Never {
    return __root___unrenamed()
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
extension KotlinRuntimeSupport._KotlinExistential: main.SwiftInterfaceC where Wrapped : main._SwiftInterfaceC {
}

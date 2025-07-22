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
@_spi(Barnnotation) @_spi(Foonnotation)
public final class Bar: main.Foo {
    @_spi(Barnnotation) @_spi(Foonnotation)
    public override init() {
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
}
@_spi(Foonnotation)
open class Foo: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    @_spi(Foonnotation)
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
}
@_spi(Foonnotation)
public final class FooObject: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
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
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    @_spi(Barnnotation) @_spi(Foonnotation)
    public func objectMethod() -> Swift.Void {
        return FooObject_objectMethod(self.__externalRCRef())
    }
}
public final class OptInConstructor: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public var name: Swift.String {
        get {
            return OptInConstructor_name_get(self.__externalRCRef())
        }
    }
    public init() {
        if Self.self != main.OptInConstructor.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.OptInConstructor ") }
        let __kt = __root___OptInConstructor_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___OptInConstructor_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    public init(
        name: Swift.String
    ) {
        if Self.self != main.OptInConstructor.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.OptInConstructor ") }
        let __kt = __root___OptInConstructor_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___OptInConstructor_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(__kt, name)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class WithCompanion: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    @_spi(Foonnotation)
    public final class Companion: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
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
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        @_spi(Foonnotation)
        public func companionMethod() -> Swift.Void {
            return WithCompanion_Companion_companionMethod(self.__externalRCRef())
        }
    }
    public init() {
        if Self.self != main.WithCompanion.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.WithCompanion ") }
        let __kt = __root___WithCompanion_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___WithCompanion_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class deprecatedChildT: main.deprecatedT {
    public var deprecationFurtherReinforcedV: Swift.Void {
        get {
            return deprecatedChildT_deprecationFurtherReinforcedV_get(self.__externalRCRef())
        }
    }
    public var deprecationReinforcedV: Swift.Void {
        get {
            return deprecatedChildT_deprecationReinforcedV_get(self.__externalRCRef())
        }
    }
    public override var deprecationRestatedV: Swift.Void {
        get {
            return deprecatedChildT_deprecationRestatedV_get(self.__externalRCRef())
        }
    }
    public override init() {
        if Self.self != main.deprecatedChildT.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.deprecatedChildT ") }
        let __kt = __root___deprecatedChildT_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___deprecatedChildT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    public func deprecationFurtherReinforcedF() -> Swift.Void {
        return deprecatedChildT_deprecationFurtherReinforcedF(self.__externalRCRef())
    }
    public func deprecationReinforcedF() -> Swift.Void {
        return deprecatedChildT_deprecationReinforcedF(self.__externalRCRef())
    }
    public override func deprecationRestatedF() -> Swift.Void {
        return deprecatedChildT_deprecationRestatedF(self.__externalRCRef())
    }
}
@available(*, deprecated, message: "Deprecated")
open class deprecatedT: KotlinRuntime.KotlinBase {
    open class deprecationInheritedT: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != main.deprecatedT.deprecationInheritedT.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.deprecatedT.deprecationInheritedT ") }
            let __kt = deprecatedT_deprecationInheritedT_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            deprecatedT_deprecationInheritedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    @available(*, deprecated, message: "Deprecated")
    open class deprecationRestatedT: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != main.deprecatedT.deprecationRestatedT.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.deprecatedT.deprecationRestatedT ") }
            let __kt = deprecatedT_deprecationRestatedT_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            deprecatedT_deprecationRestatedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open var deprecationInheritedV: Swift.Void {
        get {
            return deprecatedT_deprecationInheritedV_get(self.__externalRCRef())
        }
    }
    @available(*, unavailable, message: "Obsoleted")
    open var deprecationReinforcedV: Swift.Void {
        get {
            return deprecatedT_deprecationReinforcedV_get(self.__externalRCRef())
        }
    }
    @available(*, deprecated, message: "Deprecated")
    open var deprecationRestatedV: Swift.Void {
        get {
            return deprecatedT_deprecationRestatedV_get(self.__externalRCRef())
        }
    }
    @available(*, deprecated, message: "Deprecated")
    public init() {
        if Self.self != main.deprecatedT.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.deprecatedT ") }
        let __kt = __root___deprecatedT_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___deprecatedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    open func deprecationInheritedF() -> Swift.Void {
        return deprecatedT_deprecationInheritedF(self.__externalRCRef())
    }
    @available(*, unavailable, message: "Obsoleted")
    open func deprecationReinforcedF() -> Swift.Void {
        return deprecatedT_deprecationReinforcedF(self.__externalRCRef())
    }
    @available(*, deprecated, message: "Deprecated")
    open func deprecationRestatedF() -> Swift.Void {
        return deprecatedT_deprecationRestatedF(self.__externalRCRef())
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
            return normalChildT_deprecatedInFutureP_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue)
        }
    }
    @available(*, deprecated, message: "Deprecated")
    public override var deprecatedInFutureV: Swift.Void {
        get {
            return normalChildT_deprecatedInFutureV_get(self.__externalRCRef())
        }
    }
    public override var deprecatedP: Swift.Int32 {
        @available(*, deprecated, message: "Deprecated")
        get {
            return normalChildT_deprecatedP_get(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Deprecated")
        set {
            return normalChildT_deprecatedP_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue)
        }
    }
    public override var deprecatedV: Swift.Void {
        get {
            return normalChildT_deprecatedV_get(self.__externalRCRef())
        }
    }
    public override var normalV: Swift.Void {
        get {
            return normalChildT_normalV_get(self.__externalRCRef())
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
            return normalChildT_obsoletedInFutureP_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue)
        }
    }
    @available(*, unavailable, message: "Obsoleted")
    public override var obsoletedInFutureV: Swift.Void {
        get {
            return normalChildT_obsoletedInFutureV_get(self.__externalRCRef())
        }
    }
    public override var obsoletedP: Swift.Int32 {
        @available(*, unavailable, message: "Obsoleted")
        get {
            return normalChildT_obsoletedP_get(self.__externalRCRef())
        }
        @available(*, unavailable, message: "Obsoleted")
        set {
            return normalChildT_obsoletedP_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue)
        }
    }
    public var obsoletedV: Swift.Void {
        get {
            return normalChildT_obsoletedV_get(self.__externalRCRef())
        }
    }
    public var removedV: Swift.Void {
        get {
            return normalChildT_removedV_get(self.__externalRCRef())
        }
    }
    public override init() {
        if Self.self != main.normalChildT.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.normalChildT ") }
        let __kt = __root___normalChildT_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___normalChildT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    public override func deprecatedF() -> Swift.Void {
        return normalChildT_deprecatedF(self.__externalRCRef())
    }
    @available(*, deprecated, message: "Deprecated")
    public override func deprecatedInFutureF() -> Swift.Void {
        return normalChildT_deprecatedInFutureF(self.__externalRCRef())
    }
    public override func normalF() -> Swift.Void {
        return normalChildT_normalF(self.__externalRCRef())
    }
    public func obsoletedF() -> Swift.Void {
        return normalChildT_obsoletedF(self.__externalRCRef())
    }
    @available(*, unavailable, message: "Obsoleted")
    public override func obsoletedInFutureF() -> Swift.Void {
        return normalChildT_obsoletedInFutureF(self.__externalRCRef())
    }
    public func removedF() -> Swift.Void {
        return normalChildT_removedF(self.__externalRCRef())
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
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            normalT_deprecatedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt, deprecated)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open class normalT: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != main.normalT.normalT.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.normalT.normalT ") }
            let __kt = normalT_normalT_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            normalT_normalT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    open var deprecatedInFutureP: Swift.Int32 {
        get {
            return normalT_deprecatedInFutureP_get(self.__externalRCRef())
        }
        set {
            return normalT_deprecatedInFutureP_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue)
        }
    }
    open var deprecatedInFutureV: Swift.Void {
        get {
            return normalT_deprecatedInFutureV_get(self.__externalRCRef())
        }
    }
    open var deprecatedP: Swift.Int32 {
        @available(*, deprecated, message: "Deprecated")
        get {
            return normalT_deprecatedP_get(self.__externalRCRef())
        }
        @available(*, deprecated, message: "Deprecated")
        set {
            return normalT_deprecatedP_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue)
        }
    }
    @available(*, deprecated, message: "Deprecated")
    open var deprecatedV: Swift.Void {
        get {
            return normalT_deprecatedV_get(self.__externalRCRef())
        }
    }
    open var normalP: Swift.Int32 {
        get {
            return normalT_normalP_get(self.__externalRCRef())
        }
        set {
            return normalT_normalP_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue)
        }
    }
    open var normalV: Swift.Void {
        get {
            return normalT_normalV_get(self.__externalRCRef())
        }
    }
    open var obsoletedInFutureP: Swift.Int32 {
        get {
            return normalT_obsoletedInFutureP_get(self.__externalRCRef())
        }
        set {
            return normalT_obsoletedInFutureP_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue)
        }
    }
    open var obsoletedInFutureV: Swift.Void {
        get {
            return normalT_obsoletedInFutureV_get(self.__externalRCRef())
        }
    }
    open var obsoletedP: Swift.Int32 {
        @available(*, unavailable, message: "Obsoleted")
        get {
            return normalT_obsoletedP_get(self.__externalRCRef())
        }
        @available(*, unavailable, message: "Obsoleted")
        set {
            return normalT_obsoletedP_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue)
        }
    }
    @available(*, unavailable, message: "Obsoleted")
    open var obsoletedV: Swift.Void {
        get {
            return normalT_obsoletedV_get(self.__externalRCRef())
        }
    }
    open var removedInFutureP: Swift.Int32 {
        get {
            return normalT_removedInFutureP_get(self.__externalRCRef())
        }
        set {
            return normalT_removedInFutureP_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue)
        }
    }
    open var removedInFutureV: Swift.Void {
        get {
            return normalT_removedInFutureV_get(self.__externalRCRef())
        }
    }
    public init() {
        if Self.self != main.normalT.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.normalT ") }
        let __kt = __root___normalT_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___normalT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    @available(*, deprecated, message: "Deprecated")
    open func deprecatedF() -> Swift.Void {
        return normalT_deprecatedF(self.__externalRCRef())
    }
    open func deprecatedInFutureF() -> Swift.Void {
        return normalT_deprecatedInFutureF(self.__externalRCRef())
    }
    open func normalF() -> Swift.Void {
        return normalT_normalF(self.__externalRCRef())
    }
    @available(*, unavailable, message: "Obsoleted")
    open func obsoletedF() -> Swift.Void {
        return normalT_obsoletedF(self.__externalRCRef())
    }
    open func obsoletedInFutureF() -> Swift.Void {
        return normalT_obsoletedInFutureF(self.__externalRCRef())
    }
    open func removedInFutureF() -> Swift.Void {
        return normalT_removedInFutureF(self.__externalRCRef())
    }
}
@available(*, deprecated, message: "Deprecated. Replacement: renamed")
public final class renamedT: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != main.renamedT.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.renamedT ") }
        let __kt = __root___renamedT_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___renamedT_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public var MESSAGE: Swift.String {
    get {
        return __root___MESSAGE_get()
    }
}
@_spi(Barnnotation)
public var barProperty: main.Bar {
    @_spi(Barnnotation)
    get {
        return main.Bar.__createClassWrapper(externalRCRef: __root___barProperty_get())
    }
    @_spi(Barnnotation)
    set {
        return __root___barProperty_set__TypesOfArguments__main_Bar__(newValue.__externalRCRef())
    }
}
@available(*, deprecated, message: "Deprecated")
public var deprecationInheritedImplicitlyV: Swift.Void {
    get {
        return __root___deprecationInheritedImplicitlyV_get()
    }
}
@available(*, deprecated, message: "Deprecated")
public var deprecationInheritedV: Swift.Void {
    get {
        return __root___deprecationInheritedV_get()
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
@available(*, unavailable, message: "Obsoleted")
public var obsoletedV: Swift.Void {
    get {
        return __root___obsoletedV_get()
    }
}
@available(*, deprecated, message: "Deprecated. Replacement: renamed")
public var renamedV: Swift.Void {
    get {
        return __root___renamedV_get()
    }
}
@_spi(Barnnotation)
public func bar() -> main.Bar {
    return main.Bar.__createClassWrapper(externalRCRef: __root___bar())
}
@available(*, deprecated, message: "message")
public func constMessage() -> Swift.Never {
    return __root___constMessage()
}
@available(*, deprecated, message: "Deprecated")
public func deprecatedF() -> Swift.Void {
    return __root___deprecatedF()
}
@available(*, deprecated, message: "Deprecated")
public func deprecatedImplicitlyF() -> Swift.Void {
    return __root___deprecatedImplicitlyF()
}
public func expressionOptIn() -> Swift.Void {
    return __root___expressionOptIn()
}
@_spi(Foonnotation)
public func foo() -> main.Foo {
    return main.Foo.__createClassWrapper(externalRCRef: __root___foo())
}
@available(*, deprecated, message: "->message<-")
public func formattedMessage() -> Swift.Never {
    return __root___formattedMessage()
}
public func localDeclarations() -> Swift.Void {
    return __root___localDeclarations()
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
    return __root___obsoletedF()
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
    return __root___renamedF()
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
@available(*, deprecated, message: ". Replacement: unrenamed")
public func unrenamed() -> Swift.Never {
    return __root___unrenamed()
}

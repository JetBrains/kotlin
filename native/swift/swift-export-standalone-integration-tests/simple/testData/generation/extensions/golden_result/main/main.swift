@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public typealias Foo = ExportedKotlinPackages.foo.Foo
public typealias _Foo = ExportedKotlinPackages.foo._Foo
public typealias __Foo = ExportedKotlinPackages.foo.__Foo
@_spi(ExperimentalApi)
public final class Bar: KotlinRuntime.KotlinBase {
    @_spi(ExperimentalApi)
    public init() {
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
    @_spi(ExperimentalApi)
    public func doubleReceiverExtFun(
        _ receiver: any ExportedKotlinPackages.foo.Foo
    ) -> Swift.String {
        return Bar_doubleReceiverExtFun__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo__(self.__externalRCRef(), receiver.__externalRCRef())
    }
    @_spi(ExperimentalApi)
    public func getDoubleReceiverExtProp(
        _ receiver: any ExportedKotlinPackages.foo.Foo
    ) -> Swift.Int32 {
        return Bar_doubleReceiverExtProp_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo__(self.__externalRCRef(), receiver.__externalRCRef())
    }
}
public final class Baz: KotlinRuntime.KotlinBase {
    public static var shared: main.Baz {
        get {
            return main.Baz.__createClassWrapper(externalRCRef: __root___Baz_get())
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
    public func doubleReceiverExtFun(
        _ receiver: any ExportedKotlinPackages.foo.Foo
    ) -> Swift.String {
        return Baz_doubleReceiverExtFun__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo__(self.__externalRCRef(), receiver.__externalRCRef())
    }
    public func getDoubleReceiverExtProp(
        _ receiver: any ExportedKotlinPackages.foo.Foo
    ) -> Swift.Int32 {
        return Baz_doubleReceiverExtProp_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo__(self.__externalRCRef(), receiver.__externalRCRef())
    }
}
@available(*, unavailable, message: "deprecated")
public final class DeprecatedBar: KotlinRuntime.KotlinBase {
    public init() {
        let __kt = __root___DeprecatedBar_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___DeprecatedBar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class GenericClass: KotlinRuntime.KotlinBase {
    public init() {
        let __kt = __root___GenericClass_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___GenericClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
@available(*, unavailable, message: "Unavailable type(s): main.DeprecatedBar")
public func deprecatedClassFun(
    _ receiver: main.DeprecatedBar
) -> Swift.String {
    fatalError()
}
public func funExtFun(
    _ receiver: @escaping () -> Swift.Void
) -> Swift.Bool {
    return __root___funExtFun__TypesOfArgumentsE__U2829202D_U20Swift_Void__({
        let originalBlock: () -> Swift.Void = receiver
        return {
            let _result = originalBlock()
            return { _result; return true }()
        }
    }())
}
public func genericExtFun(
    _ receiver: main.GenericClass
) -> Swift.String {
    return __root___genericExtFun__TypesOfArgumentsE__main_GenericClass__(receiver.__externalRCRef())
}
public func genericUpperBoundExtFun(
    _ receiver: main.GenericClass
) -> Swift.String {
    return __root___genericUpperBoundExtFun__TypesOfArgumentsE__main_GenericClass__(receiver.__externalRCRef())
}
@_spi(ExperimentalApi)
public func getDeprecatedSetterProp(
    _ receiver: main.Bar
) -> Swift.Bool {
    return __root___deprecatedSetterProp_get__TypesOfArgumentsE__main_Bar__(receiver.__externalRCRef())
}
@_spi(ExperimentalApi)
public func getErrorDeprecatedSetterProp(
    _ receiver: main.Bar
) -> Swift.Bool {
    return __root___errorDeprecatedSetterProp_get__TypesOfArgumentsE__main_Bar__(receiver.__externalRCRef())
}
public func getFunExtProp(
    _ receiver: @escaping () -> Swift.Void
) -> Swift.Int32 {
    return __root___funExtProp_get__TypesOfArgumentsE__U2829202D_U20Swift_Void__({
        let originalBlock: () -> Swift.Void = receiver
        return {
            let _result = originalBlock()
            return { _result; return true }()
        }
    }())
}
public func getGenericExtProp(
    _ receiver: main.GenericClass
) -> Swift.Int32 {
    return __root___genericExtProp_get__TypesOfArgumentsE__main_GenericClass__(receiver.__externalRCRef())
}
public func getGenericUpperBoundExtProp(
    _ receiver: main.GenericClass
) -> Swift.Int32 {
    return __root___genericUpperBoundExtProp_get__TypesOfArgumentsE__main_GenericClass__(receiver.__externalRCRef())
}
@_spi(ExperimentalApi)
public func getHiddenDeprecatedSetterProp(
    _ receiver: main.Bar
) -> Swift.Bool {
    return __root___hiddenDeprecatedSetterProp_get__TypesOfArgumentsE__main_Bar__(receiver.__externalRCRef())
}
@_spi(ExperimentalApi)
public func getOptInProp(
    _ receiver: main.Baz
) -> Swift.Int32 {
    return __root___optInProp_get__TypesOfArgumentsE__main_Baz__(receiver.__externalRCRef())
}
public func getOptInSetterProp(
    _ receiver: main.Baz
) -> Swift.String {
    return __root___optInSetterProp_get__TypesOfArgumentsE__main_Baz__(receiver.__externalRCRef())
}
@_spi(ExperimentalApi)
public func optInExtFun(
    _ receiver: main.Baz
) -> Swift.Bool {
    return __root___optInExtFun__TypesOfArgumentsE__main_Baz__(receiver.__externalRCRef())
}
@available(*, deprecated, message: "deprecated") @_spi(ExperimentalApi)
public func setDeprecatedSetterProp(
    _ receiver: main.Bar,
    value: Swift.Bool
) -> Swift.Void {
    return { __root___deprecatedSetterProp_set__TypesOfArgumentsE__main_Bar_Swift_Bool__(receiver.__externalRCRef(), value); return () }()
}
@available(*, unavailable, message: "deprecated") @_spi(ExperimentalApi)
public func setErrorDeprecatedSetterProp(
    _ receiver: main.Bar,
    value: Swift.Bool
) -> Swift.Void {
    fatalError()
}
@_spi(ExperimentalApi)
public func setOptInProp(
    _ receiver: main.Baz,
    value: Swift.Int32
) -> Swift.Void {
    return { __root___optInProp_set__TypesOfArgumentsE__main_Baz_Swift_Int32__(receiver.__externalRCRef(), value); return () }()
}
@_spi(ExperimentalApi)
public func setOptInSetterProp(
    _ receiver: main.Baz,
    value: Swift.String
) -> Swift.Void {
    return { __root___optInSetterProp_set__TypesOfArgumentsE__main_Baz_Swift_String__(receiver.__externalRCRef(), value); return () }()
}
extension main.Bar {
    @_spi(ExperimentalApi)
    public var deprecatedSetterProp: Swift.Bool {
        @_spi(ExperimentalApi)
        get {
            let receiver = self
            return main.getDeprecatedSetterProp(receiver)
        }
        @available(*, deprecated, message: "deprecated") @_spi(ExperimentalApi)
        set(value) {
            let receiver = self
            return main.setDeprecatedSetterProp(receiver, value: value)
        }
    }
}
extension main.Bar {
    @_spi(ExperimentalApi)
    public var errorDeprecatedSetterProp: Swift.Bool {
        @_spi(ExperimentalApi)
        get {
            let receiver = self
            return main.getErrorDeprecatedSetterProp(receiver)
        }
        @available(*, unavailable, message: "deprecated") @_spi(ExperimentalApi)
        set(value) {
            fatalError()
        }
    }
}
extension main.Bar {
    @_spi(ExperimentalApi)
    public var hiddenDeprecatedSetterProp: Swift.Bool {
        @_spi(ExperimentalApi)
        get {
            let receiver = self
            return main.getHiddenDeprecatedSetterProp(receiver)
        }
    }
}
extension main.Baz {
    @_spi(ExperimentalApi)
    public var optInProp: Swift.Int32 {
        @_spi(ExperimentalApi)
        get {
            let receiver = self
            return main.getOptInProp(receiver)
        }
        @_spi(ExperimentalApi)
        set(value) {
            let receiver = self
            return main.setOptInProp(receiver, value: value)
        }
    }
}
extension main.Baz {
    public var optInSetterProp: Swift.String {
        get {
            let receiver = self
            return main.getOptInSetterProp(receiver)
        }
        @_spi(ExperimentalApi)
        set(value) {
            let receiver = self
            return main.setOptInSetterProp(receiver, value: value)
        }
    }
}
extension main.Baz {
    @_spi(ExperimentalApi)
    public func optInExtFun() -> Swift.Bool {
        let receiver = self
        return main.optInExtFun(receiver)
    }
}
@available(*, unavailable, message: "Unavailable type(s): main.DeprecatedBar")
extension main.DeprecatedBar {
    @available(*, unavailable, message: "Unavailable type(s): main.DeprecatedBar")
    public func deprecatedClassFun() -> Swift.String {
        fatalError()
    }
}
extension ExportedKotlinPackages.foo.Foo {
    public var simpleProp: Swift.String {
        get {
            let receiver = self
            return ExportedKotlinPackages.foo.getSimpleProp(receiver)
        }
    }
}
extension ExportedKotlinPackages.foo.Foo {
    public var simplePropVar: Swift.String {
        get {
            let receiver = self
            return ExportedKotlinPackages.foo.getSimplePropVar(receiver)
        }
        set(`_`) {
            let receiver = self
            return ExportedKotlinPackages.foo.setSimplePropVar(receiver, `_`: `_`)
        }
    }
}
extension ExportedKotlinPackages.foo.Foo {
    @_spi(ExperimentalApi)
    public func getContextProp(
        _ context: main.Bar
    ) -> Swift.Int32 {
        let receiver = self
        return ExportedKotlinPackages.foo.getContextProp(context, receiver)
    }
}
extension ExportedKotlinPackages.foo.Foo {
    @_spi(ExperimentalApi)
    public func setContextProp(
        _ context: main.Bar,
        `_`: Swift.Int32
    ) -> Swift.Void {
        let receiver = self
        return ExportedKotlinPackages.foo.setContextProp(context, receiver, `_`: `_`)
    }
}
extension ExportedKotlinPackages.foo.Foo {
    public func simpleExtFun() -> Swift.String {
        let receiver = self
        return ExportedKotlinPackages.foo.simpleExtFun(receiver)
    }
}
extension ExportedKotlinPackages.foo.Foo {
    @_spi(ExperimentalApi)
    public func simpleExtFunWithArgs(
        arg1: Swift.Int32,
        arg2: main.Bar
    ) -> Swift.String {
        let receiver = self
        return ExportedKotlinPackages.foo.simpleExtFunWithArgs(receiver, arg1: arg1, arg2: arg2)
    }
}
extension ExportedKotlinPackages.foo.Foo {
    @_spi(ExperimentalApi)
    public func contextExtFun(
        _ context: main.Bar,
        arg: Swift.Bool
    ) -> Swift.Int32 {
        let receiver = self
        return ExportedKotlinPackages.foo.contextExtFun(context, receiver, arg: arg)
    }
}
extension ExportedKotlinPackages.foo.Foo {
    public func varargExtFun(
        args: Swift.String...
    ) -> Swift.Int32 {
        let receiver = self
        return foo_varargExtFun__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo_Swift_Array_Swift_String__Vararg___(receiver.__externalRCRef(), args)
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.foo.Foo where Self : ExportedKotlinPackages.foo.__Foo {
    @_spi(ExperimentalApi)
    public func doubleReceiverExtFun(
        _ receiver: main.Bar
    ) -> Swift.String {
        return foo_Foo_doubleReceiverExtFun__TypesOfArgumentsE__main_Bar__(self.__externalRCRef(), receiver.__externalRCRef())
    }
    @_spi(ExperimentalApi)
    public func getDoubleReceiverExtProp(
        _ receiver: main.Bar
    ) -> Swift.Int32 {
        return foo_Foo_doubleReceiverExtProp_get__TypesOfArgumentsE__main_Bar__(self.__externalRCRef(), receiver.__externalRCRef())
    }
}
extension ExportedKotlinPackages.foo.Foo {
    @_spi(ExperimentalApi)
    public func doubleReceiverExtFun(
        _ receiver: main.Bar
    ) -> Swift.String {
        fatalError("'doubleReceiverExtFun' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(ExperimentalApi)
    public func getDoubleReceiverExtProp(
        _ receiver: main.Bar
    ) -> Swift.Int32 {
        fatalError("'getDoubleReceiverExtProp' is an @_spi requirement that must be implemented by Swift conformers")
    }
}
extension main.GenericClass {
    public var genericExtProp: Swift.Int32 {
        get {
            let receiver = self
            return main.getGenericExtProp(receiver)
        }
    }
}
extension main.GenericClass {
    public var genericUpperBoundExtProp: Swift.Int32 {
        get {
            let receiver = self
            return main.getGenericUpperBoundExtProp(receiver)
        }
    }
}
extension main.GenericClass {
    public func genericExtFun() -> Swift.String {
        let receiver = self
        return main.genericExtFun(receiver)
    }
}
extension main.GenericClass {
    public func genericUpperBoundExtFun() -> Swift.String {
        let receiver = self
        return main.genericUpperBoundExtFun(receiver)
    }
}
extension ExportedKotlinPackages.foo.Foo? {
    public var nullableProp: Swift.Bool {
        get {
            let receiver = self
            return ExportedKotlinPackages.foo.getNullableProp(receiver)
        }
        set(`_`) {
            let receiver = self
            return ExportedKotlinPackages.foo.setNullableProp(receiver, `_`: `_`)
        }
    }
}
extension ExportedKotlinPackages.foo.Foo? {
    public func nullableFun() -> Swift.Bool {
        let receiver = self
        return ExportedKotlinPackages.foo.nullableFun(receiver)
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.foo.Foo, ExportedKotlinPackages.foo.__Foo where Wrapped : ExportedKotlinPackages.foo._Foo {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.foo._Foo {
}
extension ExportedKotlinPackages.foo {
    public protocol Foo: KotlinRuntime.KotlinBase, ExportedKotlinPackages.foo._Foo {
        @_spi(ExperimentalApi)
        func doubleReceiverExtFun(
            _ receiver: main.Bar
        ) -> Swift.String
        @_spi(ExperimentalApi)
        func getDoubleReceiverExtProp(
            _ receiver: main.Bar
        ) -> Swift.Int32
    }
    @objc(_ExportedKotlinPackages_foo_Foo)
    public protocol _Foo {
    }
    public protocol __Foo: KotlinRuntimeSupport._KotlinBridgeable {
    }
    @_spi(ExperimentalApi)
    public static func contextExtFun(
        _ context: main.Bar,
        _ receiver: any ExportedKotlinPackages.foo.Foo,
        arg: Swift.Bool
    ) -> Swift.Int32 {
        let (bar) = context
        return foo_contextExtFun__TypesOfArgumentsEC1__anyU20ExportedKotlinPackages_foo_Foo_Swift_Bool_main_Bar__(receiver.__externalRCRef(), arg, bar.__externalRCRef())
    }
    @_spi(ExperimentalApi)
    public static func getContextProp(
        _ context: main.Bar,
        _ receiver: any ExportedKotlinPackages.foo.Foo
    ) -> Swift.Int32 {
        let (bar) = context
        return foo_contextProp_get__TypesOfArgumentsEC1__anyU20ExportedKotlinPackages_foo_Foo_main_Bar__(receiver.__externalRCRef(), bar.__externalRCRef())
    }
    public static func getNullableProp(
        _ receiver: (any ExportedKotlinPackages.foo.Foo)?
    ) -> Swift.Bool {
        return foo_nullableProp_get__TypesOfArgumentsE__Swift_Optional_anyU20ExportedKotlinPackages_foo_Foo___(receiver.map { it in it.__externalRCRef() } ?? nil)
    }
    public static func getSimpleProp(
        _ receiver: any ExportedKotlinPackages.foo.Foo
    ) -> Swift.String {
        return foo_simpleProp_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo__(receiver.__externalRCRef())
    }
    public static func getSimplePropVar(
        _ receiver: any ExportedKotlinPackages.foo.Foo
    ) -> Swift.String {
        return foo_simplePropVar_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo__(receiver.__externalRCRef())
    }
    public static func nullableFun(
        _ receiver: (any ExportedKotlinPackages.foo.Foo)?
    ) -> Swift.Bool {
        return foo_nullableFun__TypesOfArgumentsE__Swift_Optional_anyU20ExportedKotlinPackages_foo_Foo___(receiver.map { it in it.__externalRCRef() } ?? nil)
    }
    @_spi(ExperimentalApi)
    public static func setContextProp(
        _ context: main.Bar,
        _ receiver: any ExportedKotlinPackages.foo.Foo,
        `_`: Swift.Int32
    ) -> Swift.Void {
        let (bar) = context
        return { foo_contextProp_set__TypesOfArgumentsEC1__anyU20ExportedKotlinPackages_foo_Foo_Swift_Int32_main_Bar__(receiver.__externalRCRef(), `_`, bar.__externalRCRef()); return () }()
    }
    public static func setNullableProp(
        _ receiver: (any ExportedKotlinPackages.foo.Foo)?,
        `_`: Swift.Bool
    ) -> Swift.Void {
        return { foo_nullableProp_set__TypesOfArgumentsE__Swift_Optional_anyU20ExportedKotlinPackages_foo_Foo__Swift_Bool__(receiver.map { it in it.__externalRCRef() } ?? nil, `_`); return () }()
    }
    public static func setSimplePropVar(
        _ receiver: any ExportedKotlinPackages.foo.Foo,
        `_`: Swift.String
    ) -> Swift.Void {
        return { foo_simplePropVar_set__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo_Swift_String__(receiver.__externalRCRef(), `_`); return () }()
    }
    public static func simpleExtFun(
        _ receiver: any ExportedKotlinPackages.foo.Foo
    ) -> Swift.String {
        return foo_simpleExtFun__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo__(receiver.__externalRCRef())
    }
    @_spi(ExperimentalApi)
    public static func simpleExtFunWithArgs(
        _ receiver: any ExportedKotlinPackages.foo.Foo,
        arg1: Swift.Int32,
        arg2: main.Bar
    ) -> Swift.String {
        return foo_simpleExtFunWithArgs__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo_Swift_Int32_main_Bar__(receiver.__externalRCRef(), arg1, arg2.__externalRCRef())
    }
    public static func varargExtFun(
        _ receiver: any ExportedKotlinPackages.foo.Foo,
        args: Swift.String...
    ) -> Swift.Int32 {
        return foo_varargExtFun__TypesOfArgumentsE__anyU20ExportedKotlinPackages_foo_Foo_Swift_Array_Swift_String__Vararg___(receiver.__externalRCRef(), args)
    }
}
extension ExportedKotlinPackages.other {
    public final class Other: KotlinRuntime.KotlinBase {
        public init() {
            let __kt = other_Other_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { other_Other_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public static func getOtherProp(
        _ receiver: ExportedKotlinPackages.other.Other
    ) -> Swift.Int32 {
        return other_otherProp_get__TypesOfArgumentsE__ExportedKotlinPackages_other_Other__(receiver.__externalRCRef())
    }
    public static func otherExtFun(
        _ receiver: ExportedKotlinPackages.other.Other
    ) -> Swift.String {
        return other_otherExtFun__TypesOfArgumentsE__ExportedKotlinPackages_other_Other__(receiver.__externalRCRef())
    }
    public static func setOtherProp(
        _ receiver: ExportedKotlinPackages.other.Other,
        value: Swift.Int32
    ) -> Swift.Void {
        return { other_otherProp_set__TypesOfArgumentsE__ExportedKotlinPackages_other_Other_Swift_Int32__(receiver.__externalRCRef(), value); return () }()
    }
}
@_cdecl("foo_Foo_doubleReceiverExtFun__TypesOfArgumentsE__main_Bar____reverse_swift")
package func foo_Foo_doubleReceiverExtFun__TypesOfArgumentsE__main_Bar____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ receiver: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.foo.Foo.Type.self) as! any ExportedKotlinPackages.foo.Foo
    let _result: Swift.String = _self.doubleReceiverExtFun(main.Bar.__createClassWrapper(externalRCRef: receiver))
    return _result
}

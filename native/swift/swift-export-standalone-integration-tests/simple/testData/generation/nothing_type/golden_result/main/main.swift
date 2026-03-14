@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public typealias Foo = Swift.Never
public typealias OptionalNothing = Swift.Never?
public final class Bar: KotlinRuntime.KotlinBase {
    public var p: Swift.Never {
        get {
            return { Bar_p_get(self.__externalRCRef()); fatalError() }()
        }
    }
    public init(
        p: Swift.Never
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
public var nullableNothingVariable: Swift.Never? {
    get {
        return { __root___nullableNothingVariable_get(); return nil }()
    }
    set {
        return { __root___nullableNothingVariable_set__TypesOfArguments__Swift_Optional_Swift_Never___({ newValue; return true }()); return () }()
    }
}
public var value: Swift.Never {
    get {
        return { __root___value_get(); fatalError() }()
    }
}
public var variable: Swift.Never {
    get {
        return { __root___variable_get(); fatalError() }()
    }
    set {
        fatalError()
    }
}
public func meaningOfLife() -> Swift.Never {
    return { __root___meaningOfLife(); fatalError() }()
}
public func meaningOfLife(
    input: Swift.Int32
) -> Swift.Never? {
    return { __root___meaningOfLife__TypesOfArguments__Swift_Int32__(input); return nil }()
}
public func meaningOfLife(
    input: Swift.Never?
) -> Swift.String {
    return __root___meaningOfLife__TypesOfArguments__Swift_Optional_Swift_Never___({ input; return true }())
}
public func meaningOfLife(
    p: Swift.Never
) -> Swift.Never {
    fatalError()
}
public func nothingClosure(
    block: @escaping () -> Swift.Never
) -> Swift.Never {
    return { __root___nothingClosure__TypesOfArguments__U2829202D_U20Swift_Never__({
        let originalBlock = block
        return { return { originalBlock() }() }
    }()); fatalError() }()
}
public func nothingClosureParam(
    block: @escaping (Swift.Never) -> Swift.String
) -> Swift.String {
    return __root___nothingClosureParam__TypesOfArguments__U28Swift_NeverU29202D_U20Swift_String__({
        let originalBlock = block
        return { arg0 in return originalBlock({ arg0; fatalError() }()) }
    }())
}
public func nothingFunctional() -> () -> Swift.Never {
    return {
        let pointerToBlock = __root___nothingFunctional()
        return { return { main_internal_functional_type_caller_SwiftU2ENever__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock); fatalError() }() }
    }()
}
public func nothingFunctionalParam() -> (Swift.Never) -> Swift.String {
    return {
        let _ = __root___nothingFunctionalParam()
        return { _ in fatalError() }
    }()
}
public func nothingOptClosure(
    block: @escaping () -> Swift.Never?
) -> Swift.Never {
    return { __root___nothingOptClosure__TypesOfArguments__U2829202D_U20Swift_Optional_Swift_Never___({
        let originalBlock = block
        return { return { originalBlock(); return true }() }
    }()); fatalError() }()
}
public func nothingOptClosureParam(
    block: @escaping (Swift.Never?) -> Swift.String
) -> Swift.String {
    return __root___nothingOptClosureParam__TypesOfArguments__U28Swift_Optional_Swift_Never_U29202D_U20Swift_String__({
        let originalBlock = block
        return { arg0 in return originalBlock({ arg0; return nil }()) }
    }())
}
public func nothingOptFunctional() -> () -> Swift.Never? {
    return {
        let pointerToBlock = __root___nothingOptFunctional()
        return { return { main_internal_functional_type_caller_SwiftU2EOptionalU3CSwiftU2ENeverU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock); return nil }() }
    }()
}
public func nothingOptFunctionalParam() -> (Swift.Never?) -> Swift.String {
    return {
        let pointerToBlock = __root___nothingOptFunctionalParam()
        return { _1 in return main_internal_functional_type_caller_SwiftU2EString__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Never___(pointerToBlock, { _1; return true }()) }
    }()
}
public func nullableNothingInput(
    input: Swift.Never?
) -> Swift.Void {
    return { __root___nullableNothingInput__TypesOfArguments__Swift_Optional_Swift_Never___({ input; return true }()); return () }()
}
public func nullableNothingOutput() -> Swift.Never? {
    return { __root___nullableNothingOutput(); return nil }()
}

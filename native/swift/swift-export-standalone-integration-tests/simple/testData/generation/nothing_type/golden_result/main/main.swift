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
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
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
    return { __root___nothingClosure__TypesOfArguments__U2829202D_U20Swift_Never__(Unmanaged.passRetained((block as () -> Swift.Never) as AnyObject).toOpaque()); fatalError() }()
}
public func nothingClosureParam(
    block: @escaping (Swift.Never) -> Swift.String
) -> Swift.String {
    return __root___nothingClosureParam__TypesOfArguments__U28Swift_NeverU29202D_U20Swift_String__(Unmanaged.passRetained((block as (Swift.Never) -> Swift.String) as AnyObject).toOpaque())
}
public func nothingFunctional() -> () -> Swift.Never {
    return {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___nothingFunctional(), options: .asBestFittingWrapper)
        return { return { main_internal_functional_type_caller_SwiftU2ENever__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef()); fatalError() }() }
    }()
}
public func nothingFunctionalParam() -> (Swift.Never) -> Swift.String {
    return {
        let _ = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___nothingFunctionalParam(), options: .asBestFittingWrapper)
        return { _ in fatalError() }
    }()
}
public func nothingOptClosure(
    block: @escaping () -> Swift.Never?
) -> Swift.Never {
    return { __root___nothingOptClosure__TypesOfArguments__U2829202D_U20Swift_Optional_Swift_Never___(Unmanaged.passRetained((block as () -> Swift.Optional<Swift.Never>) as AnyObject).toOpaque()); fatalError() }()
}
public func nothingOptClosureParam(
    block: @escaping (Swift.Never?) -> Swift.String
) -> Swift.String {
    return __root___nothingOptClosureParam__TypesOfArguments__U28Swift_Optional_Swift_Never_U29202D_U20Swift_String__(Unmanaged.passRetained((block as (Swift.Optional<Swift.Never>) -> Swift.String) as AnyObject).toOpaque())
}
public func nothingOptFunctional() -> () -> Swift.Never? {
    return {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___nothingOptFunctional(), options: .asBestFittingWrapper)
        return { return { main_internal_functional_type_caller_SwiftU2EOptionalU3CSwiftU2ENeverU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef()); return nil }() }
    }()
}
public func nothingOptFunctionalParam() -> (Swift.Never?) -> Swift.String {
    return {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___nothingOptFunctionalParam(), options: .asBestFittingWrapper)
        return { _1 in return main_internal_functional_type_caller_SwiftU2EString__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Never___(pointerToBlock.__externalRCRef(), { _1; return true }()) }
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
@_cdecl("main_internal_functional_type_callee_SwiftU2ENever__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
package func main_internal_functional_type_callee_SwiftU2ENever__TypesOfArguments__Swift_UnsafeMutableRawPointer__(_ pointerToClosure: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Never = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! () -> Swift.Never)()
    return { _result }()
}

@_cdecl("main_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2ENeverU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
package func main_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2ENeverU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__(_ pointerToClosure: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Optional<Swift.Never> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! () -> Swift.Optional<Swift.Never>)()
    return { _result; return true }()
}

@_cdecl("main_internal_functional_type_callee_SwiftU2EString__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Never___")
package func main_internal_functional_type_callee_SwiftU2EString__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Never___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.Bool) -> Swift.String {
    let _result: Swift.String = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.Never>) -> Swift.String)({ _1; return nil }())
    return _result
}

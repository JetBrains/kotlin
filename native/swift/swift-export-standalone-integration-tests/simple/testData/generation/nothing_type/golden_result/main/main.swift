@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public typealias Foo = Swift.Never
public typealias OptionalNothing = Swift.Never?
public final class Bar: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public var p: Swift.Never {
        get {
            return Bar_p_get(self.__externalRCRef())
        }
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public init(
        p: Swift.Never
    ) {
        fatalError()
    }
}
public var nullableNothingVariable: Swift.Never? {
    get {
        return { __root___nullableNothingVariable_get(); return nil; }()
    }
    set {
        return __root___nullableNothingVariable_set__TypesOfArguments__Swift_Optional_Swift_Never___()
    }
}
public var value: Swift.Never {
    get {
        return __root___value_get()
    }
}
public var variable: Swift.Never {
    get {
        return __root___variable_get()
    }
    set {
        fatalError()
    }
}
public func meaningOfLife() -> Swift.Never {
    return __root___meaningOfLife()
}
public func meaningOfLife(
    input: Swift.Int32
) -> Swift.Never? {
    return { __root___meaningOfLife__TypesOfArguments__Swift_Int32__(input); return nil; }()
}
public func meaningOfLife(
    input: Swift.Never?
) -> Swift.String {
    return __root___meaningOfLife__TypesOfArguments__Swift_Optional_Swift_Never___()
}
public func meaningOfLife(
    p: Swift.Never
) -> Swift.Never {
    fatalError()
}
public func nullableNothingInput(
    input: Swift.Never?
) -> Swift.Void {
    return __root___nullableNothingInput__TypesOfArguments__Swift_Optional_Swift_Never___()
}
public func nullableNothingOutput() -> Swift.Never? {
    return { __root___nullableNothingOutput(); return nil; }()
}

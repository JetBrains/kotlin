@_implementationOnly import KotlinBridges_main
import KotlinRuntime

public typealias Foo = Swift.Never
public final class Bar : KotlinRuntime.KotlinBase {
    public var p: Swift.Never {
        get {
            return Bar_p_get(self.__externalRCRef())
        }
    }
    public override init(
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
@_cdecl("SwiftExport_main_Bar_toRetainedSwift")
private func SwiftExport_main_Bar_toRetainedSwift(
    externalRCRef: Swift.UInt
) -> Swift.UnsafeMutableRawPointer {
    return Unmanaged.passRetained(main.Bar(__externalRCRef: externalRCRef)).toOpaque()
}
public func meaningOfLife() -> Swift.Never {
    return __root___meaningOfLife()
}
public func meaningOfLife(
    p: Swift.Never
) -> Swift.Never {
    fatalError()
}

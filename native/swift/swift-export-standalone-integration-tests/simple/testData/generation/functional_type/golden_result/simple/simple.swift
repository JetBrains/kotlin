@_implementationOnly import KotlinBridges_simple
import KotlinRuntime
import KotlinRuntimeSupport

public var closure_property: Swift.Never {
    get {
        fatalError()
    }
    set {
        fatalError()
    }
}
public func foo_1() -> Swift.Never {
    fatalError()
}
public func foo_consume_recursive(
    block: @escaping (Swift.Never) -> () -> Swift.Void
) -> Swift.Void {
    fatalError()
}
public func foo_consume_simple(
    block: @escaping () -> Swift.Void
) -> Swift.Void {
    return __root___foo_consume_simple__TypesOfArguments__U2829202D_U20Swift_Void__({
        let originalBlock = block
        return { return originalBlock() }
    }())
}
public func foo_sus() -> Swift.Never {
    fatalError()
}

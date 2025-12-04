@_implementationOnly import KotlinBridges_simple
import KotlinRuntime
import KotlinRuntimeSupport

public var closure_property: () -> Swift.Void {
    get {
        return {
            let pointerToBlock = __root___closure_property_get()
            return { return simple_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock) }
        }()
    }
    set {
        return __root___closure_property_set__TypesOfArguments__U2829202D_U20Swift_Void__({
            let originalBlock = newValue
            return { return originalBlock() }
        }())
    }
}
public func foo_1() -> () -> Swift.Void {
    return {
        let pointerToBlock = __root___foo_1()
        return { return simple_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock) }
    }()
}
public func foo_consume_producing(
    block: @escaping () -> () -> Swift.Void
) -> Swift.Void {
    return __root___foo_consume_producing__TypesOfArguments__U2829202D_U202829202D_U20Swift_Void__({
        let originalBlock = block
        return { return {
        let originalBlock = originalBlock()
        return { return originalBlock() }
    }() }
    }())
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

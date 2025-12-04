@_implementationOnly import KotlinBridges_optional_closure
import KotlinRuntime
import KotlinRuntimeSupport

public func consume_consuming_opt_closure(
    arg: ((Swift.Optional<() -> Swift.String>) -> Swift.Void)?
) -> Swift.Void {
    return __root___consume_consuming_opt_closure__TypesOfArguments__Swift_Optional_U28Swift_Optional_U2829202D_U20Swift_String_U29202D_U20Swift_Void___(arg.map { it in {
        let originalBlock = it
        return { arg0 in return originalBlock(arg0.map { it in {
        let pointerToBlock = it
        return { return optional_closure_internal_functional_type_caller_SwiftU2EString__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock) }
    }() }) }
    }() } ?? nil)
}
public func consume_opt_closure(
    arg: (() -> Swift.Void)?
) -> Swift.Void {
    return __root___consume_opt_closure__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Void___(arg.map { it in {
        let originalBlock = it
        return { return originalBlock() }
    }() } ?? nil)
}
public func consume_producing_opt_closure(
    arg: (() -> Swift.Optional<() -> Swift.Void>)?
) -> Swift.Void {
    return __root___consume_producing_opt_closure__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Optional_U2829202D_U20Swift_Void____(arg.map { it in {
        let originalBlock = it
        return { return originalBlock().map { it in {
        let originalBlock = it
        return { return originalBlock() }
    }() } ?? nil }
    }() } ?? nil)
}
public func produce_opt_closure(
    arg: Swift.Void
) -> (() -> Swift.String)? {
    return __root___produce_opt_closure__TypesOfArguments__Swift_Void__().map { it in {
        let pointerToBlock = it
        return { return optional_closure_internal_functional_type_caller_SwiftU2EString__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock) }
    }() }
}

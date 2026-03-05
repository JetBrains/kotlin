@_implementationOnly import KotlinBridges_unit_param
import KotlinRuntime
import KotlinRuntimeSupport

public func bar() -> (Swift.String, Swift.Void) -> Swift.Void {
    return {
        let pointerToBlock = __root___bar()
        return { _1, _ in return unit_param_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_Void__(pointerToBlock, _1) }
    }()
}
public func foo() -> (Swift.Void) -> Swift.Void {
    return {
        let pointerToBlock = __root___foo()
        return { _ in return unit_param_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock) }
    }()
}

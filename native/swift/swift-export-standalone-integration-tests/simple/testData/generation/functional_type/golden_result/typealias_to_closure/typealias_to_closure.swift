@_implementationOnly import KotlinBridges_typealias_to_closure
import KotlinRuntime
import KotlinRuntimeSupport

public typealias Closure = (Swift.Int32, Swift.Int32) -> Swift.Void
public func typealias_demo(
    input: @escaping typealias_to_closure.Closure
) -> typealias_to_closure.Closure {
    return {
        let pointerToBlock = __root___typealias_demo__TypesOfArguments__U28Swift_Int32_U20Swift_Int32U29202D_U20Swift_Void__({
        let originalBlock = input
        return { arg0, arg1 in return originalBlock(arg0, arg1) }
    }())
        return { _1, _2 in return typealias_to_closure_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Int32__(pointerToBlock, _1, _2) }
    }()
}

@_implementationOnly import KotlinBridges_typealias_to_closure
import KotlinRuntime
import KotlinRuntimeSupport

public typealias CallbackWithInnerClosure = (@escaping () -> Swift.Int32) -> Swift.Int32
public typealias Closure = (Swift.Int32, Swift.Int32) -> Swift.Void
public func foo_flow_with_callback(
    callback: @escaping typealias_to_closure.CallbackWithInnerClosure
) -> typealias_to_closure.CallbackWithInnerClosure {
    return {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___foo_flow_with_callback__TypesOfArguments__U2840escapingU202829202D_U20Swift_Int32U29202D_U20Swift_Int32__({
        let originalBlock: (@escaping () -> Swift.Int32) -> Swift.Int32 = callback
        return { (arg0: Swift.UnsafeMutableRawPointer) in
            let _arg0: () -> Swift.Int32 = {
                let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: arg0, options: .asBestFittingWrapper)!
                return { return typealias_to_closure_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef()!) }
            }()
            let _result = originalBlock(_arg0)
            return _result
        }
    }()), options: .asBestFittingWrapper)!
        return { _1 in return typealias_to_closure_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_U2829202D_U20Swift_Int32__(pointerToBlock.__externalRCRef()!, {
        let originalBlock: () -> Swift.Int32 = _1
        return {
            let _result = originalBlock()
            return _result
        }
    }()) }
    }()
}
public func typealias_demo(
    input: @escaping typealias_to_closure.Closure
) -> typealias_to_closure.Closure {
    return {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __root___typealias_demo__TypesOfArguments__U28Swift_Int32_U20Swift_Int32U29202D_U20Swift_Void__({
        let originalBlock: (Swift.Int32, Swift.Int32) -> Swift.Void = input
        return { (arg0: Swift.Int32, arg1: Swift.Int32) in
            let _arg0: Swift.Int32 = arg0
            let _arg1: Swift.Int32 = arg1
            let _result = originalBlock(_arg0, _arg1)
            return { _result; return true }()
        }
    }()), options: .asBestFittingWrapper)!
        return { _1, _2 in return { typealias_to_closure_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Int32__(pointerToBlock.__externalRCRef()!, _1, _2); return () }() }
    }()
}

@_implementationOnly import KotlinBridges_main
@_exported import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport

public func accept_suspend_fun_with_context(
    block: @escaping (Swift.String) async throws -> Swift.Int32
) -> Swift.Int32 {
    return __root___accept_suspend_fun_with_context__TypesOfArguments__U28Swift_StringU2920asyncU20throwsU202D_U20Swift_Int32__({
        let originalBlock: (Swift.String) async throws -> Swift.Int32 = block
        return { (ctx0: Swift.String, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
            let _ctx0: Swift.String = ctx0
            let _continuation: (Swift.Int32) -> Swift.Void = {
                let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(pointerToBlock.__externalRCRef()!, _1); return () }() }
            }()
            let _exception: (Swift.Error) -> Swift.Void = {
                let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
            }()
            let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
            let _result = withKotlinTask(_continuation, _exception, _cancellation){
                try await originalBlock((_ctx0))
            }
            return { _result; return true }()
        }
    }())
}
public func accept_suspend_function_type(
    block: @escaping () async throws -> Swift.Int32
) -> Swift.Int32 {
    return __root___accept_suspend_function_type__TypesOfArguments__U282920asyncU20throwsU202D_U20Swift_Int32__({
        let originalBlock: () async throws -> Swift.Int32 = block
        return { (continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
            let _continuation: (Swift.Int32) -> Swift.Void = {
                let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(pointerToBlock.__externalRCRef()!, _1); return () }() }
            }()
            let _exception: (Swift.Error) -> Swift.Void = {
                let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
            }()
            let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
            let _result = withKotlinTask(_continuation, _exception, _cancellation){
                try await originalBlock()
            }
            return { _result; return true }()
        }
    }())
}

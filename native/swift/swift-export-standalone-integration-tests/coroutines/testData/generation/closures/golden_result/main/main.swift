@_implementationOnly import KotlinBridges_main
@_exported import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport

public func accept_suspend_function_type(
    block: @escaping () async throws -> Swift.Int32
) -> Swift.Int32 {
    return __root___accept_suspend_function_type__TypesOfArguments__U282920asyncU20throwsU202D_U20Swift_Int32__({
        let originalBlock = block
        return { (__continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
            let __continuation: (Swift.Int32) -> Swift.Void = {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
        return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(pointerToBlock.__externalRCRef()!, _1); return () }() }
    }()
            let __exception: (Swift.Error) -> Swift.Void = {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
        return { _1 in return { main_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
    }()
            let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)

            let task = Task {
                await withTaskCancellationHandler {
                    do {
                        let result = try await originalBlock()
                        __continuation(result)
                    } catch {
                        __exception(error)
                    }
                } onCancel: {
                    __cancellation.cancelExternally()
                }
            }
            __cancellation.setCallback { shouldCancel in
                defer { if shouldCancel { task.cancel() } }
                return task.isCancelled
            }
        }
    }())
}

@_implementationOnly import KotlinBridges_main
import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinxCoroutinesCore

public func demo() -> any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow {
    return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___demo()) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow
}
public func demo_ft_produce() -> (Swift.Int32) async throws -> Swift.Int32 {
    return {
        let pointerToBlock = __root___demo_ft_produce()
        return { _1 in try await {
        try Task.checkCancellation()
        var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
        return try await withTaskCancellationHandler {
            try await withUnsafeThrowingContinuation { nativeContinuation in
                withUnsafeCurrentTask { currentTask in
                    let continuation: (Swift.Int32) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                    let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                        nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                    }
                    cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                    let _: () = main_internal_functional_type_caller_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(pointerToBlock, _1, {
                        let originalBlock = continuation
                        return { arg0 in return { originalBlock(arg0); return 0 }() }
                    }(), {
                        let originalBlock = exception
                        return { arg0 in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return 0 }() }
                    }(), cancellation.__externalRCRef())
                }
            }
        } onCancel: {
            cancellation?.cancelExternally()
        }
    }() }
    }()
}

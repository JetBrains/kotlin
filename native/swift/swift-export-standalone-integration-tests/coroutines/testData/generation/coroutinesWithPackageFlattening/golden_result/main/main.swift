@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport

public func testSuspendFunction() async throws -> Swift.Int32 {
    try await ExportedKotlinPackages.flattened.testSuspendFunction()
}
extension ExportedKotlinPackages.flattened {
    public static func testSuspendFunction() async throws -> Swift.Int32 {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Int32) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: () -> Swift.Void = { nativeContinuation.resume(throwing: CancellationError()) }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: () = flattened_testSuspendFunction({
                            let originalBlock = continuation
                            return { arg0 in return { originalBlock(arg0); return 0 }() }
                        }(), {
                            let originalBlock = exception
                            return { return { originalBlock(); return 0 }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
}

@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
@_exported import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport

public func testSuspendFunction() async throws -> Swift.Int32 {
    try await ExportedKotlinPackages.flattened.testSuspendFunction()
}
extension ExportedKotlinPackages.flattened {
    public static func testSuspendFunction() async throws -> Swift.Int32 {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = flattened_testSuspendFunction({
                let originalBlock: (Swift.Int32) -> Swift.Void = continuation
                return { (arg0: Swift.Int32) in return { originalBlock(arg0); return true }() }
            }(), {
                let originalBlock: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
            }(), cancellation.__externalRCRef())
        }
    }
}

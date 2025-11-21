@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport

public func testSuspendFunction() async -> Swift.Int32 {
    await ExportedKotlinPackages.flattened.testSuspendFunction()
}
extension ExportedKotlinPackages.flattened {
    public static func testSuspendFunction() async -> Swift.Int32 {
                await withUnsafeContinuation { nativeContinuation in
                    let continuation: (Swift.Int32) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                    let _: () = flattened_testSuspendFunction({
            let originalBlock = continuation
            return { arg0 in return { originalBlock(arg0); return 0 }() }
        }())
                }
    }
}

@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_another
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.some {
    public static func foo() -> Swift.ClosedRange<Swift.Int32> {
        let _result = some_foo()
        return kotlin_ranges_intRange_getStart_int_another(_result) ... kotlin_ranges_intRange_getEndInclusive_int_another(_result)
    }
}

@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.generation.ranges.ranges {
    public static func bar() -> Swift.ClosedRange<Swift.String> {
        return Swift.ClosedRange<Swift.String>(__externalRCRefUnsafe: generation_ranges_ranges_bar(), options: .asBestFittingWrapper)
    }
    public static func baz() -> Swift.Range<Swift.String> {
        return Swift.Range<Swift.String>(__externalRCRefUnsafe: generation_ranges_ranges_baz(), options: .asBestFittingWrapper)
    }
    public static func foo() -> Swift.ClosedRange<Swift.Int32> {
        return Swift.ClosedRange<Swift.Int32>(__externalRCRefUnsafe: generation_ranges_ranges_foo(), options: .asBestFittingWrapper)
    }
}

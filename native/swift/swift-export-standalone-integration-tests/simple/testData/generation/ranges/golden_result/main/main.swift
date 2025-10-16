@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib

extension ExportedKotlinPackages.generation.ranges.ranges {
    public static func accept(
        range: ExportedKotlinPackages.kotlin.ranges.IntRange
    ) -> ExportedKotlinPackages.kotlin.ranges.LongRange {
        return ExportedKotlinPackages.kotlin.ranges.LongRange.__createClassWrapper(externalRCRef: generation_ranges_ranges_accept__TypesOfArguments__ExportedKotlinPackages_kotlin_ranges_IntRange__(range.__externalRCRef()))
    }
    public static func bar() -> any ExportedKotlinPackages.kotlin.ranges.ClosedRange {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: generation_ranges_ranges_bar()) as! any ExportedKotlinPackages.kotlin.ranges.ClosedRange
    }
    public static func baz() -> any ExportedKotlinPackages.kotlin.ranges.OpenEndRange {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: generation_ranges_ranges_baz()) as! any ExportedKotlinPackages.kotlin.ranges.OpenEndRange
    }
    public static func foo() -> ExportedKotlinPackages.kotlin.ranges.IntRange {
        return ExportedKotlinPackages.kotlin.ranges.IntRange.__createClassWrapper(externalRCRef: generation_ranges_ranges_foo())
    }
    public static func unsupported() -> any ExportedKotlinPackages.kotlin.ranges.ClosedRange {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: generation_ranges_ranges_unsupported()) as! any ExportedKotlinPackages.kotlin.ranges.ClosedRange
    }
}

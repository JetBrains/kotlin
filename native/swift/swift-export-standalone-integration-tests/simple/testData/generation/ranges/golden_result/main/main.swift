@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib

extension ExportedKotlinPackages.generation.ranges.ranges {
    public static func accept(
        range: Swift.ClosedRange<Swift.Int32>
    ) -> Swift.ClosedRange<Swift.Int64> {
        let _result = generation_ranges_ranges_accept__TypesOfArguments__Swift_ClosedRange_Swift_Int32___(range.lowerBound, range.upperBound)
        return kotlin_ranges_longRange_getStart_long(_result) ... kotlin_ranges_longRange_getEndInclusive_long(_result)
    }
    public static func acceptClosed(
        range: Swift.ClosedRange<Swift.Int32>
    ) -> Swift.Range<Swift.Int32> {
        let _result = generation_ranges_ranges_acceptClosed__TypesOfArguments__Swift_ClosedRange_Swift_Int32___(range.lowerBound, range.upperBound)
        return kotlin_ranges_openEndRange_getStart_int(_result) ..< kotlin_ranges_openEndRange_getEndExclusive_int(_result)
    }
    public static func acceptNullable(
        range: Swift.ClosedRange<Swift.Int32>?
    ) -> Swift.ClosedRange<Swift.Int32>? {
        let _result = generation_ranges_ranges_acceptNullable__TypesOfArguments__Swift_Optional_Swift_ClosedRange_Swift_Int32____(range.map { it in NSNumber(value: it.lowerBound) } ?? nil, range.map { it in NSNumber(value: it.upperBound) } ?? nil)
        return { switch _result { case nil: .none; case let res: kotlin_ranges_intRange_getStart_int(res) ... kotlin_ranges_intRange_getEndInclusive_int(res); } }()
    }
    public static func bar() -> Swift.ClosedRange<Swift.Int32> {
        let _result = generation_ranges_ranges_bar()
        return kotlin_ranges_closedRange_getStart_int(_result) ... kotlin_ranges_closedRange_getEndInclusive_int(_result)
    }
    public static func baz() -> Swift.Range<Swift.Int64> {
        let _result = generation_ranges_ranges_baz()
        return kotlin_ranges_openEndRange_getStart_long(_result) ..< kotlin_ranges_openEndRange_getEndExclusive_long(_result)
    }
    public static func foo() -> Swift.ClosedRange<Swift.Int32> {
        let _result = generation_ranges_ranges_foo()
        return kotlin_ranges_intRange_getStart_int(_result) ... kotlin_ranges_intRange_getEndInclusive_int(_result)
    }
    public static func mapper(
        arg: [Swift.ClosedRange<Swift.Int64>]
    ) -> [Swift.ClosedRange<Swift.Int32>] {
        return generation_ranges_ranges_mapper__TypesOfArguments__Swift_Array_Swift_ClosedRange_Swift_Int64____(arg) as! Swift.Array<Swift.ClosedRange<Swift.Int32>>
    }
    public static func total(
        list: [Swift.ClosedRange<Swift.Int32>]
    ) -> Swift.ClosedRange<Swift.Int32> {
        let _result = generation_ranges_ranges_total__TypesOfArguments__Swift_Array_Swift_ClosedRange_Swift_Int32____(list)
        return kotlin_ranges_intRange_getStart_int(_result) ... kotlin_ranges_intRange_getEndInclusive_int(_result)
    }
    public static func unsupported() -> any ExportedKotlinPackages.kotlin.ranges.ClosedRange {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: generation_ranges_ranges_unsupported()) as! any ExportedKotlinPackages.kotlin.ranges.ClosedRange
    }
}

@_implementationOnly import KotlinBridges_simple
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib

public func accept(
    range: Swift.ClosedRange<Swift.Int32>
) -> Swift.ClosedRange<Swift.Int64> {
    return { let _ref = __root___accept__TypesOfArguments__Swift_ClosedRange_Swift_Int32___(kotlin_ranges_closedRange_create_int_simple(range.lowerBound, range.upperBound)); return kotlin_ranges_longRange_getStart_long_simple(_ref) ... kotlin_ranges_longRange_getEndInclusive_long_simple(_ref) }()
}
public func acceptClosed(
    range: Swift.ClosedRange<Swift.Int32>
) -> Swift.Range<Swift.Int32> {
    return { let _ref = __root___acceptClosed__TypesOfArguments__Swift_ClosedRange_Swift_Int32___(kotlin_ranges_closedRange_create_int_simple(range.lowerBound, range.upperBound)); return kotlin_ranges_openEndRange_getStart_int_simple(_ref) ..< kotlin_ranges_openEndRange_getEndExclusive_int_simple(_ref) }()
}
public func bar() -> Swift.ClosedRange<Swift.Int32> {
    return { let _ref = __root___bar(); return kotlin_ranges_closedRange_getStart_int_simple(_ref) ... kotlin_ranges_closedRange_getEndInclusive_int_simple(_ref) }()
}
public func baz() -> Swift.Range<Swift.Int64> {
    return { let _ref = __root___baz(); return kotlin_ranges_openEndRange_getStart_long_simple(_ref) ..< kotlin_ranges_openEndRange_getEndExclusive_long_simple(_ref) }()
}
public func foo() -> Swift.ClosedRange<Swift.Int32> {
    return { let _ref = __root___foo(); return kotlin_ranges_intRange_getStart_int_simple(_ref) ... kotlin_ranges_intRange_getEndInclusive_int_simple(_ref) }()
}
public func unsupported() -> any ExportedKotlinPackages.kotlin.ranges.ClosedRange {
    return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___unsupported()) as! any ExportedKotlinPackages.kotlin.ranges.ClosedRange
}

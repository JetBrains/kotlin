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
public func foo_opt() -> Swift.ClosedRange<Swift.Int32>? {
    return { switch __root___foo_opt() { case nil: .none; case let res?: { let _ref = res; return kotlin_ranges_intRange_getStart_int_simple(_ref) ... kotlin_ranges_intRange_getEndInclusive_int_simple(_ref) }(); } }()
}
public func unsupported() -> any ExportedKotlinPackages.kotlin.ranges.ClosedRange<Swift.String> {
    return { let _ptr = __root___unsupported(); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.ranges.ClosedRange<Swift.String>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.ranges.AnyClosedRange<Swift.String>.__createTypedWrapper(externalRCRef: _ptr) }()
}

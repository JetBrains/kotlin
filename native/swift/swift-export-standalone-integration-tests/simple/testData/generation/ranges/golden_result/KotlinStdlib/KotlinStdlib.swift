@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinStdlib
import KotlinRuntime
import KotlinRuntimeSupport

@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.ranges.ClosedRange where Self : ExportedKotlinPackages.kotlin.ranges.__ClosedRange {
    public var endInclusive: T {
        get {
            return T.__fromKotlinPointer(kotlin_ranges_ClosedRange_endInclusive_get(self.__externalRCRef()))
        }
    }
    public var start: T {
        get {
            return T.__fromKotlinPointer(kotlin_ranges_ClosedRange_start_get(self.__externalRCRef()))
        }
    }
    public func contains(
        value: T
    ) -> Swift.Bool {
        return kotlin_ranges_ClosedRange_contains__TypesOfArguments__T__(self.__externalRCRef(), value.__retainedToKotlinPointer())
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_ranges_ClosedRange_isEmpty(self.__externalRCRef())
    }
    public static func ~=(
        this: Self,
        value: T
    ) -> Swift.Bool {
        this.contains(value: value)
    }
}
extension ExportedKotlinPackages.kotlin.ranges.ClosedRange {
    public func contains(
        value: T
    ) -> Swift.Bool {
        return kotlin_ranges_ClosedRange_contains__TypesOfArguments__T___direct(self.__externalRCRef(), value.__retainedToKotlinPointer())
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_ranges_ClosedRange_isEmpty_direct(self.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.ranges._ClosedRange {
}
extension ExportedKotlinPackages.kotlin.ranges {
    public protocol ClosedRange<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.ranges._ClosedRange {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        var endInclusive: T {
            get
        }
        var start: T {
            get
        }
        func contains(
            value: T
        ) -> Swift.Bool
        func isEmpty() -> Swift.Bool
    }
    @objc(_ExportedKotlinPackages_kotlin_ranges_ClosedRange)
    public protocol _ClosedRange {
    }
    public protocol __ClosedRange: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public final class AnyClosedRange<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.ranges.ClosedRange, ExportedKotlinPackages.kotlin.ranges.__ClosedRange, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
@_cdecl("kotlin_ranges_ClosedRange_contains__TypesOfArguments__T____reverse_swift")
package func kotlin_ranges_ClosedRange_contains__TypesOfArguments__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.ranges.ClosedRange)
    func _open<__Self: ExportedKotlinPackages.kotlin.ranges.ClosedRange>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.contains(value: __Self.T.__fromKotlinPointer(value))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_ranges_ClosedRange_endInclusive_get__reverse_swift")
package func kotlin_ranges_ClosedRange_endInclusive_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.ranges.ClosedRange)
    func _open<__Self: ExportedKotlinPackages.kotlin.ranges.ClosedRange>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.endInclusive
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_ranges_ClosedRange_isEmpty__reverse_swift")
package func kotlin_ranges_ClosedRange_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.ranges.ClosedRange)
    func _open<__Self: ExportedKotlinPackages.kotlin.ranges.ClosedRange>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.isEmpty()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_ranges_ClosedRange_start_get__reverse_swift")
package func kotlin_ranges_ClosedRange_start_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.ranges.ClosedRange)
    func _open<__Self: ExportedKotlinPackages.kotlin.ranges.ClosedRange>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.start
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

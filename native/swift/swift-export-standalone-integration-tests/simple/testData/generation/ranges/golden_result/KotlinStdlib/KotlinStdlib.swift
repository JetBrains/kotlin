@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinStdlib
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.kotlin.ranges.ClosedRange where Self : ExportedKotlinPackages.kotlin.ranges.__ClosedRange {
    public var endInclusive: any ExportedKotlinPackages.kotlin.Comparable {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_ranges_ClosedRange_endInclusive_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.Comparable
        }
    }
    public var start: any ExportedKotlinPackages.kotlin.Comparable {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_ranges_ClosedRange_start_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.Comparable
        }
    }
    public func contains(
        value: any ExportedKotlinPackages.kotlin.Comparable
    ) -> Swift.Bool {
        return kotlin_ranges_ClosedRange_contains__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_Comparable__(self.__externalRCRef(), value.__externalRCRef())
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_ranges_ClosedRange_isEmpty(self.__externalRCRef())
    }
    public static func ~=(
        this: Self,
        value: any ExportedKotlinPackages.kotlin.Comparable
    ) -> Swift.Bool {
        this.contains(value: value)
    }
}
extension ExportedKotlinPackages.kotlin.ranges.ClosedRange {
    public func contains(
        value: any ExportedKotlinPackages.kotlin.Comparable
    ) -> Swift.Bool {
        return kotlin_ranges_ClosedRange_contains__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_Comparable___direct(self.__externalRCRef(), value.__externalRCRef())
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_ranges_ClosedRange_isEmpty_direct(self.__externalRCRef())
    }
}
extension ExportedKotlinPackages.kotlin.Comparable where Self : ExportedKotlinPackages.kotlin.__Comparable {
    public static func <(
        this: Self,
        other: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        this._compareTo(other: other) < 0
    }
    public static func <=(
        this: Self,
        other: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        this._compareTo(other: other) <= 0
    }
    public static func >(
        this: Self,
        other: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        this._compareTo(other: other) > 0
    }
    public static func >=(
        this: Self,
        other: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        this._compareTo(other: other) >= 0
    }
    public func _compareTo(
        other: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Int32 {
        return kotlin_Comparable_compareTo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
    }
}
extension ExportedKotlinPackages.kotlin.Comparable {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.ranges.ClosedRange, ExportedKotlinPackages.kotlin.ranges.__ClosedRange where Wrapped : ExportedKotlinPackages.kotlin.ranges._ClosedRange {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.Comparable, ExportedKotlinPackages.kotlin.__Comparable where Wrapped : ExportedKotlinPackages.kotlin._Comparable {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.ranges._ClosedRange {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin._Comparable {
}
extension ExportedKotlinPackages.kotlin {
    public protocol Comparable: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin._Comparable {
        func _compareTo(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Int32
    }
    @objc(_Comparable)
    public protocol _Comparable {
    }
    public protocol __Comparable: KotlinRuntimeSupport._KotlinBridgeable {
    }
}
extension ExportedKotlinPackages.kotlin.ranges {
    public protocol ClosedRange: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.ranges._ClosedRange {
        var endInclusive: any ExportedKotlinPackages.kotlin.Comparable {
            get
        }
        var start: any ExportedKotlinPackages.kotlin.Comparable {
            get
        }
        func contains(
            value: any ExportedKotlinPackages.kotlin.Comparable
        ) -> Swift.Bool
        func isEmpty() -> Swift.Bool
    }
    @objc(_ClosedRange)
    public protocol _ClosedRange {
    }
    public protocol __ClosedRange: KotlinRuntimeSupport._KotlinBridgeable {
    }
}
@_cdecl("kotlin_Comparable_compareTo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_Comparable_compareTo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ other: Swift.UnsafeMutableRawPointer?) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any ExportedKotlinPackages.kotlin.Comparable
    let _result: Swift.Int32 = _self._compareTo(other: { switch other { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_ranges_ClosedRange_contains__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_Comparable____reverse_swift")
package func kotlin_ranges_ClosedRange_contains__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_Comparable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any ExportedKotlinPackages.kotlin.ranges.ClosedRange
    let _result: Swift.Bool = _self.contains(value: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: value) as! any ExportedKotlinPackages.kotlin.Comparable)
    return _result
}

@_cdecl("kotlin_ranges_ClosedRange_endInclusive_get__reverse_swift")
package func kotlin_ranges_ClosedRange_endInclusive_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any ExportedKotlinPackages.kotlin.ranges.ClosedRange
    let _result: any ExportedKotlinPackages.kotlin.Comparable = _self.endInclusive
    return _result.__externalRCRef()
}

@_cdecl("kotlin_ranges_ClosedRange_isEmpty__reverse_swift")
package func kotlin_ranges_ClosedRange_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any ExportedKotlinPackages.kotlin.ranges.ClosedRange
    let _result: Swift.Bool = _self.isEmpty()
    return _result
}

@_cdecl("kotlin_ranges_ClosedRange_start_get__reverse_swift")
package func kotlin_ranges_ClosedRange_start_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any ExportedKotlinPackages.kotlin.ranges.ClosedRange
    let _result: any ExportedKotlinPackages.kotlin.Comparable = _self.start
    return _result.__externalRCRef()
}

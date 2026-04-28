@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinStdlib
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.kotlin.ranges.ClosedRange where Self : KotlinRuntimeSupport._KotlinBridgeable {
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
}
extension ExportedKotlinPackages.kotlin.Comparable where Self : KotlinRuntimeSupport._KotlinBridgeable {
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
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.ranges.ClosedRange where Wrapped : ExportedKotlinPackages.kotlin.ranges._ClosedRange {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.Comparable where Wrapped : ExportedKotlinPackages.kotlin._Comparable {
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
}
@_cdecl("kotlin_Comparable_compareTo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
public func kotlin_Comparable_compareTo__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ other: Swift.UnsafeMutableRawPointer?) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any ExportedKotlinPackages.kotlin.Comparable
    let _result: Swift.Int32 = _self._compareTo(other: { switch other { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_ranges_ClosedRange_contains__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_Comparable____reverse_swift")
public func kotlin_ranges_ClosedRange_contains__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_Comparable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any ExportedKotlinPackages.kotlin.ranges.ClosedRange
    let _result: Swift.Bool = _self.contains(value: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: value) as! any ExportedKotlinPackages.kotlin.Comparable)
    return _result
}

@_cdecl("kotlin_ranges_ClosedRange_isEmpty__reverse_swift")
public func kotlin_ranges_ClosedRange_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any ExportedKotlinPackages.kotlin.ranges.ClosedRange
    let _result: Swift.Bool = _self.isEmpty()
    return _result
}

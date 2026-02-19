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
extension ExportedKotlinPackages.kotlin {
    public protocol Comparable: KotlinRuntime.KotlinBase {
        func _compareTo(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Int32
    }
    @objc(_Comparable)
    package protocol _Comparable {
    }
}
extension ExportedKotlinPackages.kotlin.ranges {
    public protocol ClosedRange: KotlinRuntime.KotlinBase {
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
    package protocol _ClosedRange {
    }
}

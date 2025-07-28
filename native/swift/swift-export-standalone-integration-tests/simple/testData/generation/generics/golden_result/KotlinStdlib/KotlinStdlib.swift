@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinStdlib
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.kotlin.Comparable where Self : KotlinRuntimeSupport._KotlinBridged {
    public static func <(
        this: Self,
        other: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        this._compareTo(other: other) < 0
    }
    public static func <=(
        this: Self,
        other: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        this._compareTo(other: other) <= 0
    }
    public static func >(
        this: Self,
        other: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        this._compareTo(other: other) > 0
    }
    public static func >=(
        this: Self,
        other: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        this._compareTo(other: other) >= 0
    }
    public func _compareTo(
        other: KotlinRuntime.KotlinBase?
    ) -> Swift.Int32 {
        return kotlin_Comparable_compareTo__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.Comparable where Wrapped : ExportedKotlinPackages.kotlin._Comparable {
}
extension ExportedKotlinPackages.kotlin {
    public protocol Comparable: KotlinRuntime.KotlinBase {
        func _compareTo(
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Int32
    }
    @objc(_Comparable)
    package protocol _Comparable {
    }
}

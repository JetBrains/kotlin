@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinStdlib
import KotlinRuntime
import KotlinRuntimeSupport

public extension ExportedKotlinPackages.kotlin {
    open class Enum: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public final class Companion: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public static var shared: ExportedKotlinPackages.kotlin.Enum.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.Enum.Companion.__createClassWrapper(externalRCRef: kotlin_Enum_Companion_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
        }
        public final var name: Swift.String {
            get {
                return kotlin_Enum_name_get(self.__externalRCRef())
            }
        }
        public final var ordinal: Swift.Int32 {
            get {
                return kotlin_Enum_ordinal_get(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        package init(
            name: Swift.String,
            ordinal: Swift.Int32
        ) {
            fatalError()
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Enum,
            other: ExportedKotlinPackages.kotlin.Enum
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Enum,
            other: ExportedKotlinPackages.kotlin.Enum
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.Enum,
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Enum,
            other: ExportedKotlinPackages.kotlin.Enum
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Enum,
            other: ExportedKotlinPackages.kotlin.Enum
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public final func _compareTo(
            other: ExportedKotlinPackages.kotlin.Enum
        ) -> Swift.Int32 {
            return kotlin_Enum_compareTo__TypesOfArguments__ExportedKotlinPackages_kotlin_Enum__(self.__externalRCRef(), other.__externalRCRef())
        }
        public final func equals(
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            return kotlin_Enum_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public final func hashCode() -> Swift.Int32 {
            return kotlin_Enum_hashCode(self.__externalRCRef())
        }
        open func toString() -> Swift.String {
            return kotlin_Enum_toString(self.__externalRCRef())
        }
    }
}

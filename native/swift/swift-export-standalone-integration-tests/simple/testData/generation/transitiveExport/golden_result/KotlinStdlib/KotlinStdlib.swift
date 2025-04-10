@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinStdlib
import KotlinRuntime
import KotlinRuntimeSupport

public extension ExportedKotlinPackages.kotlin.collections {
    open class ByteIterator: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    }
}
public extension ExportedKotlinPackages.kotlin {
    public final class ByteArray: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public var size: Swift.Int32 {
            get {
                return kotlin_ByteArray_size_get(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRef: Swift.UnsafeMutableRawPointer?
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public init(
            size: Swift.Int32
        ) {
            fatalError()
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> Swift.Int8
        ) {
            fatalError()
        }
        public func get(
            index: Swift.Int32
        ) -> Swift.Int8 {
            return kotlin_ByteArray_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        public func iterator() -> ExportedKotlinPackages.kotlin.collections.ByteIterator {
            fatalError()
        }
        public func set(
            index: Swift.Int32,
            value: Swift.Int8
        ) -> Swift.Void {
            return kotlin_ByteArray_set__TypesOfArguments__Swift_Int32_Swift_Int8__(self.__externalRCRef(), index, value)
        }
    }
}

@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib

public final class Accessor: KotlinRuntime.KotlinBase {
    public var x: ExportedKotlinPackages.kotlin.IntArray {
        get {
            return ExportedKotlinPackages.kotlin.IntArray.__createClassWrapper(externalRCRef: Accessor_x_get(self.__externalRCRef()))
        }
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    public func _get(
        i: Swift.Int32
    ) -> Swift.Int32 {
        return Accessor_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), i)
    }
    public subscript(
        i: Swift.Int32
    ) -> Swift.Int32 {
        get {
            _get(i: i)
        }
    }
}

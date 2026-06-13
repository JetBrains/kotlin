@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinStdlib
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.kotlinx.cinterop.ObjCObject where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension ExportedKotlinPackages.kotlinx.cinterop.ObjCObject {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.cinterop.ObjCObject where Wrapped : ExportedKotlinPackages.kotlinx.cinterop._ObjCObject {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.cinterop._ObjCObject {
}
extension ExportedKotlinPackages.kotlinx.cinterop {
    public protocol ObjCObject: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.cinterop._ObjCObject {
    }
    @objc(_ObjCObject)
    public protocol _ObjCObject {
    }
    open class ObjCObjectBase: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.cinterop.ObjCObject, ExportedKotlinPackages.kotlinx.cinterop._ObjCObject {
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}

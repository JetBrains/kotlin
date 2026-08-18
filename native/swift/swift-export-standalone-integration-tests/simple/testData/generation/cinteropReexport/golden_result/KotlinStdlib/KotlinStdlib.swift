@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinStdlib
import KotlinRuntime
import KotlinRuntimeSupport

@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.cinterop.ObjCObject where Self : ExportedKotlinPackages.kotlinx.cinterop.__ObjCObject {
}
extension ExportedKotlinPackages.kotlinx.cinterop.ObjCObject {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.cinterop.ObjCObject, ExportedKotlinPackages.kotlinx.cinterop.__ObjCObject where Wrapped : ExportedKotlinPackages.kotlinx.cinterop._ObjCObject {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.cinterop._ObjCObject {
}
extension ExportedKotlinPackages.kotlinx.cinterop {
    public protocol ObjCObject: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.cinterop._ObjCObject {
    }
    @objc(_ExportedKotlinPackages_kotlinx_cinterop_ObjCObject)
    public protocol _ObjCObject {
    }
    public protocol __ObjCObject: KotlinRuntimeSupport._KotlinBridgeable {
    }
    open class ObjCObjectBase: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.cinterop.ObjCObject, ExportedKotlinPackages.kotlinx.cinterop.__ObjCObject {
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}

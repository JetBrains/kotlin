@_implementationOnly import KotlinRuntimeSupportBridge
import KotlinRuntime

public struct KotlinError: Error {
    public var wrapped: KotlinRuntime.KotlinBase

    public init(wrapped: KotlinRuntime.KotlinBase) {
        self.wrapped = wrapped
    }
}

public protocol _KotlinBridgeable {
    init(__externalRCRefUnsafe: UnsafeMutableRawPointer!, options: KotlinBaseConstructionOptions)
    func __externalRCRef() -> UnsafeMutableRawPointer!
}

public class _KotlinExistential<Wrapped>: KotlinBase {

}

extension KotlinBase : _KotlinBridgeable {
}

extension NSObject {
    // FIXME: swap to @expose(C) when it's available or consider using @expose(Cxx)
    @objc(_Kotlin_SwiftExport_wrapIntoExistential:)
    private static func _kotlinGetExistentialType(markerType: AnyObject.Type) -> KotlinBase.Type {
        func wrap<T>(_ cls: T.Type) -> KotlinBase.Type {
            _KotlinExistential<T>.self
        }

        func openAndWrap(_ markerType: AnyObject.Type) -> KotlinBase.Type {
            return _openExistential(markerType, do: wrap)
        }

        return openAndWrap(markerType)
    }
}
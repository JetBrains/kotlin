@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinStdlib
import KotlinRuntime
import KotlinRuntimeSupport

public extension ExportedKotlinPackages.kotlin.collections.Iterator where Self : KotlinRuntimeSupport._KotlinBridged {
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_Iterator_hasNext(self.__externalRCRef())
    }
    public func next() -> KotlinRuntime.KotlinBase? {
        return { switch kotlin_collections_Iterator_next(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Iterator where Wrapped : ExportedKotlinPackages.kotlin.collections._Iterator {
}
public extension ExportedKotlinPackages.kotlin.collections {
    public protocol Iterator: KotlinRuntime.KotlinBase {
        func hasNext() -> Swift.Bool
        func next() -> KotlinRuntime.KotlinBase?
    }
    @objc(_Iterator)
    protocol _Iterator {
    }
}

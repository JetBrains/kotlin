import KotlinRuntime

public struct KotlinError: Error {
    public var wrapped: KotlinRuntime.KotlinBase

    public init(wrapped: KotlinRuntime.KotlinBase) {
        self.wrapped = wrapped
    }
}

public protocol _KotlinBridged: KotlinBase {}

public class _KotlinExistential<Wrapped>: KotlinBase & _KotlinBridged {

}


@_cdecl("kotlin_wrap_into_existential")
func _kotlinGetExistentialType(markerType: AnyObject.Type) -> KotlinBase.Type {
    func wrap<T>(_ cls: T.Type) -> KotlinBase.Type {
        _KotlinExistential<T>.self
    }

    func openAndWrap(_ markerType: AnyObject.Type) -> KotlinBase.Type {
        return _openExistential(markerType, do: wrap)
    }

    return openAndWrap(markerType)
}

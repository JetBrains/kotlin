@frozen
public enum Result_SealedType<T: KotlinRuntimeSupport._KotlinBridgeable> {
    case ok(T)
}
public typealias BoxAlias<T: KotlinRuntimeSupport._KotlinBridgeable> = Test.Box<T>
public protocol Repo<T>: KotlinRuntimeSupport._KotlinBridgeable {
    associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
    func get() -> T
}
open class Box<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase {
    public var value: T {
        get {
            stub()
        }
    }
    public init(
        value: T
    ) {
        stub()
    }
}
@inlinable
public func identity<T: KotlinRuntimeSupport._KotlinBridgeable>(
    value: T
) -> T where T : Test.Repo<Swift.Int32> {
    return value
}

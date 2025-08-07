import KotlinRuntime
import KotlinRuntimeSupport

public typealias Closure = () -> Swift.Void
public func typealias_demo(
    input: @escaping typealias_to_closure.Closure
) -> Swift.Never {
    fatalError()
}

import KotlinRuntime
import KotlinRuntimeSupport

@available(*, unavailable, message: "Declaration uses unsupported types")
public func badHiddenList() -> Swift.Never {
    fatalError()
}
@available(*, unavailable, message: "Declaration uses unsupported types")
public func badKClassList() -> Swift.Never {
    fatalError()
}

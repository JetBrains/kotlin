import KotlinRuntime
import KotlinRuntimeSupport

@discardableResult @available(*, unavailable, message: "Declaration uses unsupported types")
public func badHiddenList() -> Swift.Never {
    fatalError()
}
@discardableResult @available(*, unavailable, message: "Declaration uses unsupported types")
public func badKClassList() -> Swift.Never {
    fatalError()
}

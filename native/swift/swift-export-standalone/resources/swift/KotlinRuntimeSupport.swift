import KotlinRuntime

public struct KotlinError: Error {
    public var wrapped: KotlinRuntime.KotlinBase

    public init(wrapped: KotlinRuntime.KotlinBase) {
        self.wrapped = wrapped
    }
}

public protocol _KotlinBridged: KotlinBase {}



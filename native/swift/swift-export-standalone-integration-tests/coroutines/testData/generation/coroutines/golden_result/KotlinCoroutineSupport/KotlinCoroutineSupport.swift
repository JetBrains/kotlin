import KotlinRuntime
@_implementationOnly import KotlinCoroutineSupportBridge

/// A Bridge type for Job-like class in Kotlin
///
/// ## Discussion
/// This type is a manually bridged counterpart to SwiftJob type in Kotlin
/// It wraps `UnsafeCurrentTask` can communicates cancellation between it and kotlin world.
/// The value of this type should never outlive the task it wraps.
package final class KotlinTask: KotlinRuntime.KotlinBase {
    public convenience init(_ currentTask: UnsafeCurrentTask) {
        self.init { shouldCancel in
            defer { if shouldCancel { currentTask.cancel() } }
            return currentTask.isCancelled
        }
    }

    private init(
        cancellationCallback: @escaping (Swift.Bool) -> Swift.Bool
    ) {
        let __kt = __root___SwiftJob_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___SwiftJob_init_initialize(__kt, {
            let originalBlock = cancellationCallback
            return { arg0 in return originalBlock(arg0) }
        }())
    }

    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }

    public func cancelExternally() -> Swift.Void {
        return __root___SwiftJob_cancelExternally(self.__externalRCRef())
    }
}

import KotlinRuntime
import KotlinRuntimeSupport
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

public protocol _KotlinFlow: KotlinRuntime.KotlinBase, AsyncSequence { }

extension _KotlinFlow {
    public func makeAsyncIterator() -> KotlinFlowIterator {
        .init(flow: self)
    }
}

/// A typed wrapper around kotlinx.coroutines.flow.Flow
/// that preserves the element type as a Swift generic parameter.
public struct _KotlinTypedFlow<Element>: AsyncSequence {
    internal let _flow: any _KotlinFlow

    public init(_ flow: any _KotlinFlow) {
        self._flow = flow
    }

    /// Returns the underlying type-erased flow.
    public var wrapped: any _KotlinFlow {
        _flow
    }

    public func makeAsyncIterator() -> _KotlinTypedFlowIterator<Element> {
        _KotlinTypedFlowIterator(_flow.makeAsyncIterator())
    }
}

public struct _KotlinTypedFlowIterator<Element>: AsyncIteratorProtocol {
    private var _iterator: KotlinFlowIterator

    internal init(_ iterator: KotlinFlowIterator) {
        self._iterator = iterator
    }

    public mutating func next() async throws -> Element? {
        guard let raw = try await _iterator.next() else { return nil }
        return raw as! Element
    }
}

/// An async iterator type for kotlinx.coroutines.flow.Flow
///
/// ## Discussion
/// This type is a manually bridged counterpart to SwiftFlowIterator type in Kotlin
/// It simply maps `next()` calls to its implementation in Kotlin.
public final class KotlinFlowIterator: KotlinRuntime.KotlinBase, AsyncIteratorProtocol {
    public typealias Element = any KotlinRuntimeSupport._KotlinBridgeable
    public typealias Failure = any Error

    fileprivate init(flow: some _KotlinFlow) {
        let __kt = _kotlin_swift_SwiftFlowIterator_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        _kotlin_swift_SwiftFlowIterator_init_initialize(__kt, flow.__externalRCRef())
    }

    deinit {
        _kotlin_swift_SwiftFlowIterator_cancel(self.__externalRCRef())
    }

    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }

    public func next() async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { (nativeContinuation: UnsafeContinuation<(any KotlinRuntimeSupport._KotlinBridgeable)?, any Error>) in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: () = _kotlin_swift_SwiftFlowIterator_next(self.__externalRCRef(), { arg0 in
                                return {
                                    continuation(arg0.flatMap(KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef:)));
                                    return 0
                                }()
                        }, { arg0 in
                                return {
                                    exception(arg0.flatMap(KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef:)));
                                    return 0
                                }()
                        }, cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
}

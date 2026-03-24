import KotlinRuntime
import KotlinRuntimeSupport
@_implementationOnly import KotlinCoroutineSupportBridge

/// A Bridge type for Job-like class in Kotlin
///
/// ## Discussion
/// This type is a manually bridged counterpart to SwiftJob type in Kotlin
/// It wraps `UnsafeCurrentTask` can communicates cancellation between it and kotlin world.
/// The value of this type should never outlive the task it wraps.
@objc(KotlinTask)
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

    public func setCallback(_ callback: @escaping @convention(block) (Bool) -> Bool) {
        __root___SwiftJob_setCallback(self.__externalRCRef(), callback)
    }
}

public protocol KotlinFlow: KotlinRuntime.KotlinBase { }

public protocol KotlinTypedFlow<Element> {
    associatedtype Element

    var _flow: any KotlinFlow { get }
}

extension KotlinTypedFlow {
    public var wrapped: any KotlinFlow { _flow }

    public func asAsyncSequence() -> KotlinFlowSequence<Element> {
        KotlinFlowSequence(_flow)
    }
}

public struct _KotlinTypedFlowImpl<Element>: KotlinTypedFlow {
    public let _flow: any KotlinFlow

    public init(_ flow: any KotlinFlow) {
        self._flow = flow
    }
}

public protocol KotlinSharedFlow: KotlinFlow {
    var replayCache: [(any KotlinRuntimeSupport._KotlinBridgeable)?] { get }
}

public protocol KotlinTypedSharedFlow<Element>: KotlinTypedFlow { }

extension KotlinTypedSharedFlow {
    public var wrapped: any KotlinSharedFlow { _flow as! (any KotlinSharedFlow) }

    public var replayCache: [Element] { wrapped.replayCache as! [Element] }
}

public struct _KotlinTypedSharedFlowImpl<Element>: KotlinTypedSharedFlow {
    public let _flow: any KotlinFlow

    public init(_ flow: any KotlinSharedFlow) {
        self._flow = flow
    }
}

public protocol KotlinMutableSharedFlow: KotlinSharedFlow {
    var subscriptionCount: any KotlinTypedStateFlow<Int32> { get }
    func emit(value: (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws
    func resetReplayCache()
    func tryEmit(value: (any KotlinRuntimeSupport._KotlinBridgeable)?) -> Bool
}

public protocol KotlinTypedMutableSharedFlow<Element>: KotlinTypedSharedFlow { }

extension KotlinTypedMutableSharedFlow {
    public var wrapped: any KotlinMutableSharedFlow { _flow as! (any KotlinMutableSharedFlow) }

    public var subscriptionCount: any KotlinTypedStateFlow<Int32> {
        wrapped.subscriptionCount
    }

    public func emit(value: Element) async throws {
        try await wrapped.emit(value: value as! (any KotlinRuntimeSupport._KotlinBridgeable)?)
    }

    public func resetReplayCache() {
        wrapped.resetReplayCache()
    }

    public func tryEmit(value: Element) -> Bool {
        wrapped.tryEmit(value: value as! (any KotlinRuntimeSupport._KotlinBridgeable)?)
    }
}

public struct _KotlinTypedMutableSharedFlowImpl<Element>: KotlinTypedMutableSharedFlow {
    public let _flow: any KotlinFlow

    public init(_ flow: any KotlinMutableSharedFlow) {
        self._flow = flow
    }
}

public protocol KotlinStateFlow: KotlinSharedFlow {
    var value: (any KotlinRuntimeSupport._KotlinBridgeable)? { get }
}

public protocol KotlinTypedStateFlow<Element>: KotlinTypedSharedFlow { }

extension KotlinTypedStateFlow {
    public var wrapped: any KotlinStateFlow { _flow as! (any KotlinStateFlow) }

    public var value: Element { wrapped.value as! Element }
}

public struct _KotlinTypedStateFlowImpl<Element>: KotlinTypedStateFlow {
    public let _flow: any KotlinFlow

    public init(_ flow: any KotlinStateFlow) {
        self._flow = flow
    }
}

public protocol KotlinMutableStateFlow: KotlinStateFlow, KotlinMutableSharedFlow {
    var value: (any KotlinRuntimeSupport._KotlinBridgeable)? { get set }
    func compareAndSet(expect: (any KotlinRuntimeSupport._KotlinBridgeable)?, update: (any KotlinRuntimeSupport._KotlinBridgeable)?) -> Bool
}

public protocol KotlinTypedMutableStateFlow<Element>: KotlinTypedStateFlow, KotlinTypedMutableSharedFlow { }

extension KotlinTypedMutableStateFlow {
    public var wrapped: any KotlinMutableStateFlow { _flow as! (any KotlinMutableStateFlow) }

    public var value: Element {
        get { wrapped.value as! Element }
        nonmutating set { wrapped.value = newValue as! (any KotlinRuntimeSupport._KotlinBridgeable)? }
    }

    public func compareAndSet(expect: Element, update: Element) -> Bool {
        wrapped.compareAndSet(
            expect: expect as! (any KotlinRuntimeSupport._KotlinBridgeable)?,
            update: update as! (any KotlinRuntimeSupport._KotlinBridgeable)?
        )
    }
}

public struct _KotlinTypedMutableStateFlowImpl<Element>: KotlinTypedMutableStateFlow {
    public let _flow: any KotlinFlow

    public init(_ flow: any KotlinMutableStateFlow) {
        self._flow = flow
    }
}

/// An async sequence type for kotlinx.coroutines.flow.Flow
public struct KotlinFlowSequence<Element>: AsyncSequence {
    private let flow: any KotlinFlow

    fileprivate init(_ flow: any KotlinFlow) {
        self.flow = flow
    }

    public func makeAsyncIterator() -> KotlinFlowIterator<Element> {
        KotlinFlowIterator<Element>(flow)
    }
}

/// An async iterator type for kotlinx.coroutines.flow.Flow
///
/// ## Discussion
/// This type is a manually bridged counterpart to SwiftFlowIterator type in Kotlin
/// It simply maps `next()` calls to its implementation in Kotlin.
public final class KotlinFlowIterator<Element>: KotlinRuntime.KotlinBase, AsyncIteratorProtocol {
    public typealias Failure = any Error

    fileprivate init(_ flow: some KotlinFlow) {
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

    public func next() async throws -> Element? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { (nativeContinuation: UnsafeContinuation<Element?, any Error>) in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<Element>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: () = _kotlin_swift_SwiftFlowIterator_next(self.__externalRCRef(), { arg0, arg1 in
                                return {
                                    if arg0 {
                                        let element = arg1.flatMap(KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef:)) as! Element
                                        continuation(.some(element))
                                    } else {
                                        continuation(.none)
                                    }
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

@_exported import ExportedKotlinPackages
import KotlinRuntime
import KotlinRuntimeSupport
@_exported import KotlinCoroutineSupport
import KotlinxAtomicFu
@_implementationOnly import KotlinBridges_KotlinxCoroutinesCore
@_spi(kotlin$ExperimentalStdlibApi) import KotlinStdlib

public typealias intrinsics = ExportedKotlinPackages.kotlinx.coroutines.intrinsics
public typealias channels = ExportedKotlinPackages.kotlinx.coroutines.channels
public typealias flow = ExportedKotlinPackages.kotlinx.coroutines.flow
public typealias selects = ExportedKotlinPackages.kotlinx.coroutines.selects
public typealias `internal` = ExportedKotlinPackages.kotlinx.coroutines.`internal`
public typealias sync = ExportedKotlinPackages.kotlinx.coroutines.sync
@_spi(kotlinx$coroutines$InternalCoroutinesApi) @available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.JobSupport")
public typealias AbstractCoroutine = ExportedKotlinPackages.kotlinx.coroutines.AbstractCoroutine
public typealias CancellableContinuation = ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation
public typealias _CancellableContinuation = ExportedKotlinPackages.kotlinx.coroutines._CancellableContinuation
public typealias __CancellableContinuation = ExportedKotlinPackages.kotlinx.coroutines.__CancellableContinuation
@available(*, unavailable, message: "This is internal API and may be removed in the future releases") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
public typealias ChildHandle = ExportedKotlinPackages.kotlinx.coroutines.ChildHandle
public typealias _ChildHandle = ExportedKotlinPackages.kotlinx.coroutines._ChildHandle
@available(*, unavailable, message: "This is internal API and may be removed in the future releases") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
public typealias __ChildHandle = ExportedKotlinPackages.kotlinx.coroutines.__ChildHandle
@available(*, unavailable, message: "This is internal API and may be removed in the future releases") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
public typealias ChildJob = ExportedKotlinPackages.kotlinx.coroutines.ChildJob
public typealias _ChildJob = ExportedKotlinPackages.kotlinx.coroutines._ChildJob
@available(*, unavailable, message: "This is internal API and may be removed in the future releases") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
public typealias __ChildJob = ExportedKotlinPackages.kotlinx.coroutines.__ChildJob
public typealias CloseableCoroutineDispatcher = ExportedKotlinPackages.kotlinx.coroutines.CloseableCoroutineDispatcher
public typealias CompletableDeferred = ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred
public typealias _CompletableDeferred = ExportedKotlinPackages.kotlinx.coroutines._CompletableDeferred
public typealias __CompletableDeferred = ExportedKotlinPackages.kotlinx.coroutines.__CompletableDeferred
public typealias CompletableJob = ExportedKotlinPackages.kotlinx.coroutines.CompletableJob
public typealias _CompletableJob = ExportedKotlinPackages.kotlinx.coroutines._CompletableJob
public typealias __CompletableJob = ExportedKotlinPackages.kotlinx.coroutines.__CompletableJob
@_spi(kotlinx$coroutines$InternalCoroutinesApi)
public typealias CompletionHandlerException = ExportedKotlinPackages.kotlinx.coroutines.CompletionHandlerException
public typealias CoroutineDispatcher = ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher
public typealias CoroutineExceptionHandler = ExportedKotlinPackages.kotlinx.coroutines.CoroutineExceptionHandler
public typealias _CoroutineExceptionHandler = ExportedKotlinPackages.kotlinx.coroutines._CoroutineExceptionHandler
public typealias __CoroutineExceptionHandler = ExportedKotlinPackages.kotlinx.coroutines.__CoroutineExceptionHandler
public typealias CoroutineName = ExportedKotlinPackages.kotlinx.coroutines.CoroutineName
public typealias CoroutineScope = ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
public typealias _CoroutineScope = ExportedKotlinPackages.kotlinx.coroutines._CoroutineScope
public typealias __CoroutineScope = ExportedKotlinPackages.kotlinx.coroutines.__CoroutineScope
public typealias CoroutineStart = ExportedKotlinPackages.kotlinx.coroutines.CoroutineStart
public typealias Deferred = ExportedKotlinPackages.kotlinx.coroutines.Deferred
public typealias _Deferred = ExportedKotlinPackages.kotlinx.coroutines._Deferred
public typealias __Deferred = ExportedKotlinPackages.kotlinx.coroutines.__Deferred
@_spi(kotlinx$coroutines$InternalCoroutinesApi)
public typealias Delay = ExportedKotlinPackages.kotlinx.coroutines.Delay
public typealias _Delay = ExportedKotlinPackages.kotlinx.coroutines._Delay
@_spi(kotlinx$coroutines$InternalCoroutinesApi)
public typealias __Delay = ExportedKotlinPackages.kotlinx.coroutines.__Delay
public typealias Dispatchers = ExportedKotlinPackages.kotlinx.coroutines.Dispatchers
public typealias DisposableHandle = ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
public typealias _DisposableHandle = ExportedKotlinPackages.kotlinx.coroutines._DisposableHandle
public typealias __DisposableHandle = ExportedKotlinPackages.kotlinx.coroutines.__DisposableHandle
@_spi(kotlinx$coroutines$DelicateCoroutinesApi)
public typealias GlobalScope = ExportedKotlinPackages.kotlinx.coroutines.GlobalScope
public typealias Job = ExportedKotlinPackages.kotlinx.coroutines.Job
public typealias _Job = ExportedKotlinPackages.kotlinx.coroutines._Job
public typealias __Job = ExportedKotlinPackages.kotlinx.coroutines.__Job
@available(*, unavailable, message: "This is internal API and may be removed in the future releases") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
public typealias JobSupport = ExportedKotlinPackages.kotlinx.coroutines.JobSupport
public typealias MainCoroutineDispatcher = ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher
public typealias NonCancellable = ExportedKotlinPackages.kotlinx.coroutines.NonCancellable
@_spi(kotlinx$coroutines$InternalCoroutinesApi)
public typealias NonDisposableHandle = ExportedKotlinPackages.kotlinx.coroutines.NonDisposableHandle
@available(*, unavailable, message: "This is internal API and may be removed in the future releases") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
public typealias ParentJob = ExportedKotlinPackages.kotlinx.coroutines.ParentJob
public typealias _ParentJob = ExportedKotlinPackages.kotlinx.coroutines._ParentJob
@available(*, unavailable, message: "This is internal API and may be removed in the future releases") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
public typealias __ParentJob = ExportedKotlinPackages.kotlinx.coroutines.__ParentJob
public typealias Runnable = ExportedKotlinPackages.kotlinx.coroutines.Runnable
public typealias _Runnable = ExportedKotlinPackages.kotlinx.coroutines._Runnable
public typealias __Runnable = ExportedKotlinPackages.kotlinx.coroutines.__Runnable
@_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
public typealias TimeoutCancellationException = ExportedKotlinPackages.kotlinx.coroutines.TimeoutCancellationException
public typealias CancellationException = ExportedKotlinPackages.kotlinx.coroutines.CancellationException
public typealias CompletionHandler = ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
public final class _ExportedKotlinPackages_kotlinx_coroutines_CoroutineExceptionHandler_Key: KotlinRuntime.KotlinBase {
    public static var shared: KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_CoroutineExceptionHandler_Key {
        get {
            return KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_CoroutineExceptionHandler_Key.__createClassWrapper(externalRCRef: kotlinx_coroutines_CoroutineExceptionHandler_Key_get())
        }
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    private init() {
        fatalError()
    }
}
public final class _ExportedKotlinPackages_kotlinx_coroutines_Job_Key: KotlinRuntime.KotlinBase {
    public static var shared: KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_Job_Key {
        get {
            return KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_Job_Key.__createClassWrapper(externalRCRef: kotlinx_coroutines_Job_Key_get())
        }
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    private init() {
        fatalError()
    }
}
public final class _ExportedKotlinPackages_kotlinx_coroutines_channels_Channel_Factory: KotlinRuntime.KotlinBase {
    public var BUFFERED: Swift.Int32 {
        get {
            return kotlinx_coroutines_channels_Channel_Factory_BUFFERED_get(self.__externalRCRef())
        }
    }
    public var CONFLATED: Swift.Int32 {
        get {
            return kotlinx_coroutines_channels_Channel_Factory_CONFLATED_get(self.__externalRCRef())
        }
    }
    public var DEFAULT_BUFFER_PROPERTY_NAME: Swift.String {
        get {
            return kotlinx_coroutines_channels_Channel_Factory_DEFAULT_BUFFER_PROPERTY_NAME_get(self.__externalRCRef())
        }
    }
    public var RENDEZVOUS: Swift.Int32 {
        get {
            return kotlinx_coroutines_channels_Channel_Factory_RENDEZVOUS_get(self.__externalRCRef())
        }
    }
    public var UNLIMITED: Swift.Int32 {
        get {
            return kotlinx_coroutines_channels_Channel_Factory_UNLIMITED_get(self.__externalRCRef())
        }
    }
    public static var shared: KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_channels_Channel_Factory {
        get {
            return KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_channels_Channel_Factory.__createClassWrapper(externalRCRef: kotlinx_coroutines_channels_Channel_Factory_get())
        }
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    private init() {
        fatalError()
    }
}
public final class _ExportedKotlinPackages_kotlinx_coroutines_flow_SharingStarted_Companion: KotlinRuntime.KotlinBase {
    public var Eagerly: any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_SharingStarted_Companion_Eagerly_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted
        }
    }
    public var Lazily: any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_SharingStarted_Companion_Lazily_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted
        }
    }
    public static var shared: KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_flow_SharingStarted_Companion {
        get {
            return KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_flow_SharingStarted_Companion.__createClassWrapper(externalRCRef: kotlinx_coroutines_flow_SharingStarted_Companion_get())
        }
    }
    public func WhileSubscribed(
        stopTimeoutMillis: Swift.Int64,
        replayExpirationMillis: Swift.Int64
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_SharingStarted_Companion_WhileSubscribed__TypesOfArguments__Swift_Int64_Swift_Int64__(self.__externalRCRef(), stopTimeoutMillis, replayExpirationMillis), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    private init() {
        fatalError()
    }
}
public func cancellationException(
    message: Swift.String?,
    cause: ExportedKotlinPackages.kotlin.Throwable?
) -> ExportedKotlinPackages.kotlinx.coroutines.CancellationException {
    return ExportedKotlinPackages.kotlinx.coroutines.cancellationException(message: message, cause: cause)
}
public func completableDeferred(
    value: (any KotlinRuntimeSupport._KotlinBridgeable)?
) -> any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred {
    return ExportedKotlinPackages.kotlinx.coroutines.completableDeferred(value: value)
}
public func completableDeferred(
    parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)?
) -> any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred {
    return ExportedKotlinPackages.kotlinx.coroutines.completableDeferred(parent: parent)
}
public func coroutineExceptionHandler(
    handler: @escaping (any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext, ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void
) -> any ExportedKotlinPackages.kotlinx.coroutines.CoroutineExceptionHandler {
    return ExportedKotlinPackages.kotlinx.coroutines.coroutineExceptionHandler(handler: handler)
}
public func coroutineScope(
    context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
) -> any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope {
    return ExportedKotlinPackages.kotlinx.coroutines.coroutineScope(context: context)
}
public func job(
    parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)?
) -> any ExportedKotlinPackages.kotlinx.coroutines.CompletableJob {
    return ExportedKotlinPackages.kotlinx.coroutines.job(parent: parent)
}
public func MainScope() -> any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope {
    return ExportedKotlinPackages.kotlinx.coroutines.MainScope()
}
public func runnable(
    block: @escaping () -> Swift.Void
) -> any ExportedKotlinPackages.kotlinx.coroutines.Runnable {
    return ExportedKotlinPackages.kotlinx.coroutines.runnable(block: block)
}
public func supervisorJob(
    parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)?
) -> any ExportedKotlinPackages.kotlinx.coroutines.CompletableJob {
    return ExportedKotlinPackages.kotlinx.coroutines.supervisorJob(parent: parent)
}
public func awaitAll(
    deferreds: any ExportedKotlinPackages.kotlinx.coroutines.Deferred...
) async throws -> [(any KotlinRuntimeSupport._KotlinBridgeable)?] {
    try await withKotlinContinuation { continuation, exception, cancellation in
        let _: Bool = kotlinx_coroutines_awaitAll__TypesOfArguments__Swift_Array_anyU20ExportedKotlinPackages_kotlinx_coroutines_Deferred__Vararg___(deferreds, {
            let originalBlock: (Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>) -> Swift.Void = continuation
            return { (arg0: Any) in
                let _arg0: Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> = arg0 as! Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
                let _result = originalBlock(_arg0)
                return { _result; return true }()
            }
        }(), {
            let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
            return { (arg0: Swift.UnsafeMutableRawPointer?) in
                let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                let _result = originalBlock(_arg0)
                return { _result; return true }()
            }
        }(), cancellation.__externalRCRef())
    }
}
public func awaitCancellation() async throws -> Swift.Never {
    return try await ExportedKotlinPackages.kotlinx.coroutines.awaitCancellation()
}
public func coroutineScope(
    block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
    return try await ExportedKotlinPackages.kotlinx.coroutines.coroutineScope(block: block)
}
public func currentCoroutineContext() async throws -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
    return try await ExportedKotlinPackages.kotlinx.coroutines.currentCoroutineContext()
}
public func delay(
    timeMillis: Swift.Int64
) async throws -> Swift.Void {
    return try await ExportedKotlinPackages.kotlinx.coroutines.delay(timeMillis: timeMillis)
}
public func delay(
    duration: ExportedKotlinPackages.kotlin.time.Duration
) async throws -> Swift.Void {
    return try await ExportedKotlinPackages.kotlinx.coroutines.delay(duration: duration)
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi)
public func handleCoroutineException(
    context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
    exception: ExportedKotlinPackages.kotlin.Throwable
) -> Swift.Void {
    return ExportedKotlinPackages.kotlinx.coroutines.handleCoroutineException(context: context, exception: exception)
}
public func joinAll(
    jobs: any ExportedKotlinPackages.kotlinx.coroutines.Job...
) async throws -> Swift.Void {
    try await withKotlinContinuation { continuation, exception, cancellation in
        let _: Bool = kotlinx_coroutines_joinAll__TypesOfArguments__Swift_Array_anyU20ExportedKotlinPackages_kotlinx_coroutines_Job__Vararg___(jobs, {
            let originalBlock: (Swift.Void) -> Swift.Void = continuation
            return { (arg0: Swift.Bool) in
                let _arg0: Swift.Void = { arg0; return () }()
                let _result = originalBlock(_arg0)
                return { _result; return true }()
            }
        }(), {
            let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
            return { (arg0: Swift.UnsafeMutableRawPointer?) in
                let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                let _result = originalBlock(_arg0)
                return { _result; return true }()
            }
        }(), cancellation.__externalRCRef())
    }
}
public func newFixedThreadPoolContext(
    nThreads: Swift.Int32,
    name: Swift.String
) -> ExportedKotlinPackages.kotlinx.coroutines.CloseableCoroutineDispatcher {
    return ExportedKotlinPackages.kotlinx.coroutines.newFixedThreadPoolContext(nThreads: nThreads, name: name)
}
@_spi(kotlinx$coroutines$DelicateCoroutinesApi) @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
public func newSingleThreadContext(
    name: Swift.String
) -> ExportedKotlinPackages.kotlinx.coroutines.CloseableCoroutineDispatcher {
    return ExportedKotlinPackages.kotlinx.coroutines.newSingleThreadContext(name: name)
}
public func runBlocking(
    context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
    block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
    return ExportedKotlinPackages.kotlinx.coroutines.runBlocking(context: context, block: block)
}
public func supervisorScope(
    block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
    return try await ExportedKotlinPackages.kotlinx.coroutines.supervisorScope(block: block)
}
public func suspendCancellableCoroutine(
    block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation) -> Swift.Void
) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
    return try await ExportedKotlinPackages.kotlinx.coroutines.suspendCancellableCoroutine(block: block)
}
public func withContext(
    context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
    block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
    return try await ExportedKotlinPackages.kotlinx.coroutines.withContext(context: context, block: block)
}
public func withTimeout(
    timeMillis: Swift.Int64,
    block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
    return try await ExportedKotlinPackages.kotlinx.coroutines.withTimeout(timeMillis: timeMillis, block: block)
}
public func withTimeout(
    timeout: ExportedKotlinPackages.kotlin.time.Duration,
    block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
    return try await ExportedKotlinPackages.kotlinx.coroutines.withTimeout(timeout: timeout, block: block)
}
public func withTimeoutOrNull(
    timeMillis: Swift.Int64,
    block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
    return try await ExportedKotlinPackages.kotlinx.coroutines.withTimeoutOrNull(timeMillis: timeMillis, block: block)
}
public func withTimeoutOrNull(
    timeout: ExportedKotlinPackages.kotlin.time.Duration,
    block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
    return try await ExportedKotlinPackages.kotlinx.coroutines.withTimeoutOrNull(timeout: timeout, block: block)
}
public func yield() async throws -> Swift.Void {
    return try await ExportedKotlinPackages.kotlinx.coroutines.yield()
}
public func disposableHandle(
    function: @escaping () -> Swift.Void
) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
    return ExportedKotlinPackages.kotlinx.coroutines.disposableHandle(function: function)
}
extension ExportedKotlinPackages.kotlinx.coroutines {
    public enum CoroutineStart: KotlinRuntimeSupport._KotlinBridgeable, Swift.CaseIterable, Swift.LosslessStringConvertible, Swift.RawRepresentable {
        case DEFAULT
        case LAZY
        case ATOMIC
        case UNDISPATCHED
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public var isLazy: Swift.Bool {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return kotlinx_coroutines_CoroutineStart_isLazy_get(self.__externalRCRef())
            }
        }
        public var description: Swift.String {
            get {
                switch self {
                case .DEFAULT: "DEFAULT"
                case .LAZY: "LAZY"
                case .ATOMIC: "ATOMIC"
                case .UNDISPATCHED: "UNDISPATCHED"
                default: fatalError()
                }
            }
        }
        public var rawValue: Swift.Int32 {
            get {
                switch self {
                case .DEFAULT: 0
                case .LAZY: 1
                case .ATOMIC: 2
                case .UNDISPATCHED: 3
                default: fatalError()
                }
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public func callAsFunction(
            block: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?,
            receiver: (any KotlinRuntimeSupport._KotlinBridgeable)?,
            completion: any ExportedKotlinPackages.kotlin.coroutines.Continuation
        ) -> Swift.Void {
            return { kotlinx_coroutines_CoroutineStart_invoke__TypesOfArguments__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation__(self.__externalRCRef(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
                return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                    }()
                    let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                    }()
                    let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                    let _result = withKotlinTask(_continuation, _exception, _cancellation){
                        try await originalBlock(_arg0)
                    }
                    return { _result; return true }()
                }
            }(), receiver.map { it in it.__externalRCRef() } ?? nil, completion.__externalRCRef()); return () }()
        }
        public init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer!,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            switch kotlinx_coroutines_CoroutineStart_ordinal(__externalRCRefUnsafe) {
            case 0: self = .DEFAULT
            case 1: self = .LAZY
            case 2: self = .ATOMIC
            case 3: self = .UNDISPATCHED
            default: fatalError()
            }
        }
        public func __externalRCRef() -> Swift.UnsafeMutableRawPointer! {
            return switch self {
            case .DEFAULT: kotlinx_coroutines_CoroutineStart_DEFAULT()
            case .LAZY: kotlinx_coroutines_CoroutineStart_LAZY()
            case .ATOMIC: kotlinx_coroutines_CoroutineStart_ATOMIC()
            case .UNDISPATCHED: kotlinx_coroutines_CoroutineStart_UNDISPATCHED()
            default: fatalError()
            }
        }
        public init?(
            _ description: Swift.String
        ) {
            switch description {
            case "DEFAULT": self = .DEFAULT
            case "LAZY": self = .LAZY
            case "ATOMIC": self = .ATOMIC
            case "UNDISPATCHED": self = .UNDISPATCHED
            default: return nil
            }
        }
        public init?(
            rawValue: Swift.Int32
        ) {
            guard 0..<4 ~= rawValue else { return nil }
            self = CoroutineStart.allCases[Int(rawValue)]
        }
    }
    public typealias CancellationException = ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException
    public typealias CompletionHandler = (ExportedKotlinPackages.kotlin.Throwable?) -> Swift.Void
    public protocol CancellableContinuation: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.coroutines.Continuation, ExportedKotlinPackages.kotlinx.coroutines._CancellableContinuation {
        var isActive: Swift.Bool {
            get
        }
        var isCancelled: Swift.Bool {
            get
        }
        var isCompleted: Swift.Bool {
            get
        }
        func cancel(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) -> Swift.Bool
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func completeResume(
            token: any KotlinRuntimeSupport._KotlinBridgeable
        ) -> Swift.Void
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func initCancellability() -> Swift.Void
        func invokeOnCancellation(
            handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
        ) -> Swift.Void
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        func resume(
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?,
            onCancellation: ((ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void)?
        ) -> Swift.Void
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func tryResume(
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?,
            idempotent: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func tryResume(
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?,
            idempotent: (any KotlinRuntimeSupport._KotlinBridgeable)?,
            onCancellation: ((ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func tryResumeWithException(
            exception: ExportedKotlinPackages.kotlin.Throwable
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        func resumeUndispatched(
            _ receiver: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher,
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        func resumeUndispatchedWithException(
            _ receiver: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher,
            exception: ExportedKotlinPackages.kotlin.Throwable
        ) -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_CancellableContinuation)
    public protocol _CancellableContinuation: ExportedKotlinPackages.kotlin.coroutines._Continuation {
    }
    public protocol __CancellableContinuation: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.coroutines.__Continuation {
    }
    @available(*, unavailable, message: "This is internal API and may be removed in the future releases") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol ChildHandle: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        var parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)? {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func childCancelled(
            cause: ExportedKotlinPackages.kotlin.Throwable
        ) -> Swift.Bool
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_ChildHandle)
    public protocol _ChildHandle: ExportedKotlinPackages.kotlinx.coroutines._DisposableHandle {
    }
    @available(*, unavailable, message: "This is internal API and may be removed in the future releases") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol __ChildHandle: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.__DisposableHandle {
    }
    @available(*, unavailable, message: "This is internal API and may be removed in the future releases") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol ChildJob: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.Job {
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_ChildJob)
    public protocol _ChildJob: ExportedKotlinPackages.kotlinx.coroutines._Job {
    }
    @available(*, unavailable, message: "This is internal API and may be removed in the future releases") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol __ChildJob: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.__Job {
    }
    public protocol CompletableDeferred: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.Deferred, ExportedKotlinPackages.kotlinx.coroutines._CompletableDeferred {
        func complete(
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func completeExceptionally(
            exception: ExportedKotlinPackages.kotlin.Throwable
        ) -> Swift.Bool
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_CompletableDeferred)
    public protocol _CompletableDeferred: ExportedKotlinPackages.kotlinx.coroutines._Deferred {
    }
    public protocol __CompletableDeferred: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.__Deferred {
    }
    public protocol CompletableJob: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.Job, ExportedKotlinPackages.kotlinx.coroutines._CompletableJob {
        func complete() -> Swift.Bool
        func completeExceptionally(
            exception: ExportedKotlinPackages.kotlin.Throwable
        ) -> Swift.Bool
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_CompletableJob)
    public protocol _CompletableJob: ExportedKotlinPackages.kotlinx.coroutines._Job {
    }
    public protocol __CompletableJob: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.__Job {
    }
    public protocol CoroutineExceptionHandler: KotlinRuntime.KotlinBase, KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element, ExportedKotlinPackages.kotlinx.coroutines._CoroutineExceptionHandler {
        func handleException(
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
            exception: ExportedKotlinPackages.kotlin.Throwable
        ) -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_CoroutineExceptionHandler)
    public protocol _CoroutineExceptionHandler: KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
    }
    public protocol __CoroutineExceptionHandler: KotlinRuntimeSupport._KotlinBridgeable, KotlinStdlib.___ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
    }
    public protocol CoroutineScope: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines._CoroutineScope {
        var coroutineContext: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
            get
        }
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope)
    public protocol _CoroutineScope {
    }
    public protocol __CoroutineScope: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol Deferred: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.Job, ExportedKotlinPackages.kotlinx.coroutines._Deferred {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        var onAwait: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1 {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get
        }
        func `await`() async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        func getCompleted() -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        func getCompletionExceptionOrNull() -> ExportedKotlinPackages.kotlin.Throwable?
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_Deferred)
    public protocol _Deferred: ExportedKotlinPackages.kotlinx.coroutines._Job {
    }
    public protocol __Deferred: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.__Job {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol Delay: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines._Delay {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func invokeOnTimeout(
            timeMillis: Swift.Int64,
            block: any ExportedKotlinPackages.kotlinx.coroutines.Runnable,
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_Delay)
    public protocol _Delay {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol __Delay: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol DisposableHandle: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines._DisposableHandle {
        func dispose() -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_DisposableHandle)
    public protocol _DisposableHandle {
    }
    public protocol __DisposableHandle: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol Job: KotlinRuntime.KotlinBase, KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element, ExportedKotlinPackages.kotlinx.coroutines._Job {
        var children: any ExportedKotlinPackages.kotlin.sequences.Sequence {
            get
        }
        var isActive: Swift.Bool {
            get
        }
        var isCancelled: Swift.Bool {
            get
        }
        var isCompleted: Swift.Bool {
            get
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        var onJoin: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0 {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get
        }
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        var parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)? {
            @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
            get
        }
        func cancel(
            cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
        ) -> Swift.Void
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func getCancellationException() -> ExportedKotlinPackages.kotlinx.coroutines.CancellationException
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func invokeOnCompletion(
            onCancelling: Swift.Bool,
            invokeImmediately: Swift.Bool,
            handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
        func invokeOnCompletion(
            handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
        func join() async throws -> Swift.Void
        func start() -> Swift.Bool
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_Job)
    public protocol _Job: KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
    }
    public protocol __Job: KotlinRuntimeSupport._KotlinBridgeable, KotlinStdlib.___ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
    }
    @available(*, unavailable, message: "This is internal API and may be removed in the future releases") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol ParentJob: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.Job {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func getChildJobCancellationCause() -> ExportedKotlinPackages.kotlinx.coroutines.CancellationException
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_ParentJob)
    public protocol _ParentJob: ExportedKotlinPackages.kotlinx.coroutines._Job {
    }
    @available(*, unavailable, message: "This is internal API and may be removed in the future releases") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol __ParentJob: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.__Job {
    }
    public protocol Runnable: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines._Runnable {
        func run() -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_Runnable)
    public protocol _Runnable {
    }
    public protocol __Runnable: KotlinRuntimeSupport._KotlinBridgeable {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi) @available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.JobSupport")
    open class AbstractCoroutine: ExportedKotlinPackages.kotlinx.coroutines.JobSupport, ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope, ExportedKotlinPackages.kotlinx.coroutines.__CoroutineScope {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final var context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_AbstractCoroutine_context_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open var coroutineContext: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_AbstractCoroutine_coroutineContext_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open override var isActive: Swift.Bool {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return kotlinx_coroutines_AbstractCoroutine_isActive_get(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func resumeWith(
            result: ExportedKotlinPackages.kotlin.Result
        ) -> Swift.Void {
            return { kotlinx_coroutines_AbstractCoroutine_resumeWith__TypesOfArguments__ExportedKotlinPackages_kotlin_Result__(self.__externalRCRef(), result.__externalRCRef()); return () }()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func start(
            start: ExportedKotlinPackages.kotlinx.coroutines.CoroutineStart,
            receiver: (any KotlinRuntimeSupport._KotlinBridgeable)?,
            block: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void {
            return { kotlinx_coroutines_AbstractCoroutine_start__TypesOfArguments__ExportedKotlinPackages_kotlinx_coroutines_CoroutineStart_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), start.__externalRCRef(), receiver.map { it in it.__externalRCRef() } ?? nil, {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
                return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                    }()
                    let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                    }()
                    let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                    let _result = withKotlinTask(_continuation, _exception, _cancellation){
                        try await originalBlock(_arg0)
                    }
                    return { _result; return true }()
                }
            }()); return () }()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public init(
            parentContext: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
            initParentJob: Swift.Bool,
            active: Swift.Bool
        ) {
            precondition(Self.self != ExportedKotlinPackages.kotlinx.coroutines.AbstractCoroutine.self, "ExportedKotlinPackages.kotlinx.coroutines.AbstractCoroutine is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_AbstractCoroutine_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Bool_Swift_Bool__(__kt, parentContext.__externalRCRef(), initParentJob, active); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class CloseableCoroutineDispatcher: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher {
        open func close() -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.CloseableCoroutineDispatcher.self {
                return { kotlinx_coroutines_CloseableCoroutineDispatcher_close(self.__externalRCRef()); return () }()
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlinx.coroutines.CloseableCoroutineDispatcher.close': a Swift subclass must override it and must not call super.")
            }
        }
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlinx.coroutines.CloseableCoroutineDispatcher.self, "ExportedKotlinPackages.kotlinx.coroutines.CloseableCoroutineDispatcher is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_CloseableCoroutineDispatcher_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public final class CompletionHandlerException: ExportedKotlinPackages.kotlin.RuntimeException {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public init(
            message: Swift.String,
            cause: ExportedKotlinPackages.kotlin.Throwable
        ) {
            let __kt = kotlinx_coroutines_CompletionHandlerException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_CompletionHandlerException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_ExportedKotlinPackages_kotlin_Throwable__(__kt, message, cause.__externalRCRef()); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class CoroutineDispatcher: ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextElement, ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor, ExportedKotlinPackages.kotlin.coroutines.__ContinuationInterceptor {
        @_spi(kotlin$ExperimentalStdlibApi)
        public final class Key: ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextKey {
            @_spi(kotlin$ExperimentalStdlibApi)
            public static var shared: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.Key {
                @_spi(kotlin$ExperimentalStdlibApi)
                get {
                    return ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.Key.__createClassWrapper(externalRCRef: kotlinx_coroutines_CoroutineDispatcher_Key_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
                fatalError()
            }
        }
        open func dispatch(
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
            block: any ExportedKotlinPackages.kotlinx.coroutines.Runnable
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.self {
                return { kotlinx_coroutines_CoroutineDispatcher_dispatch__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_anyU20ExportedKotlinPackages_kotlinx_coroutines_Runnable__(self.__externalRCRef(), context.__externalRCRef(), block.__externalRCRef()); return () }()
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.dispatch': a Swift subclass must override it and must not call super.")
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func dispatchYield(
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
            block: any ExportedKotlinPackages.kotlinx.coroutines.Runnable
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.self {
                return { kotlinx_coroutines_CoroutineDispatcher_dispatchYield__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_anyU20ExportedKotlinPackages_kotlinx_coroutines_Runnable__(self.__externalRCRef(), context.__externalRCRef(), block.__externalRCRef()); return () }()
            } else {
                return { kotlinx_coroutines_CoroutineDispatcher_dispatchYield__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_anyU20ExportedKotlinPackages_kotlinx_coroutines_Runnable___direct(self.__externalRCRef(), context.__externalRCRef(), block.__externalRCRef()); return () }()
            }
        }
        public final func interceptContinuation(
            continuation: any ExportedKotlinPackages.kotlin.coroutines.Continuation
        ) -> any ExportedKotlinPackages.kotlin.coroutines.Continuation {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_CoroutineDispatcher_interceptContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation__(self.__externalRCRef(), continuation.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.Continuation.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.Continuation
        }
        open func isDispatchNeeded(
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
        ) -> Swift.Bool {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.self {
                return kotlinx_coroutines_CoroutineDispatcher_isDispatchNeeded__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(self.__externalRCRef(), context.__externalRCRef())
            } else {
                return kotlinx_coroutines_CoroutineDispatcher_isDispatchNeeded__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext___direct(self.__externalRCRef(), context.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        open func limitedParallelism(
            parallelism: Swift.Int32
        ) -> ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.self {
                return ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: kotlinx_coroutines_CoroutineDispatcher_limitedParallelism__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), parallelism))
            } else {
                return ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: kotlinx_coroutines_CoroutineDispatcher_limitedParallelism__TypesOfArguments__Swift_Int32___direct(self.__externalRCRef(), parallelism))
            }
        }
        @available(*, unavailable, message: "Operator '+' on two CoroutineDispatcher objects is meaningless. CoroutineDispatcher is a coroutine context element and `+` is a set-sum operator for coroutine contexts. The dispatcher to the right of `+` just replaces the dispatcher to the left.")
        public final func _plus(
            other: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher
        ) -> ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher {
            fatalError()
        }
        @available(*, unavailable, message: "Operator '+' on two CoroutineDispatcher objects is meaningless. CoroutineDispatcher is a coroutine context element and `+` is a set-sum operator for coroutine contexts. The dispatcher to the right of `+` just replaces the dispatcher to the left.")
        public static func +(
            this: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher,
            other: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher
        ) -> ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher {
            fatalError()
        }
        public final func releaseInterceptedContinuation(
            continuation: any ExportedKotlinPackages.kotlin.coroutines.Continuation
        ) -> Swift.Void {
            return { kotlinx_coroutines_CoroutineDispatcher_releaseInterceptedContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation__(self.__externalRCRef(), continuation.__externalRCRef()); return () }()
        }
        open func toString() -> Swift.String {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.self {
                return kotlinx_coroutines_CoroutineDispatcher_toString(self.__externalRCRef())
            } else {
                return kotlinx_coroutines_CoroutineDispatcher_toString_direct(self.__externalRCRef())
            }
        }
        public init() {
            precondition(Self.self != ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.self, "ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_CoroutineDispatcher_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class CoroutineName: ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextElement {
        public final class Key: KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.kotlinx.coroutines.CoroutineName.Key {
                get {
                    return ExportedKotlinPackages.kotlinx.coroutines.CoroutineName.Key.__createClassWrapper(externalRCRef: kotlinx_coroutines_CoroutineName_Key_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
                fatalError()
            }
        }
        public var name: Swift.String {
            get {
                return kotlinx_coroutines_CoroutineName_name_get(self.__externalRCRef())
            }
        }
        public func copy(
            name: Swift.String
        ) -> ExportedKotlinPackages.kotlinx.coroutines.CoroutineName {
            return ExportedKotlinPackages.kotlinx.coroutines.CoroutineName.__createClassWrapper(externalRCRef: kotlinx_coroutines_CoroutineName_copy__TypesOfArguments__Swift_String__(self.__externalRCRef(), name))
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlinx_coroutines_CoroutineName_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlinx.coroutines.CoroutineName,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func hashCode() -> Swift.Int32 {
            return kotlinx_coroutines_CoroutineName_hashCode(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlinx_coroutines_CoroutineName_toString(self.__externalRCRef())
        }
        public init(
            name: Swift.String
        ) {
            let __kt = kotlinx_coroutines_CoroutineName_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_CoroutineName_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(__kt, name); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class Dispatchers: KotlinRuntime.KotlinBase {
        public var Default: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher {
            get {
                return ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: kotlinx_coroutines_Dispatchers_Default_get(self.__externalRCRef()))
            }
        }
        public var Main: ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher {
            get {
                return ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher.__createClassWrapper(externalRCRef: kotlinx_coroutines_Dispatchers_Main_get(self.__externalRCRef()))
            }
        }
        public var Unconfined: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher {
            get {
                return ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: kotlinx_coroutines_Dispatchers_Unconfined_get(self.__externalRCRef()))
            }
        }
        public static var shared: ExportedKotlinPackages.kotlinx.coroutines.Dispatchers {
            get {
                return ExportedKotlinPackages.kotlinx.coroutines.Dispatchers.__createClassWrapper(externalRCRef: kotlinx_coroutines_Dispatchers_get())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        private init() {
            fatalError()
        }
    }
    @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
    public final class GlobalScope: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope, ExportedKotlinPackages.kotlinx.coroutines.__CoroutineScope {
        @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
        public var coroutineContext: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
            @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_GlobalScope_coroutineContext_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
            }
        }
        @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
        public static var shared: ExportedKotlinPackages.kotlinx.coroutines.GlobalScope {
            @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
            get {
                return ExportedKotlinPackages.kotlinx.coroutines.GlobalScope.__createClassWrapper(externalRCRef: kotlinx_coroutines_GlobalScope_get())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        private init() {
            fatalError()
        }
    }
    @available(*, unavailable, message: "This is internal API and may be removed in the future releases") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    open class JobSupport: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.Job, ExportedKotlinPackages.kotlinx.coroutines.__Job, ExportedKotlinPackages.kotlinx.coroutines.ChildJob, ExportedKotlinPackages.kotlinx.coroutines.__ChildJob, ExportedKotlinPackages.kotlinx.coroutines.ParentJob, ExportedKotlinPackages.kotlinx.coroutines.__ParentJob {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final var children: any ExportedKotlinPackages.kotlin.sequences.Sequence {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_JobSupport_children_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.sequences.Sequence.Type.self) as! any ExportedKotlinPackages.kotlin.sequences.Sequence
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open var isActive: Swift.Bool {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return kotlinx_coroutines_JobSupport_isActive_get(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final var isCancelled: Swift.Bool {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return kotlinx_coroutines_JobSupport_isCancelled_get(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final var isCompleted: Swift.Bool {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return kotlinx_coroutines_JobSupport_isCompleted_get(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final var isCompletedExceptionally: Swift.Bool {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return kotlinx_coroutines_JobSupport_isCompletedExceptionally_get(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final var key: any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_JobSupport_key_get(self.__externalRCRef()), conformsTo: KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key.Type.self) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final var onJoin: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0 {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_JobSupport_onJoin_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0
            }
        }
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi) @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open var parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)? {
            @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi) @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return { switch kotlinx_coroutines_JobSupport_parent_get(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job; } }()
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi) @available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ChildJob, ExportedKotlinPackages.kotlinx.coroutines.ChildHandle")
        public final func attachChild(
            child: any ExportedKotlinPackages.kotlinx.coroutines.ChildJob
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.ChildHandle {
            fatalError()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func cancel(
            cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
        ) -> Swift.Void {
            return { kotlinx_coroutines_JobSupport_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException___(self.__externalRCRef(), cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func cancelCoroutine(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) -> Swift.Bool {
            return kotlinx_coroutines_JobSupport_cancelCoroutine__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(self.__externalRCRef(), cause.map { it in it.__externalRCRef() } ?? nil)
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func cancelInternal(
            cause: ExportedKotlinPackages.kotlin.Throwable
        ) -> Swift.Void {
            return { kotlinx_coroutines_JobSupport_cancelInternal__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable__(self.__externalRCRef(), cause.__externalRCRef()); return () }()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func childCancelled(
            cause: ExportedKotlinPackages.kotlin.Throwable
        ) -> Swift.Bool {
            return kotlinx_coroutines_JobSupport_childCancelled__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable__(self.__externalRCRef(), cause.__externalRCRef())
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func getCancellationException() -> ExportedKotlinPackages.kotlinx.coroutines.CancellationException {
            return ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException.__createClassWrapper(externalRCRef: kotlinx_coroutines_JobSupport_getCancellationException(self.__externalRCRef()))
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func getChildJobCancellationCause() -> ExportedKotlinPackages.kotlinx.coroutines.CancellationException {
            return ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException.__createClassWrapper(externalRCRef: kotlinx_coroutines_JobSupport_getChildJobCancellationCause(self.__externalRCRef()))
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func getCompletionExceptionOrNull() -> ExportedKotlinPackages.kotlin.Throwable? {
            return { switch kotlinx_coroutines_JobSupport_getCompletionExceptionOrNull(self.__externalRCRef()) { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func invokeOnCompletion(
            onCancelling: Swift.Bool,
            invokeImmediately: Swift.Bool,
            handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_JobSupport_invokeOnCompletion__TypesOfArguments__Swift_Bool_Swift_Bool_U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), onCancelling, invokeImmediately, {
                let originalBlock: (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void = handler
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable> = { switch arg0 { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func invokeOnCompletion(
            handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_JobSupport_invokeOnCompletion__TypesOfArguments__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), {
                let originalBlock: (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void = handler
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable> = { switch arg0 { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func join() async throws -> Swift.Void {
            try await withKotlinContinuation { continuation, exception, cancellation in
                let _: Bool = kotlinx_coroutines_JobSupport_join(self.__externalRCRef(), {
                    let originalBlock: (Swift.Void) -> Swift.Void = continuation
                    return { (arg0: Swift.Bool) in
                        let _arg0: Swift.Void = { arg0; return () }()
                        let _result = originalBlock(_arg0)
                        return { _result; return true }()
                    }
                }(), {
                    let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                    return { (arg0: Swift.UnsafeMutableRawPointer?) in
                        let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                        let _result = originalBlock(_arg0)
                        return { _result; return true }()
                    }
                }(), cancellation.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi) @available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ParentJob")
        public final func parentCancelled(
            parentJob: any ExportedKotlinPackages.kotlinx.coroutines.ParentJob
        ) -> Swift.Void {
            fatalError()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func start() -> Swift.Bool {
            return kotlinx_coroutines_JobSupport_start(self.__externalRCRef())
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func toDebugString() -> Swift.String {
            return kotlinx_coroutines_JobSupport_toDebugString(self.__externalRCRef())
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func toString() -> Swift.String {
            return kotlinx_coroutines_JobSupport_toString(self.__externalRCRef())
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public init(
            active: Swift.Bool
        ) {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlinx.coroutines.JobSupport.self {
                 __kt = kotlinx_coroutines_JobSupport_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_JobSupport_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(__kt, active); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class MainCoroutineDispatcher: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher {
        open var immediate: ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher {
            get {
                if Self.self == ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher.self {
                    return ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher.__createClassWrapper(externalRCRef: kotlinx_coroutines_MainCoroutineDispatcher_immediate_get(self.__externalRCRef()))
                } else {
                    fatalError("Cannot invoke the inherited implementation of abstract property 'ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher.immediate': a Swift subclass must override it and must not call super.")
                }
            }
        }
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        open override func limitedParallelism(
            parallelism: Swift.Int32
        ) -> ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher.self {
                return ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: kotlinx_coroutines_MainCoroutineDispatcher_limitedParallelism__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), parallelism))
            } else {
                return ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: kotlinx_coroutines_MainCoroutineDispatcher_limitedParallelism__TypesOfArguments__Swift_Int32___direct(self.__externalRCRef(), parallelism))
            }
        }
        open override func toString() -> Swift.String {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher.self {
                return kotlinx_coroutines_MainCoroutineDispatcher_toString(self.__externalRCRef())
            } else {
                return kotlinx_coroutines_MainCoroutineDispatcher_toString_direct(self.__externalRCRef())
            }
        }
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher.self, "ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_MainCoroutineDispatcher_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class NonCancellable: ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextElement, ExportedKotlinPackages.kotlinx.coroutines.Job, ExportedKotlinPackages.kotlinx.coroutines.__Job {
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")
        public var children: any ExportedKotlinPackages.kotlin.sequences.Sequence {
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_NonCancellable_children_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.sequences.Sequence.Type.self) as! any ExportedKotlinPackages.kotlin.sequences.Sequence
            }
        }
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")
        public var isActive: Swift.Bool {
            get {
                return kotlinx_coroutines_NonCancellable_isActive_get(self.__externalRCRef())
            }
        }
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")
        public var isCancelled: Swift.Bool {
            get {
                return kotlinx_coroutines_NonCancellable_isCancelled_get(self.__externalRCRef())
            }
        }
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")
        public var isCompleted: Swift.Bool {
            get {
                return kotlinx_coroutines_NonCancellable_isCompleted_get(self.__externalRCRef())
            }
        }
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public var onJoin: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0 {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_NonCancellable_onJoin_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0
            }
        }
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited") @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        public var parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)? {
            @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
            get {
                return { switch kotlinx_coroutines_NonCancellable_parent_get(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job; } }()
            }
        }
        public static var shared: ExportedKotlinPackages.kotlinx.coroutines.NonCancellable {
            get {
                return ExportedKotlinPackages.kotlinx.coroutines.NonCancellable.__createClassWrapper(externalRCRef: kotlinx_coroutines_NonCancellable_get())
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi) @available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ChildJob, ExportedKotlinPackages.kotlinx.coroutines.ChildHandle")
        public func attachChild(
            child: any ExportedKotlinPackages.kotlinx.coroutines.ChildJob
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.ChildHandle {
            fatalError()
        }
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")
        public func cancel(
            cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
        ) -> Swift.Void {
            return { kotlinx_coroutines_NonCancellable_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException___(self.__externalRCRef(), cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public func getCancellationException() -> ExportedKotlinPackages.kotlinx.coroutines.CancellationException {
            return ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException.__createClassWrapper(externalRCRef: kotlinx_coroutines_NonCancellable_getCancellationException(self.__externalRCRef()))
        }
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public func invokeOnCompletion(
            onCancelling: Swift.Bool,
            invokeImmediately: Swift.Bool,
            handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_NonCancellable_invokeOnCompletion__TypesOfArguments__Swift_Bool_Swift_Bool_U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), onCancelling, invokeImmediately, {
                let originalBlock: (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void = handler
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable> = { switch arg0 { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
        }
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")
        public func invokeOnCompletion(
            handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_NonCancellable_invokeOnCompletion__TypesOfArguments__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), {
                let originalBlock: (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void = handler
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable> = { switch arg0 { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
        }
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")
        public func join() async throws -> Swift.Void {
            try await withKotlinContinuation { continuation, exception, cancellation in
                let _: Bool = kotlinx_coroutines_NonCancellable_join(self.__externalRCRef(), {
                    let originalBlock: (Swift.Void) -> Swift.Void = continuation
                    return { (arg0: Swift.Bool) in
                        let _arg0: Swift.Void = { arg0; return () }()
                        let _result = originalBlock(_arg0)
                        return { _result; return true }()
                    }
                }(), {
                    let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                    return { (arg0: Swift.UnsafeMutableRawPointer?) in
                        let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                        let _result = originalBlock(_arg0)
                        return { _result; return true }()
                    }
                }(), cancellation.__externalRCRef())
            }
        }
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")
        public func start() -> Swift.Bool {
            return kotlinx_coroutines_NonCancellable_start(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlinx_coroutines_NonCancellable_toString(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        private init() {
            fatalError()
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public final class NonDisposableHandle: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle, ExportedKotlinPackages.kotlinx.coroutines.__DisposableHandle {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public var parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)? {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return { switch kotlinx_coroutines_NonDisposableHandle_parent_get(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job; } }()
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public static var shared: ExportedKotlinPackages.kotlinx.coroutines.NonDisposableHandle {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return ExportedKotlinPackages.kotlinx.coroutines.NonDisposableHandle.__createClassWrapper(externalRCRef: kotlinx_coroutines_NonDisposableHandle_get())
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public func childCancelled(
            cause: ExportedKotlinPackages.kotlin.Throwable
        ) -> Swift.Bool {
            return kotlinx_coroutines_NonDisposableHandle_childCancelled__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable__(self.__externalRCRef(), cause.__externalRCRef())
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public func dispose() -> Swift.Void {
            return { kotlinx_coroutines_NonDisposableHandle_dispose(self.__externalRCRef()); return () }()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public func toString() -> Swift.String {
            return kotlinx_coroutines_NonDisposableHandle_toString(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        private init() {
            fatalError()
        }
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public final class TimeoutCancellationException: ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException {
    }
    public static func getIO(
        _ receiver: ExportedKotlinPackages.kotlinx.coroutines.Dispatchers
    ) -> ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher {
        return ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: kotlinx_coroutines_IO_get__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_Dispatchers__(receiver.__externalRCRef()))
    }
    public static func getIsActive(
        _ receiver: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> Swift.Bool {
        return kotlinx_coroutines_isActive_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(receiver.__externalRCRef())
    }
    public static func getIsActive(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
    ) -> Swift.Bool {
        return kotlinx_coroutines_isActive_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope__(receiver.__externalRCRef())
    }
    public static func getJob(
        _ receiver: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.Job {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_job_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(receiver.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job
    }
    public static func cancellationException(
        message: Swift.String?,
        cause: ExportedKotlinPackages.kotlin.Throwable?
    ) -> ExportedKotlinPackages.kotlinx.coroutines.CancellationException {
        return ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException.__createClassWrapper(externalRCRef: kotlinx_coroutines_CancellationException__TypesOfArguments__Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil))
    }
    public static func completableDeferred(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_CompletableDeferred__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(value.map { it in it.__externalRCRef() } ?? nil), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred
    }
    public static func completableDeferred(
        parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)?
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_CompletableDeferred__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_kotlinx_coroutines_Job___(parent.map { it in it.__externalRCRef() } ?? nil), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred
    }
    public static func coroutineExceptionHandler(
        handler: @escaping (any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext, ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.CoroutineExceptionHandler {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_CoroutineExceptionHandler__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_U20ExportedKotlinPackages_kotlin_ThrowableU29202D_U20Swift_Void__({
            let originalBlock: (any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext, ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void = handler
            return { (arg0: Swift.UnsafeMutableRawPointer, arg1: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
                let _arg1: ExportedKotlinPackages.kotlin.Throwable = ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: arg1)
                let _result = originalBlock(_arg0, _arg1)
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineExceptionHandler.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineExceptionHandler
    }
    public static func coroutineScope(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_CoroutineScope__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(context.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
    }
    public static func job(
        parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)?
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.CompletableJob {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_Job__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_kotlinx_coroutines_Job___(parent.map { it in it.__externalRCRef() } ?? nil), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CompletableJob.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CompletableJob
    }
    public static func MainScope() -> any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_MainScope(), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
    }
    public static func runnable(
        block: @escaping () -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.Runnable {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_Runnable__TypesOfArguments__U2829202D_U20Swift_Void__({
            let originalBlock: () -> Swift.Void = block
            return {
                let _result = originalBlock()
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Runnable.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Runnable
    }
    public static func supervisorJob(
        parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)?
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.CompletableJob {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_SupervisorJob__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_kotlinx_coroutines_Job___(parent.map { it in it.__externalRCRef() } ?? nil), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CompletableJob.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CompletableJob
    }
    public static func awaitAll(
        deferreds: any ExportedKotlinPackages.kotlinx.coroutines.Deferred...
    ) async throws -> [(any KotlinRuntimeSupport._KotlinBridgeable)?] {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_awaitAll__TypesOfArguments__Swift_Array_anyU20ExportedKotlinPackages_kotlinx_coroutines_Deferred__Vararg___(deferreds, {
                let originalBlock: (Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>) -> Swift.Void = continuation
                return { (arg0: Any) in
                    let _arg0: Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> = arg0 as! Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func awaitCancellation() async throws -> Swift.Never {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_awaitCancellation({
                let originalBlock: (Swift.Never) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Never = { arg0; fatalError() }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func coroutineScope(
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_coroutineScope__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___({
                let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
                return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                    let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                    let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                    }()
                    let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                    }()
                    let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                    let _result = withKotlinTask(_continuation, _exception, _cancellation){
                        try await originalBlock(_arg0)
                    }
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func currentCoroutineContext() async throws -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_currentCoroutineContext({
                let originalBlock: (any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer) in
                    let _arg0: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func delay(
        timeMillis: Swift.Int64
    ) async throws -> Swift.Void {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_delay__TypesOfArguments__Swift_Int64__(timeMillis, {
                let originalBlock: (Swift.Void) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Void = { arg0; return () }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func delay(
        duration: ExportedKotlinPackages.kotlin.time.Duration
    ) async throws -> Swift.Void {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_delay__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__(duration.__externalRCRef(), {
                let originalBlock: (Swift.Void) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Void = { arg0; return () }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public static func handleCoroutineException(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        exception: ExportedKotlinPackages.kotlin.Throwable
    ) -> Swift.Void {
        return { kotlinx_coroutines_handleCoroutineException__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ExportedKotlinPackages_kotlin_Throwable__(context.__externalRCRef(), exception.__externalRCRef()); return () }()
    }
    public static func joinAll(
        jobs: any ExportedKotlinPackages.kotlinx.coroutines.Job...
    ) async throws -> Swift.Void {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_joinAll__TypesOfArguments__Swift_Array_anyU20ExportedKotlinPackages_kotlinx_coroutines_Job__Vararg___(jobs, {
                let originalBlock: (Swift.Void) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Void = { arg0; return () }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func newFixedThreadPoolContext(
        nThreads: Swift.Int32,
        name: Swift.String
    ) -> ExportedKotlinPackages.kotlinx.coroutines.CloseableCoroutineDispatcher {
        return ExportedKotlinPackages.kotlinx.coroutines.CloseableCoroutineDispatcher.__createClassWrapper(externalRCRef: kotlinx_coroutines_newFixedThreadPoolContext__TypesOfArguments__Swift_Int32_Swift_String__(nThreads, name))
    }
    @_spi(kotlinx$coroutines$DelicateCoroutinesApi) @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func newSingleThreadContext(
        name: Swift.String
    ) -> ExportedKotlinPackages.kotlinx.coroutines.CloseableCoroutineDispatcher {
        return ExportedKotlinPackages.kotlinx.coroutines.CloseableCoroutineDispatcher.__createClassWrapper(externalRCRef: kotlinx_coroutines_newSingleThreadContext__TypesOfArguments__Swift_String__(name))
    }
    public static func runBlocking(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_runBlocking__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(context.__externalRCRef(), {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
            return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public static func supervisorScope(
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_supervisorScope__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___({
                let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
                return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                    let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                    let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                    }()
                    let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                    }()
                    let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                    let _result = withKotlinTask(_continuation, _exception, _cancellation){
                        try await originalBlock(_arg0)
                    }
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func suspendCancellableCoroutine(
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation) -> Swift.Void
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_suspendCancellableCoroutine__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CancellableContinuationU29202D_U20Swift_Void__({
                let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation) -> Swift.Void = block
                return { (arg0: Swift.UnsafeMutableRawPointer) in
                    let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func withContext(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_withContext__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(context.__externalRCRef(), {
                let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
                return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                    let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                    let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                    }()
                    let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                    }()
                    let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                    let _result = withKotlinTask(_continuation, _exception, _cancellation){
                        try await originalBlock(_arg0)
                    }
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func withTimeout(
        timeMillis: Swift.Int64,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_withTimeout__TypesOfArguments__Swift_Int64_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(timeMillis, {
                let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
                return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                    let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                    let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                    }()
                    let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                    }()
                    let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                    let _result = withKotlinTask(_continuation, _exception, _cancellation){
                        try await originalBlock(_arg0)
                    }
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func withTimeout(
        timeout: ExportedKotlinPackages.kotlin.time.Duration,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_withTimeout__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(timeout.__externalRCRef(), {
                let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
                return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                    let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                    let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                    }()
                    let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                    }()
                    let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                    let _result = withKotlinTask(_continuation, _exception, _cancellation){
                        try await originalBlock(_arg0)
                    }
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func withTimeoutOrNull(
        timeMillis: Swift.Int64,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_withTimeoutOrNull__TypesOfArguments__Swift_Int64_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(timeMillis, {
                let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
                return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                    let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                    let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                    }()
                    let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                    }()
                    let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                    let _result = withKotlinTask(_continuation, _exception, _cancellation){
                        try await originalBlock(_arg0)
                    }
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func withTimeoutOrNull(
        timeout: ExportedKotlinPackages.kotlin.time.Duration,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_withTimeoutOrNull__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(timeout.__externalRCRef(), {
                let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
                return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                    let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                    let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                    }()
                    let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                    }()
                    let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                    let _result = withKotlinTask(_continuation, _exception, _cancellation){
                        try await originalBlock(_arg0)
                    }
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func yield() async throws -> Swift.Void {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_yield({
                let originalBlock: (Swift.Void) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Void = { arg0; return () }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func async(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        start: ExportedKotlinPackages.kotlinx.coroutines.CoroutineStart,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.Deferred {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_async__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ExportedKotlinPackages_kotlinx_coroutines_CoroutineStart_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.__externalRCRef(), context.__externalRCRef(), start.__externalRCRef(), {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
            return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Deferred.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Deferred
    }
    public static func cancel(
        _ receiver: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
    ) -> Swift.Void {
        return { kotlinx_coroutines_cancel__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException___(receiver.__externalRCRef(), cause.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    public static func cancel(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.Job,
        message: Swift.String,
        cause: ExportedKotlinPackages.kotlin.Throwable?
    ) -> Swift.Void {
        return { kotlinx_coroutines_cancel__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_Job_Swift_String_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(receiver.__externalRCRef(), message, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    public static func cancel(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        message: Swift.String,
        cause: ExportedKotlinPackages.kotlin.Throwable?
    ) -> Swift.Void {
        return { kotlinx_coroutines_cancel__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_Swift_String_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(receiver.__externalRCRef(), message, cause.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    public static func cancel(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
    ) -> Swift.Void {
        return { kotlinx_coroutines_cancel__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException___(receiver.__externalRCRef(), cause.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    public static func cancelAndJoin(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.Job
    ) async throws -> Swift.Void {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_cancelAndJoin__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_Job__(receiver.__externalRCRef(), {
                let originalBlock: (Swift.Void) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Void = { arg0; return () }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func cancelChildren(
        _ receiver: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
    ) -> Swift.Void {
        return { kotlinx_coroutines_cancelChildren__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException___(receiver.__externalRCRef(), cause.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    public static func cancelChildren(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.Job,
        cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
    ) -> Swift.Void {
        return { kotlinx_coroutines_cancelChildren__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_Job_Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException___(receiver.__externalRCRef(), cause.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    public static func completeWith(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred,
        result: ExportedKotlinPackages.kotlin.Result
    ) -> Swift.Bool {
        return kotlinx_coroutines_completeWith__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CompletableDeferred_ExportedKotlinPackages_kotlin_Result__(receiver.__externalRCRef(), result.__externalRCRef())
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public static func disposeOnCancellation(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation,
        handle: any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
    ) -> Swift.Void {
        return { kotlinx_coroutines_disposeOnCancellation__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CancellableContinuation_anyU20ExportedKotlinPackages_kotlinx_coroutines_DisposableHandle__(receiver.__externalRCRef(), handle.__externalRCRef()); return () }()
    }
    public static func ensureActive(
        _ receiver: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> Swift.Void {
        return { kotlinx_coroutines_ensureActive__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(receiver.__externalRCRef()); return () }()
    }
    public static func ensureActive(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.Job
    ) -> Swift.Void {
        return { kotlinx_coroutines_ensureActive__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_Job__(receiver.__externalRCRef()); return () }()
    }
    public static func ensureActive(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
    ) -> Swift.Void {
        return { kotlinx_coroutines_ensureActive__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope__(receiver.__externalRCRef()); return () }()
    }
    public static func invoke(
        _ receiver: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_invoke__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_CoroutineDispatcher_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.__externalRCRef(), {
                let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
                return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                    let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                    let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                    }()
                    let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                    }()
                    let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                    let _result = withKotlinTask(_continuation, _exception, _cancellation){
                        try await originalBlock(_arg0)
                    }
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func launch(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        start: ExportedKotlinPackages.kotlinx.coroutines.CoroutineStart,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.Job {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_launch__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ExportedKotlinPackages_kotlinx_coroutines_CoroutineStart_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Void__(receiver.__externalRCRef(), context.__externalRCRef(), start.__externalRCRef(), {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Void = block
            return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job
    }
    public static func newCoroutineContext(
        _ receiver: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        addedContext: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_newCoroutineContext__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(receiver.__externalRCRef(), addedContext.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
    public static func newCoroutineContext(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_newCoroutineContext__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(receiver.__externalRCRef(), context.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
    public static func plus(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_plus__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(receiver.__externalRCRef(), context.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
    }
    public static func disposableHandle(
        function: @escaping () -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_DisposableHandle__TypesOfArguments__U2829202D_U20Swift_Void__({
            let originalBlock: () -> Swift.Void = function
            return {
                let _result = originalBlock()
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.intrinsics {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public static func startCoroutineCancellable(
        _ receiver: @escaping () async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?,
        completion: any ExportedKotlinPackages.kotlin.coroutines.Continuation
    ) -> Swift.Void {
        return { kotlinx_coroutines_intrinsics_startCoroutineCancellable__TypesOfArgumentsE__U282920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation__({
            let originalBlock: () async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = receiver
            return { (continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock()
                }
                return { _result; return true }()
            }
        }(), completion.__externalRCRef()); return () }()
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels {
    public enum BufferOverflow: KotlinRuntimeSupport._KotlinBridgeable, Swift.CaseIterable, Swift.LosslessStringConvertible, Swift.RawRepresentable {
        case SUSPEND
        case DROP_OLDEST
        case DROP_LATEST
        public var description: Swift.String {
            get {
                switch self {
                case .SUSPEND: "SUSPEND"
                case .DROP_OLDEST: "DROP_OLDEST"
                case .DROP_LATEST: "DROP_LATEST"
                default: fatalError()
                }
            }
        }
        public var rawValue: Swift.Int32 {
            get {
                switch self {
                case .SUSPEND: 0
                case .DROP_OLDEST: 1
                case .DROP_LATEST: 2
                default: fatalError()
                }
            }
        }
        public init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer!,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            switch kotlinx_coroutines_channels_BufferOverflow_ordinal(__externalRCRefUnsafe) {
            case 0: self = .SUSPEND
            case 1: self = .DROP_OLDEST
            case 2: self = .DROP_LATEST
            default: fatalError()
            }
        }
        public func __externalRCRef() -> Swift.UnsafeMutableRawPointer! {
            return switch self {
            case .SUSPEND: kotlinx_coroutines_channels_BufferOverflow_SUSPEND()
            case .DROP_OLDEST: kotlinx_coroutines_channels_BufferOverflow_DROP_OLDEST()
            case .DROP_LATEST: kotlinx_coroutines_channels_BufferOverflow_DROP_LATEST()
            default: fatalError()
            }
        }
        public init?(
            _ description: Swift.String
        ) {
            switch description {
            case "SUSPEND": self = .SUSPEND
            case "DROP_OLDEST": self = .DROP_OLDEST
            case "DROP_LATEST": self = .DROP_LATEST
            default: return nil
            }
        }
        public init?(
            rawValue: Swift.Int32
        ) {
            guard 0..<3 ~= rawValue else { return nil }
            self = BufferOverflow.allCases[Int(rawValue)]
        }
    }
    @available(*, deprecated, message: "BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public protocol BroadcastChannel: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel, ExportedKotlinPackages.kotlinx.coroutines.channels._BroadcastChannel {
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        func cancel(
            cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
        ) -> Swift.Void
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        func openSubscription() -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_channels_BroadcastChannel)
    public protocol _BroadcastChannel: ExportedKotlinPackages.kotlinx.coroutines.channels._SendChannel {
    }
    @available(*, deprecated, message: "BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public protocol __BroadcastChannel: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.channels.__SendChannel {
    }
    public protocol Channel: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel, ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel, ExportedKotlinPackages.kotlinx.coroutines.channels._Channel {
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_channels_Channel)
    public protocol _Channel: ExportedKotlinPackages.kotlinx.coroutines.channels._SendChannel, ExportedKotlinPackages.kotlinx.coroutines.channels._ReceiveChannel {
    }
    public protocol __Channel: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.channels.__SendChannel, ExportedKotlinPackages.kotlinx.coroutines.channels.__ReceiveChannel {
    }
    public protocol ChannelIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.channels._ChannelIterator {
        func hasNext() async throws -> Swift.Bool
        func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_channels_ChannelIterator)
    public protocol _ChannelIterator {
    }
    public protocol __ChannelIterator: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol ProducerScope: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope, ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel, ExportedKotlinPackages.kotlinx.coroutines.channels._ProducerScope {
        var channel: any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel {
            get
        }
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_channels_ProducerScope)
    public protocol _ProducerScope: ExportedKotlinPackages.kotlinx.coroutines._CoroutineScope, ExportedKotlinPackages.kotlinx.coroutines.channels._SendChannel {
    }
    public protocol __ProducerScope: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.__CoroutineScope, ExportedKotlinPackages.kotlinx.coroutines.channels.__SendChannel {
    }
    public protocol ReceiveChannel: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.channels._ReceiveChannel {
        @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
        var isClosedForReceive: Swift.Bool {
            @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
            get
        }
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        var isEmpty: Swift.Bool {
            @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
            get
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        var onReceive: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1 {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        var onReceiveCatching: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1 {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get
        }
        func cancel(
            cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
        ) -> Swift.Void
        func iterator() -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator
        func receive() async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        func receiveCatching() async throws -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult
        func tryReceive() -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_channels_ReceiveChannel)
    public protocol _ReceiveChannel {
    }
    public protocol __ReceiveChannel: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol SendChannel: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.channels._SendChannel {
        @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
        var isClosedForSend: Swift.Bool {
            @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
            get
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        var onSend: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2 {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get
        }
        func close(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) -> Swift.Bool
        func invokeOnClose(
            handler: @escaping (ExportedKotlinPackages.kotlin.Throwable?) -> Swift.Void
        ) -> Swift.Void
        func send(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) async throws -> Swift.Void
        func trySend(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_channels_SendChannel)
    public protocol _SendChannel {
    }
    public protocol __SendChannel: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public final class ChannelResult: KotlinRuntime.KotlinBase {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final class Companion: KotlinRuntime.KotlinBase {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            public static var shared: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult.Companion {
                @_spi(kotlinx$coroutines$InternalCoroutinesApi)
                get {
                    return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult.Companion.__createClassWrapper(externalRCRef: kotlinx_coroutines_channels_ChannelResult_Companion_get())
                }
            }
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            public func closed(
                cause: ExportedKotlinPackages.kotlin.Throwable?
            ) -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult {
                return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult.__createClassWrapper(externalRCRef: kotlinx_coroutines_channels_ChannelResult_Companion_closed__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(self.__externalRCRef(), cause.map { it in it.__externalRCRef() } ?? nil))
            }
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            public func failure() -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult {
                return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult.__createClassWrapper(externalRCRef: kotlinx_coroutines_channels_ChannelResult_Companion_failure(self.__externalRCRef()))
            }
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            public func success(
                value: (any KotlinRuntimeSupport._KotlinBridgeable)?
            ) -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult {
                return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult.__createClassWrapper(externalRCRef: kotlinx_coroutines_channels_ChannelResult_Companion_success__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil))
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private init() {
                fatalError()
            }
        }
        public var isClosed: Swift.Bool {
            get {
                return kotlinx_coroutines_channels_ChannelResult_isClosed_get(self.__externalRCRef())
            }
        }
        public var isFailure: Swift.Bool {
            get {
                return kotlinx_coroutines_channels_ChannelResult_isFailure_get(self.__externalRCRef())
            }
        }
        public var isSuccess: Swift.Bool {
            get {
                return kotlinx_coroutines_channels_ChannelResult_isSuccess_get(self.__externalRCRef())
            }
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlinx_coroutines_channels_ChannelResult_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func exceptionOrNull() -> ExportedKotlinPackages.kotlin.Throwable? {
            return { switch kotlinx_coroutines_channels_ChannelResult_exceptionOrNull(self.__externalRCRef()) { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
        }
        public func getOrNull() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlinx_coroutines_channels_ChannelResult_getOrNull(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
        public func getOrThrow() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlinx_coroutines_channels_ChannelResult_getOrThrow(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
        public func hashCode() -> Swift.Int32 {
            return kotlinx_coroutines_channels_ChannelResult_hashCode(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlinx_coroutines_channels_ChannelResult_toString(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class ClosedReceiveChannelException: ExportedKotlinPackages.kotlin.NoSuchElementException {
        public override init(
            message: Swift.String?
        ) {
            let __kt = kotlinx_coroutines_channels_ClosedReceiveChannelException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_channels_ClosedReceiveChannelException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class ClosedSendChannelException: ExportedKotlinPackages.kotlin.IllegalStateException {
        public override init(
            message: Swift.String?
        ) {
            let __kt = kotlinx_coroutines_channels_ClosedSendChannelException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_channels_ClosedSendChannelException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt, message ?? nil); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @available(*, deprecated, message: "ConflatedBroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public final class ConflatedBroadcastChannel: KotlinRuntime.KotlinBase {
        @_spi(kotlinx$coroutines$DelicateCoroutinesApi) @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public var isClosedForSend: Swift.Bool {
            @_spi(kotlinx$coroutines$DelicateCoroutinesApi) @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
            get {
                return kotlinx_coroutines_channels_ConflatedBroadcastChannel_isClosedForSend_get(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi) @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public var onSend: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2 {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi) @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_ConflatedBroadcastChannel_onSend_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2
            }
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public var value: (any KotlinRuntimeSupport._KotlinBridgeable)? {
            @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
            get {
                return { switch kotlinx_coroutines_channels_ConflatedBroadcastChannel_value_get(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
            }
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public var valueOrNull: (any KotlinRuntimeSupport._KotlinBridgeable)? {
            @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
            get {
                return { switch kotlinx_coroutines_channels_ConflatedBroadcastChannel_valueOrNull_get(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
            }
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public func cancel(
            cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
        ) -> Swift.Void {
            return { kotlinx_coroutines_channels_ConflatedBroadcastChannel_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException___(self.__externalRCRef(), cause.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public func close(
            cause: ExportedKotlinPackages.kotlin.Throwable?
        ) -> Swift.Bool {
            return kotlinx_coroutines_channels_ConflatedBroadcastChannel_close__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(self.__externalRCRef(), cause.map { it in it.__externalRCRef() } ?? nil)
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public func invokeOnClose(
            handler: @escaping (ExportedKotlinPackages.kotlin.Throwable?) -> Swift.Void
        ) -> Swift.Void {
            return { kotlinx_coroutines_channels_ConflatedBroadcastChannel_invokeOnClose__TypesOfArguments__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), {
                let originalBlock: (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void = handler
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable> = { switch arg0 { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }()); return () }()
        }
        @available(*, unavailable, message: "Deprecated in the favour of 'trySend' method. Replacement: trySend(element).isSuccess") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public func offer(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            fatalError()
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public func openSubscription() -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_ConflatedBroadcastChannel_openSubscription(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public func send(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) async throws -> Swift.Void {
            try await withKotlinContinuation { continuation, exception, cancellation in
                let _: Bool = kotlinx_coroutines_channels_ConflatedBroadcastChannel_send__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil, {
                    let originalBlock: (Swift.Void) -> Swift.Void = continuation
                    return { (arg0: Swift.Bool) in
                        let _arg0: Swift.Void = { arg0; return () }()
                        let _result = originalBlock(_arg0)
                        return { _result; return true }()
                    }
                }(), {
                    let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                    return { (arg0: Swift.UnsafeMutableRawPointer?) in
                        let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                        let _result = originalBlock(_arg0)
                        return { _result; return true }()
                    }
                }(), cancellation.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public func trySend(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult {
            return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult.__createClassWrapper(externalRCRef: kotlinx_coroutines_channels_ConflatedBroadcastChannel_trySend__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil))
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public init() {
            let __kt = kotlinx_coroutines_channels_ConflatedBroadcastChannel_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_channels_ConflatedBroadcastChannel_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public init(
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) {
            let __kt = kotlinx_coroutines_channels_ConflatedBroadcastChannel_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_channels_ConflatedBroadcastChannel_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(__kt, value.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @available(*, deprecated, message: "BroadcastChannel is deprecated in the favour of SharedFlow and StateFlow, and is no longer supported") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public static func broadcastChannel(
        capacity: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_BroadcastChannel__TypesOfArguments__Swift_Int32__(capacity), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel
    }
    public static func channel(
        capacity: Swift.Int32,
        onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow,
        onUndeliveredElement: (((any KotlinRuntimeSupport._KotlinBridgeable)?) -> Swift.Void)?
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.channels.Channel {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_Channel__TypesOfArguments__Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow_Swift_Optional_U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U29202D_U20Swift_Void___(capacity, onBufferOverflow.__externalRCRef(), onUndeliveredElement.map { it in {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = it
            return { (arg0: Swift.UnsafeMutableRawPointer?) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _result = originalBlock(_arg0)
                return { _result; return true }()
            }
        }() } ?? nil), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.Channel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.Channel
    }
    public static func awaitClose(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope,
        block: @escaping () -> Swift.Void
    ) async throws -> Swift.Void {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_channels_awaitClose__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ProducerScope_U2829202D_U20Swift_Void__(receiver.__externalRCRef(), {
                let originalBlock: () -> Swift.Void = block
                return {
                    let _result = originalBlock()
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Void) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Void = { arg0; return () }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    @available(*, deprecated, message: "BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public static func broadcast(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        capacity: Swift.Int32,
        start: ExportedKotlinPackages.kotlinx.coroutines.CoroutineStart,
        onCompletion: ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler?,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope) async throws -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_broadcast__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_CoroutineStart_Swift_Optional_U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ProducerScopeU2920asyncU20throwsU202D_U20Swift_Void__(receiver.__externalRCRef(), context.__externalRCRef(), capacity, start.__externalRCRef(), onCompletion.map { it in {
            let originalBlock: (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void = it
            return { (arg0: Swift.UnsafeMutableRawPointer?) in
                let _arg0: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable> = { switch arg0 { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
                let _result = originalBlock(_arg0)
                return { _result; return true }()
            }
        }() } ?? nil, {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope) async throws -> Swift.Void = block
            return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel
    }
    @available(*, deprecated, message: "BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public static func broadcast(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel,
        capacity: Swift.Int32,
        start: ExportedKotlinPackages.kotlinx.coroutines.CoroutineStart
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_broadcast__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ReceiveChannel_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_CoroutineStart__(receiver.__externalRCRef(), capacity, start.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel
    }
    @available(*, unavailable, message: "BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public static func consume(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        fatalError()
    }
    public static func consume(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_channels_consume__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ReceiveChannel_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ReceiveChannelU29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.__externalRCRef(), {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel) -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
            return { (arg0: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
                let _result = originalBlock(_arg0)
                return _result.map { it in it.__externalRCRef() } ?? nil
            }
        }()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    @available(*, unavailable, message: "BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public static func consumeEach(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel,
        action: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) -> Swift.Void
    ) async throws -> Swift.Void {
        fatalError()
    }
    public static func consumeEach(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel,
        action: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) -> Swift.Void
    ) async throws -> Swift.Void {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_channels_consumeEach__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ReceiveChannel_U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U29202D_U20Swift_Void__(receiver.__externalRCRef(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = action
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Void) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Void = { arg0; return () }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func getOrElse(
        _ receiver: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult,
        onFailure: @escaping (ExportedKotlinPackages.kotlin.Throwable?) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_channels_getOrElse__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_channels_ChannelResult_U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.__externalRCRef(), {
            let originalBlock: (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = onFailure
            return { (arg0: Swift.UnsafeMutableRawPointer?) in
                let _arg0: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable> = { switch arg0 { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
                let _result = originalBlock(_arg0)
                return _result.map { it in it.__externalRCRef() } ?? nil
            }
        }()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public static func onClosed(
        _ receiver: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult,
        action: @escaping (ExportedKotlinPackages.kotlin.Throwable?) -> Swift.Void
    ) -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult {
        return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult.__createClassWrapper(externalRCRef: kotlinx_coroutines_channels_onClosed__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_channels_ChannelResult_U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(receiver.__externalRCRef(), {
            let originalBlock: (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void = action
            return { (arg0: Swift.UnsafeMutableRawPointer?) in
                let _arg0: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable> = { switch arg0 { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
                let _result = originalBlock(_arg0)
                return { _result; return true }()
            }
        }()))
    }
    public static func onFailure(
        _ receiver: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult,
        action: @escaping (ExportedKotlinPackages.kotlin.Throwable?) -> Swift.Void
    ) -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult {
        return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult.__createClassWrapper(externalRCRef: kotlinx_coroutines_channels_onFailure__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_channels_ChannelResult_U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(receiver.__externalRCRef(), {
            let originalBlock: (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void = action
            return { (arg0: Swift.UnsafeMutableRawPointer?) in
                let _arg0: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable> = { switch arg0 { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
                let _result = originalBlock(_arg0)
                return { _result; return true }()
            }
        }()))
    }
    public static func onSuccess(
        _ receiver: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult,
        action: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) -> Swift.Void
    ) -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult {
        return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult.__createClassWrapper(externalRCRef: kotlinx_coroutines_channels_onSuccess__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_channels_ChannelResult_U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U29202D_U20Swift_Void__(receiver.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = action
            return { (arg0: Swift.UnsafeMutableRawPointer?) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _result = originalBlock(_arg0)
                return { _result; return true }()
            }
        }()))
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public static func produce(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        capacity: Swift.Int32,
        start: ExportedKotlinPackages.kotlinx.coroutines.CoroutineStart,
        onCompletion: ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler?,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope) async throws -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_produce__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_CoroutineStart_Swift_Optional_U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ProducerScopeU2920asyncU20throwsU202D_U20Swift_Void__(receiver.__externalRCRef(), context.__externalRCRef(), capacity, start.__externalRCRef(), onCompletion.map { it in {
            let originalBlock: (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void = it
            return { (arg0: Swift.UnsafeMutableRawPointer?) in
                let _arg0: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable> = { switch arg0 { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
                let _result = originalBlock(_arg0)
                return { _result; return true }()
            }
        }() } ?? nil, {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope) async throws -> Swift.Void = block
            return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func produce(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        capacity: Swift.Int32,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope) async throws -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_produce__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ProducerScopeU2920asyncU20throwsU202D_U20Swift_Void__(receiver.__externalRCRef(), context.__externalRCRef(), capacity, {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope) async throws -> Swift.Void = block
            return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
    }
    public static func toList(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
    ) async throws -> [(any KotlinRuntimeSupport._KotlinBridgeable)?] {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_channels_toList__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ReceiveChannel__(receiver.__externalRCRef(), {
                let originalBlock: (Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>) -> Swift.Void = continuation
                return { (arg0: Any) in
                    let _arg0: Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> = arg0 as! Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func trySendBlocking(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel,
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult {
        return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult.__createClassWrapper(externalRCRef: kotlinx_coroutines_channels_trySendBlocking__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_SendChannel_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil))
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow {
    public enum SharingCommand: KotlinRuntimeSupport._KotlinBridgeable, Swift.CaseIterable, Swift.LosslessStringConvertible, Swift.RawRepresentable {
        case START
        case STOP
        case STOP_AND_RESET_REPLAY_CACHE
        public var description: Swift.String {
            get {
                switch self {
                case .START: "START"
                case .STOP: "STOP"
                case .STOP_AND_RESET_REPLAY_CACHE: "STOP_AND_RESET_REPLAY_CACHE"
                default: fatalError()
                }
            }
        }
        public var rawValue: Swift.Int32 {
            get {
                switch self {
                case .START: 0
                case .STOP: 1
                case .STOP_AND_RESET_REPLAY_CACHE: 2
                default: fatalError()
                }
            }
        }
        public init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer!,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            switch kotlinx_coroutines_flow_SharingCommand_ordinal(__externalRCRefUnsafe) {
            case 0: self = .START
            case 1: self = .STOP
            case 2: self = .STOP_AND_RESET_REPLAY_CACHE
            default: fatalError()
            }
        }
        public func __externalRCRef() -> Swift.UnsafeMutableRawPointer! {
            return switch self {
            case .START: kotlinx_coroutines_flow_SharingCommand_START()
            case .STOP: kotlinx_coroutines_flow_SharingCommand_STOP()
            case .STOP_AND_RESET_REPLAY_CACHE: kotlinx_coroutines_flow_SharingCommand_STOP_AND_RESET_REPLAY_CACHE()
            default: fatalError()
            }
        }
        public init?(
            _ description: Swift.String
        ) {
            switch description {
            case "START": self = .START
            case "STOP": self = .STOP
            case "STOP_AND_RESET_REPLAY_CACHE": self = .STOP_AND_RESET_REPLAY_CACHE
            default: return nil
            }
        }
        public init?(
            rawValue: Swift.Int32
        ) {
            guard 0..<3 ~= rawValue else { return nil }
            self = SharingCommand.allCases[Int(rawValue)]
        }
    }
    public protocol Flow: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow._Flow, KotlinCoroutineSupport.KotlinFlow {
        func collect(
            collector: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
        ) async throws -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_flow_Flow)
    public protocol _Flow {
    }
    public protocol __Flow: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol FlowCollector: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow._FlowCollector {
        func emit(
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) async throws -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector)
    public protocol _FlowCollector {
    }
    public protocol __FlowCollector: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol MutableSharedFlow: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow, ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, ExportedKotlinPackages.kotlinx.coroutines.flow._MutableSharedFlow, KotlinCoroutineSupport.KotlinMutableSharedFlow {
        var subscriptionCount: any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Int32> {
            get
        }
        func emit(
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) async throws -> Swift.Void
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        func resetReplayCache() -> Swift.Void
        func tryEmit(
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_flow_MutableSharedFlow)
    public protocol _MutableSharedFlow: ExportedKotlinPackages.kotlinx.coroutines.flow._SharedFlow, ExportedKotlinPackages.kotlinx.coroutines.flow._FlowCollector {
    }
    public protocol __MutableSharedFlow: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.flow.__SharedFlow, ExportedKotlinPackages.kotlinx.coroutines.flow.__FlowCollector {
    }
    public protocol MutableStateFlow: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow, ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow, ExportedKotlinPackages.kotlinx.coroutines.flow._MutableStateFlow, KotlinCoroutineSupport.KotlinMutableStateFlow {
        var value: (any KotlinRuntimeSupport._KotlinBridgeable)? {
            get
            set
        }
        func compareAndSet(
            expect: (any KotlinRuntimeSupport._KotlinBridgeable)?,
            update: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_flow_MutableStateFlow)
    public protocol _MutableStateFlow: ExportedKotlinPackages.kotlinx.coroutines.flow._StateFlow, ExportedKotlinPackages.kotlinx.coroutines.flow._MutableSharedFlow {
    }
    public protocol __MutableStateFlow: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.flow.__StateFlow, ExportedKotlinPackages.kotlinx.coroutines.flow.__MutableSharedFlow {
    }
    public protocol SharedFlow: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, ExportedKotlinPackages.kotlinx.coroutines.flow._SharedFlow, KotlinCoroutineSupport.KotlinSharedFlow {
        var replayCache: [(any KotlinRuntimeSupport._KotlinBridgeable)?] {
            get
        }
        func collect(
            collector: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
        ) async throws -> Swift.Never
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_flow_SharedFlow)
    public protocol _SharedFlow: ExportedKotlinPackages.kotlinx.coroutines.flow._Flow {
    }
    public protocol __SharedFlow: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.flow.__Flow {
    }
    public protocol SharingStarted: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow._SharingStarted {
        func command(
            subscriptionCount: any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Int32>
        ) -> any KotlinCoroutineSupport.KotlinTypedFlow<ExportedKotlinPackages.kotlinx.coroutines.flow.SharingCommand>
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_flow_SharingStarted)
    public protocol _SharingStarted {
    }
    public protocol __SharingStarted: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol StateFlow: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow, ExportedKotlinPackages.kotlinx.coroutines.flow._StateFlow, KotlinCoroutineSupport.KotlinStateFlow {
        var value: (any KotlinRuntimeSupport._KotlinBridgeable)? {
            get
        }
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_flow_StateFlow)
    public protocol _StateFlow: ExportedKotlinPackages.kotlinx.coroutines.flow._SharedFlow {
    }
    public protocol __StateFlow: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.flow.__SharedFlow {
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    open class AbstractFlow: KotlinRuntime.KotlinBase {
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        public final func collect(
            collector: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
        ) async throws -> Swift.Void {
            try await withKotlinContinuation { continuation, exception, cancellation in
                let _: Bool = kotlinx_coroutines_flow_AbstractFlow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector__(self.__externalRCRef(), collector.__externalRCRef(), {
                    let originalBlock: (Swift.Void) -> Swift.Void = continuation
                    return { (arg0: Swift.Bool) in
                        let _arg0: Swift.Void = { arg0; return () }()
                        let _result = originalBlock(_arg0)
                        return { _result; return true }()
                    }
                }(), {
                    let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                    return { (arg0: Swift.UnsafeMutableRawPointer?) in
                        let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                        let _result = originalBlock(_arg0)
                        return { _result; return true }()
                    }
                }(), cancellation.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        open func collectSafely(
            collector: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
        ) async throws -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.flow.AbstractFlow.self {
                try await withKotlinContinuation { continuation, exception, cancellation in
                let _: Bool = kotlinx_coroutines_flow_AbstractFlow_collectSafely__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector__(self.__externalRCRef(), collector.__externalRCRef(), {
                    let originalBlock: (Swift.Void) -> Swift.Void = continuation
                    return { (arg0: Swift.Bool) in
                        let _arg0: Swift.Void = { arg0; return () }()
                        let _result = originalBlock(_arg0)
                        return { _result; return true }()
                    }
                }(), {
                    let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                    return { (arg0: Swift.UnsafeMutableRawPointer?) in
                        let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                        let _result = originalBlock(_arg0)
                        return { _result; return true }()
                    }
                }(), cancellation.__externalRCRef())
            }
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlinx.coroutines.flow.AbstractFlow.collectSafely': a Swift subclass must override it and must not call super.")
            }
        }
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        public init() {
            precondition(Self.self != ExportedKotlinPackages.kotlinx.coroutines.flow.AbstractFlow.self, "ExportedKotlinPackages.kotlinx.coroutines.flow.AbstractFlow is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_flow_AbstractFlow_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static var DEFAULT_CONCURRENCY: Swift.Int32 {
        @_spi(kotlinx$coroutines$FlowPreview)
        get {
            return kotlinx_coroutines_flow_DEFAULT_CONCURRENCY_get()
        }
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static var DEFAULT_CONCURRENCY_PROPERTY_NAME: Swift.String {
        @_spi(kotlinx$coroutines$FlowPreview)
        get {
            return kotlinx_coroutines_flow_DEFAULT_CONCURRENCY_PROPERTY_NAME_get()
        }
    }
    public static func getCoroutineContext(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_coroutineContext_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector__(receiver.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
    public static func getIsActive(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
    ) -> Swift.Bool {
        return kotlinx_coroutines_flow_isActive_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector__(receiver.__externalRCRef())
    }
    public static func mutableSharedFlow(
        replay: Swift.Int32,
        extraBufferCapacity: Swift.Int32,
        onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow
    ) -> any KotlinCoroutineSupport.KotlinTypedMutableSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedMutableSharedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_MutableSharedFlow__TypesOfArguments__Swift_Int32_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow__(replay, extraBufferCapacity, onBufferOverflow.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func mutableStateFlow(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedMutableStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedMutableStateFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_MutableStateFlow__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(value.map { it in it.__externalRCRef() } ?? nil), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func callbackFlow(
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_callbackFlow__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ProducerScopeU2920asyncU20throwsU202D_U20Swift_Void__({
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope) async throws -> Swift.Void = block
            return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func channelFlow(
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_channelFlow__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ProducerScopeU2920asyncU20throwsU202D_U20Swift_Void__({
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope) async throws -> Swift.Void = block
            return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func combine(
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow2: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow3: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow4: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow5: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combine__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(flow.wrapped.__externalRCRef(), flow2.wrapped.__externalRCRef(), flow3.wrapped.__externalRCRef(), flow4.wrapped.__externalRCRef(), flow5.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, arg2: Swift.UnsafeMutableRawPointer?, arg3: Swift.UnsafeMutableRawPointer?, arg4: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg2: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg2 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg3: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg3 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg4: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg4 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1, _arg2, _arg3, _arg4)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func combine(
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow2: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow3: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow4: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combine__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(flow.wrapped.__externalRCRef(), flow2.wrapped.__externalRCRef(), flow3.wrapped.__externalRCRef(), flow4.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, arg2: Swift.UnsafeMutableRawPointer?, arg3: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg2: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg2 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg3: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg3 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1, _arg2, _arg3)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func combine(
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow2: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow3: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combine__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(flow.wrapped.__externalRCRef(), flow2.wrapped.__externalRCRef(), flow3.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, arg2: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg2: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg2 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1, _arg2)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func combine(
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow2: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combine__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(flow.wrapped.__externalRCRef(), flow2.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func combine(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combine__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), flow.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func combineTransform(
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow2: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow3: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow4: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow5: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combineTransform__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Void__(flow.wrapped.__externalRCRef(), flow2.wrapped.__externalRCRef(), flow3.wrapped.__externalRCRef(), flow4.wrapped.__externalRCRef(), flow5.wrapped.__externalRCRef(), {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Void = transform
            return { (arg0: Swift.UnsafeMutableRawPointer, arg1: Swift.UnsafeMutableRawPointer?, arg2: Swift.UnsafeMutableRawPointer?, arg3: Swift.UnsafeMutableRawPointer?, arg4: Swift.UnsafeMutableRawPointer?, arg5: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
                let _arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg2: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg2 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg3: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg3 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg4: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg4 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg5: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg5 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1, _arg2, _arg3, _arg4, _arg5)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func combineTransform(
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow2: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow3: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow4: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combineTransform__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Void__(flow.wrapped.__externalRCRef(), flow2.wrapped.__externalRCRef(), flow3.wrapped.__externalRCRef(), flow4.wrapped.__externalRCRef(), {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Void = transform
            return { (arg0: Swift.UnsafeMutableRawPointer, arg1: Swift.UnsafeMutableRawPointer?, arg2: Swift.UnsafeMutableRawPointer?, arg3: Swift.UnsafeMutableRawPointer?, arg4: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
                let _arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg2: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg2 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg3: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg3 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg4: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg4 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1, _arg2, _arg3, _arg4)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func combineTransform(
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow2: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow3: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combineTransform__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Void__(flow.wrapped.__externalRCRef(), flow2.wrapped.__externalRCRef(), flow3.wrapped.__externalRCRef(), {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Void = transform
            return { (arg0: Swift.UnsafeMutableRawPointer, arg1: Swift.UnsafeMutableRawPointer?, arg2: Swift.UnsafeMutableRawPointer?, arg3: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
                let _arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg2: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg2 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg3: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg3 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1, _arg2, _arg3)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func combineTransform(
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow2: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combineTransform__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Void__(flow.wrapped.__externalRCRef(), flow2.wrapped.__externalRCRef(), {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Void = transform
            return { (arg0: Swift.UnsafeMutableRawPointer, arg1: Swift.UnsafeMutableRawPointer?, arg2: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
                let _arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg2: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg2 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1, _arg2)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func combineTransform(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combineTransform__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), flow.wrapped.__externalRCRef(), {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Void = transform
            return { (arg0: Swift.UnsafeMutableRawPointer, arg1: Swift.UnsafeMutableRawPointer?, arg2: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
                let _arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg2: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg2 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1, _arg2)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func emptyFlow() -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_emptyFlow(), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func flow(
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flow__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollectorU2920asyncU20throwsU202D_U20Swift_Void__({
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector) async throws -> Swift.Void = block
            return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func flowOf(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flowOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(value.map { it in it.__externalRCRef() } ?? nil), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func flowOf(
        elements: (any KotlinRuntimeSupport._KotlinBridgeable)?...
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flowOf__TypesOfArguments__Swift_Array_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Vararg___(elements.map { it in it as! NSObject? ?? NSNull() }), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func merge(
        flows: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>...
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_merge__TypesOfArguments__Swift_Array_anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____Vararg___(flows), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func WhileSubscribed(
        _ receiver: KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_flow_SharingStarted_Companion,
        stopTimeout: ExportedKotlinPackages.kotlin.time.Duration,
        replayExpiration: ExportedKotlinPackages.kotlin.time.Duration
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_WhileSubscribed__TypesOfArgumentsE__KotlinxCoroutinesCore__ExportedKotlinPackages_kotlinx_coroutines_flow_SharingStarted_Companion_ExportedKotlinPackages_kotlin_time_Duration_ExportedKotlinPackages_kotlin_time_Duration__(receiver.__externalRCRef(), stopTimeout.__externalRCRef(), replayExpiration.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted
    }
    public static func asFlow(
        _ receiver: @escaping () -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__U2829202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___({
            let originalBlock: () -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = receiver
            return {
                let _result = originalBlock()
                return _result.map { it in it.__externalRCRef() } ?? nil
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func asFlow(
        _ receiver: ExportedKotlinPackages.kotlin.Array
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__ExportedKotlinPackages_kotlin_Array__(receiver.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func asFlow(
        _ receiver: ExportedKotlinPackages.kotlin.IntArray
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Int32> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Int32>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__ExportedKotlinPackages_kotlin_IntArray__(receiver.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, Swift.Int32.Type.self)
    }
    public static func asFlow(
        _ receiver: ExportedKotlinPackages.kotlin.LongArray
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Int64> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Int64>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__ExportedKotlinPackages_kotlin_LongArray__(receiver.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, Swift.Int64.Type.self)
    }
    public static func asFlow(
        _ receiver: any ExportedKotlinPackages.kotlin.collections.Iterable
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_collections_Iterable__(receiver.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func asFlow(
        _ receiver: any ExportedKotlinPackages.kotlin.collections.Iterator
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_collections_Iterator__(receiver.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func asFlow(
        _ receiver: Swift.ClosedRange<Swift.Int32>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Int32> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Int32>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__Swift_ClosedRange_Swift_Int32___(kotlin_ranges_intRange_create_int_KotlinxCoroutinesCore(receiver.lowerBound, receiver.upperBound)), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, Swift.Int32.Type.self)
    }
    public static func asFlow(
        _ receiver: Swift.ClosedRange<Swift.Int64>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Int64> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Int64>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__Swift_ClosedRange_Swift_Int64___(kotlin_ranges_longRange_create_long_KotlinxCoroutinesCore(receiver.lowerBound, receiver.upperBound)), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, Swift.Int64.Type.self)
    }
    public static func asFlow(
        _ receiver: any ExportedKotlinPackages.kotlin.sequences.Sequence
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_sequences_Sequence__(receiver.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func asFlow(
        _ receiver: @escaping () async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__U282920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___({
            let originalBlock: () async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = receiver
            return { (continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock()
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @available(*, unavailable, message: "'BroadcastChannel' is obsolete and all corresponding operators are deprecated in the favour of StateFlow and SharedFlow") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public static func asFlow(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    public static func asSharedFlow(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedMutableSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedSharedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asSharedFlow__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedMutableSharedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func asStateFlow(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedMutableStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedStateFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asStateFlow__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedMutableStateFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func buffer(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        capacity: Swift.Int32,
        onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_buffer__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow__(receiver.wrapped.__externalRCRef(), capacity, onBufferOverflow.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @available(*, unavailable, message: "Flow analogue of 'cache()' is 'shareIn' with unlimited replay and 'started = SharingStared.Lazily' argument'. Replacement: this.shareIn(scope, Int.MAX_VALUE, started = SharingStared.Lazily)")
    public static func cache(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @available(*, unavailable, message: "cancel() is resolved into the extension of outer CoroutineScope which is likely to be an error.Use currentCoroutineContext().cancel() instead or specify the receiver of cancel() explicitly. Replacement: currentCoroutineContext().cancel(cause)")
    public static func cancel(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector,
        cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
    ) -> Swift.Void {
        fatalError()
    }
    public static func cancellable(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_cancellable__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @available(*, unavailable, message: "Applying 'cancellable' to a SharedFlow has no effect. See the SharedFlow documentation on Operator Fusion.. Replacement: this")
    public static func cancellable(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    public static func `catch`(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        action: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_catch__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_U20ExportedKotlinPackages_kotlin_ThrowableU2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Void = action
            return { (arg0: Swift.UnsafeMutableRawPointer, arg1: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
                let _arg1: ExportedKotlinPackages.kotlin.Throwable = ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: arg1)
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @available(*, deprecated, message: "SharedFlow never completes, so this operator typically has not effect, it can only catch exceptions from 'onSubscribe' operator. Replacement: this")
    public static func `catch`(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        action: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_catch__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_U20ExportedKotlinPackages_kotlin_ThrowableU2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Void = action
            return { (arg0: Swift.UnsafeMutableRawPointer, arg1: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
                let _arg1: ExportedKotlinPackages.kotlin.Throwable = ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: arg1)
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func collect(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow
    ) async throws -> Swift.Void {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_collect__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_Flow__(receiver.__externalRCRef(), {
                let originalBlock: (Swift.Void) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Void = { arg0; return () }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func collectIndexed(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        action: @escaping (Swift.Int32, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Void
    ) async throws -> Swift.Void {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_collectIndexed__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Int32_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), {
                let originalBlock: (Swift.Int32, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Void = action
                return { (arg0: Swift.Int32, arg1: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                    let _arg0: Swift.Int32 = arg0
                    let _arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _continuation: (Swift.Void) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                    }()
                    let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                    }()
                    let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                    let _result = withKotlinTask(_continuation, _exception, _cancellation){
                        try await originalBlock(_arg0, _arg1)
                    }
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Void) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Void = { arg0; return () }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func collectLatest(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        action: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Void
    ) async throws -> Swift.Void {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_collectLatest__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Void = action
                return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _continuation: (Swift.Void) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                    }()
                    let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                    }()
                    let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                    let _result = withKotlinTask(_continuation, _exception, _cancellation){
                        try await originalBlock(_arg0)
                    }
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Void) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Void = { arg0; return () }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    @available(*, unavailable, message: "Flow analogue of 'combineLatest' is 'combine'. Replacement: combine(this, other, other2, other3, transform)")
    public static func combineLatest(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        other: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        other2: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        other3: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        other4: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'combineLatest' is 'combine'. Replacement: combine(this, other, other2, other3, transform)")
    public static func combineLatest(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        other: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        other2: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        other3: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'combineLatest' is 'combine'. Replacement: combine(this, other, other2, transform)")
    public static func combineLatest(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        other: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        other2: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'combineLatest' is 'combine'. Replacement: this.combine(other, transform)")
    public static func combineLatest(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        other: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'compose' is 'let'. Replacement: let(transformer)")
    public static func compose(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transformer: @escaping (any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'concatMap' is 'flatMapConcat'. Replacement: flatMapConcat(mapper)")
    public static func concatMap(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        mapper: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'concatWith' is 'onCompletion'. Use 'onCompletion { emit(value) }'. Replacement: onCompletion { emit(value) }")
    public static func concatWith(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'concatWith' is 'onCompletion'. Use 'onCompletion { if (it == null) emitAll(other) }'. Replacement: onCompletion { if (it == null) emitAll(other) }")
    public static func concatWith(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        other: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    public static func conflate(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_conflate__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @available(*, unavailable, message: "Applying 'conflate' to StateFlow has no effect. See the StateFlow documentation on Operator Fusion.. Replacement: this")
    public static func conflate(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    public static func consumeAsFlow(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_consumeAsFlow__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ReceiveChannel__(receiver.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func count(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) async throws -> Swift.Int32 {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_count__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
                let originalBlock: (Swift.Int32) -> Swift.Void = continuation
                return { (arg0: Swift.Int32) in
                    let _arg0: Swift.Int32 = arg0
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func count(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        predicate: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Bool
    ) async throws -> Swift.Int32 {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_count__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Bool = predicate
                return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _continuation: (Swift.Bool) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                    }()
                    let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                    }()
                    let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                    let _result = withKotlinTask(_continuation, _exception, _cancellation){
                        try await originalBlock(_arg0)
                    }
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Int32) -> Swift.Void = continuation
                return { (arg0: Swift.Int32) in
                    let _arg0: Swift.Int32 = arg0
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    @available(*, deprecated, message: "SharedFlow never completes, so this terminal operation never completes.")
    public static func count(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) async throws -> Swift.Int32 {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_count__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
                let originalBlock: (Swift.Int32) -> Swift.Void = continuation
                return { (arg0: Swift.Int32) in
                    let _arg0: Swift.Int32 = arg0
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static func debounce(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        timeoutMillis: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) -> Swift.Int64
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_debounce__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U29202D_U20Swift_Int64__(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Int64 = timeoutMillis
            return { (arg0: Swift.UnsafeMutableRawPointer?) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _result = originalBlock(_arg0)
                return _result
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static func debounce(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        timeout: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) -> ExportedKotlinPackages.kotlin.time.Duration
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_debounce__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U29202D_U20ExportedKotlinPackages_kotlin_time_Duration__(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> ExportedKotlinPackages.kotlin.time.Duration = timeout
            return { (arg0: Swift.UnsafeMutableRawPointer?) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _result = originalBlock(_arg0)
                return _result.__externalRCRef()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static func debounce(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        timeoutMillis: Swift.Int64
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_debounce__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Int64__(receiver.wrapped.__externalRCRef(), timeoutMillis), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static func debounce(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        timeout: ExportedKotlinPackages.kotlin.time.Duration
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_debounce__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___ExportedKotlinPackages_kotlin_time_Duration__(receiver.wrapped.__externalRCRef(), timeout.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @available(*, unavailable, message: "Use 'onEach { delay(timeMillis) }'. Replacement: onEach { delay(timeMillis) }")
    public static func delayEach(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        timeMillis: Swift.Int64
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @available(*, unavailable, message: "Use 'onStart { delay(timeMillis) }'. Replacement: onStart { delay(timeMillis) }")
    public static func delayFlow(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        timeMillis: Swift.Int64
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    public static func distinctUntilChanged(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_distinctUntilChanged__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func distinctUntilChanged(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        areEquivalent: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_distinctUntilChanged__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U29202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Bool = areEquivalent
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _result = originalBlock(_arg0, _arg1)
                return _result
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @available(*, unavailable, message: "Applying 'distinctUntilChanged' to StateFlow has no effect. See the StateFlow documentation on Operator Fusion.. Replacement: this")
    public static func distinctUntilChanged(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    public static func distinctUntilChangedBy(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        keySelector: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_distinctUntilChangedBy__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = keySelector
            return { (arg0: Swift.UnsafeMutableRawPointer?) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _result = originalBlock(_arg0)
                return _result.map { it in it.__externalRCRef() } ?? nil
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func drop(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        count: Swift.Int32
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_drop__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Int32__(receiver.wrapped.__externalRCRef(), count), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func dropWhile(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        predicate: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_dropWhile__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Bool = predicate
            return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Bool) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func emitAll(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector,
        channel: any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
    ) async throws -> Swift.Void {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_emitAll__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ReceiveChannel__(receiver.__externalRCRef(), channel.__externalRCRef(), {
                let originalBlock: (Swift.Void) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Void = { arg0; return () }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func emitAll(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector,
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) async throws -> Swift.Void {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_emitAll__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.__externalRCRef(), flow.wrapped.__externalRCRef(), {
                let originalBlock: (Swift.Void) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Void = { arg0; return () }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func filter(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        predicate: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_filter__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Bool = predicate
            return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Bool) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @available(*, unavailable, message: "Declaration uses unsupported types")
    public static func filterIsInstance(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow,
        klass: Swift.Never
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<any KotlinRuntimeSupport._KotlinBridgeable> {
        fatalError()
    }
    public static func filterNot(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        predicate: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_filterNot__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Bool = predicate
            return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Bool) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func filterNotNull(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<any KotlinRuntimeSupport._KotlinBridgeable> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<any KotlinRuntimeSupport._KotlinBridgeable>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_filterNotNull__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func first(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_first__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func first(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        predicate: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Bool
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_first__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Bool = predicate
                return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _continuation: (Swift.Bool) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                    }()
                    let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                    }()
                    let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                    let _result = withKotlinTask(_continuation, _exception, _cancellation){
                        try await originalBlock(_arg0)
                    }
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func firstOrNull(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_firstOrNull__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func firstOrNull(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        predicate: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Bool
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_firstOrNull__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Bool = predicate
                return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _continuation: (Swift.Bool) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                    }()
                    let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                    }()
                    let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                    let _result = withKotlinTask(_continuation, _exception, _cancellation){
                        try await originalBlock(_arg0)
                    }
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    @available(*, unavailable, message: "Flow analogue is 'flatMapConcat'. Replacement: flatMapConcat(mapper)")
    public static func flatMap(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        mapper: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func flatMapConcat(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flatMapConcat__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(pointerToBlock.__externalRCRef()!, _1.wrapped.__externalRCRef()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func flatMapLatest(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flatMapLatest__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(pointerToBlock.__externalRCRef()!, _1.wrapped.__externalRCRef()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func flatMapMerge(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        concurrency: Swift.Int32,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flatMapMerge__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Int32_U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), concurrency, {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(pointerToBlock.__externalRCRef()!, _1.wrapped.__externalRCRef()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @available(*, unavailable, message: "Flow analogue of 'flatten' is 'flattenConcat'. Replacement: flattenConcat()")
    public static func flatten(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func flattenConcat(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flattenConcat__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____(receiver.wrapped.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func flattenMerge(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>>,
        concurrency: Swift.Int32
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flattenMerge__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____Swift_Int32__(receiver.wrapped.__externalRCRef(), concurrency), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func flowOn(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flowOn__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(receiver.wrapped.__externalRCRef(), context.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @available(*, unavailable, message: "Applying 'flowOn' to SharedFlow has no effect. See the SharedFlow documentation on Operator Fusion.. Replacement: this")
    public static func flowOn(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    public static func fold(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        initial: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        operation: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_fold__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), initial.map { it in it.__externalRCRef() } ?? nil, {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = operation
                return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                    }()
                    let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                    }()
                    let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                    let _result = withKotlinTask(_continuation, _exception, _cancellation){
                        try await originalBlock(_arg0, _arg1)
                    }
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    @available(*, unavailable, message: "Flow analogue of 'forEach' is 'collect'. Replacement: collect(action)")
    public static func forEach(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        action: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Void
    ) -> Swift.Void {
        fatalError()
    }
    public static func getAndUpdate(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedMutableStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        function: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_flow_getAndUpdate__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedMutableStateFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = function
            return { (arg0: Swift.UnsafeMutableRawPointer?) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _result = originalBlock(_arg0)
                return _result.map { it in it.__externalRCRef() } ?? nil
            }
        }()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public static func last(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_last__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func lastOrNull(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_lastOrNull__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func launchIn(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        scope: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.Job {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_launchIn__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope__(receiver.wrapped.__externalRCRef(), scope.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job
    }
    public static func map(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_map__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func mapLatest(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_mapLatest__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func mapNotNull(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<any KotlinRuntimeSupport._KotlinBridgeable> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<any KotlinRuntimeSupport._KotlinBridgeable>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_mapNotNull__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @available(*, unavailable, message: "Flow analogue of 'merge' is 'flattenConcat'. Replacement: flattenConcat()")
    public static func merge(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @available(*, unavailable, message: "Collect flow in the desired context instead")
    public static func observeOn(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    public static func onCompletion(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        action: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, ExportedKotlinPackages.kotlin.Throwable?) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_onCompletion__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_U20Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) async throws -> Swift.Void = action
            return { (arg0: Swift.UnsafeMutableRawPointer, arg1: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
                let _arg1: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable> = { switch arg1 { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func onEach(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        action: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_onEach__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Void = action
            return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func onEmpty(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        action: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_onEmpty__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollectorU2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector) async throws -> Swift.Void = action
            return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @available(*, unavailable, message: "Flow analogue of 'onErrorXxx' is 'catch'. Use 'catch { emitAll(fallback) }'. Replacement: catch { emitAll(fallback) }")
    public static func onErrorResume(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        fallback: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'onErrorXxx' is 'catch'. Use 'catch { emitAll(fallback) }'. Replacement: catch { emitAll(fallback) }")
    public static func onErrorResumeNext(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        fallback: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'onErrorXxx' is 'catch'. Use 'catch { emit(fallback) }'. Replacement: catch { emit(fallback) }")
    public static func onErrorReturn(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        fallback: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'onErrorXxx' is 'catch'. Use 'catch { e -> if (predicate(e)) emit(fallback) else throw e }'. Replacement: catch { e -> if (predicate(e)) emit(fallback) else throw e }")
    public static func onErrorReturn(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        fallback: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        predicate: @escaping (ExportedKotlinPackages.kotlin.Throwable) -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    public static func onStart(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        action: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_onStart__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollectorU2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector) async throws -> Swift.Void = action
            return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func onSubscription(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        action: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedSharedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_onSubscription__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollectorU2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector) async throws -> Swift.Void = action
            return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func produceIn(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        scope: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_produceIn__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope__(receiver.wrapped.__externalRCRef(), scope.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
    }
    @available(*, unavailable, message: """
Flow analogue of 'publish()' is 'shareIn'.
publish().connect() is the default strategy (no extra call is needed),
publish().autoConnect() translates to 'started = SharingStared.Lazily' argument,
publish().refCount() translates to 'started = SharingStared.WhileSubscribed()' argument.. Replacement: this.shareIn(scope, 0)
""")
    public static func publish(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @available(*, unavailable, message: """
Flow analogue of 'publish(bufferSize)' is 'buffer' followed by 'shareIn'.
publish().connect() is the default strategy (no extra call is needed),
publish().autoConnect() translates to 'started = SharingStared.Lazily' argument,
publish().refCount() translates to 'started = SharingStared.WhileSubscribed()' argument.. Replacement: this.buffer(bufferSize).shareIn(scope, 0)
""")
    public static func publish(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        bufferSize: Swift.Int32
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @available(*, unavailable, message: "Collect flow in the desired context instead")
    public static func publishOn(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    public static func receiveAsFlow(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_receiveAsFlow__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ReceiveChannel__(receiver.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func reduce(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        operation: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_reduce__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = operation
                return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                    }()
                    let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                    }()
                    let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                    let _result = withKotlinTask(_continuation, _exception, _cancellation){
                        try await originalBlock(_arg0, _arg1)
                    }
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    @available(*, unavailable, message: """
Flow analogue of 'replay()' is 'shareIn' with unlimited replay.
replay().connect() is the default strategy (no extra call is needed),
replay().autoConnect() translates to 'started = SharingStared.Lazily' argument,
replay().refCount() translates to 'started = SharingStared.WhileSubscribed()' argument.. Replacement: this.shareIn(scope, Int.MAX_VALUE)
""")
    public static func replay(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @available(*, unavailable, message: """
Flow analogue of 'replay(bufferSize)' is 'shareIn' with the specified replay parameter.
replay().connect() is the default strategy (no extra call is needed),
replay().autoConnect() translates to 'started = SharingStared.Lazily' argument,
replay().refCount() translates to 'started = SharingStared.WhileSubscribed()' argument.. Replacement: this.shareIn(scope, bufferSize)
""")
    public static func replay(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        bufferSize: Swift.Int32
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    public static func retry(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        retries: Swift.Int64,
        predicate: @escaping (ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_retry__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Int64_U28ExportedKotlinPackages_kotlin_ThrowableU2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), retries, {
            let originalBlock: (ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Bool = predicate
            return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: ExportedKotlinPackages.kotlin.Throwable = ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: arg0)
                let _continuation: (Swift.Bool) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @available(*, deprecated, message: "SharedFlow never completes, so this operator has no effect.. Replacement: this")
    public static func retry(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        retries: Swift.Int64,
        predicate: @escaping (ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_retry__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Int64_U28ExportedKotlinPackages_kotlin_ThrowableU2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), retries, {
            let originalBlock: (ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Bool = predicate
            return { (arg0: Swift.UnsafeMutableRawPointer, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: ExportedKotlinPackages.kotlin.Throwable = ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: arg0)
                let _continuation: (Swift.Bool) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func retryWhen(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        predicate: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, ExportedKotlinPackages.kotlin.Throwable, Swift.Int64) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_retryWhen__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_U20ExportedKotlinPackages_kotlin_Throwable_U20Swift_Int64U2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, ExportedKotlinPackages.kotlin.Throwable, Swift.Int64) async throws -> Swift.Bool = predicate
            return { (arg0: Swift.UnsafeMutableRawPointer, arg1: Swift.UnsafeMutableRawPointer, arg2: Swift.Int64, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
                let _arg1: ExportedKotlinPackages.kotlin.Throwable = ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: arg1)
                let _arg2: Swift.Int64 = arg2
                let _continuation: (Swift.Bool) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1, _arg2)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @available(*, deprecated, message: "SharedFlow never completes, so this operator has no effect.. Replacement: this")
    public static func retryWhen(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        predicate: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, ExportedKotlinPackages.kotlin.Throwable, Swift.Int64) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_retryWhen__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_U20ExportedKotlinPackages_kotlin_Throwable_U20Swift_Int64U2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, ExportedKotlinPackages.kotlin.Throwable, Swift.Int64) async throws -> Swift.Bool = predicate
            return { (arg0: Swift.UnsafeMutableRawPointer, arg1: Swift.UnsafeMutableRawPointer, arg2: Swift.Int64, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
                let _arg1: ExportedKotlinPackages.kotlin.Throwable = ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: arg1)
                let _arg2: Swift.Int64 = arg2
                let _continuation: (Swift.Bool) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1, _arg2)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func runningFold(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        initial: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        operation: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_runningFold__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), initial.map { it in it.__externalRCRef() } ?? nil, {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = operation
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func runningReduce(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        operation: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_runningReduce__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = operation
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static func sample(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        periodMillis: Swift.Int64
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_sample__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Int64__(receiver.wrapped.__externalRCRef(), periodMillis), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static func sample(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        period: ExportedKotlinPackages.kotlin.time.Duration
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_sample__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___ExportedKotlinPackages_kotlin_time_Duration__(receiver.wrapped.__externalRCRef(), period.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func scan(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        initial: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        operation: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_scan__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), initial.map { it in it.__externalRCRef() } ?? nil, {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = operation
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @available(*, unavailable, message: "Flow has less verbose 'scan' shortcut. Replacement: scan(initial, operation)")
    public static func scanFold(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        initial: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        operation: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @available(*, unavailable, message: "'scanReduce' was renamed to 'runningReduce' to be consistent with Kotlin standard library. Replacement: runningReduce(operation)")
    public static func scanReduce(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        operation: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    public static func shareIn(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        scope: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        started: any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted,
        replay: Swift.Int32
    ) -> any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedSharedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_shareIn__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_SharingStarted_Swift_Int32__(receiver.wrapped.__externalRCRef(), scope.__externalRCRef(), started.__externalRCRef(), replay), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func single(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_single__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func singleOrNull(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_singleOrNull__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    @available(*, unavailable, message: "Flow analogue of 'skip' is 'drop'. Replacement: drop(count)")
    public static func skip(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        count: Swift.Int32
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'startWith' is 'onStart'. Use 'onStart { emit(value) }'. Replacement: onStart { emit(value) }")
    public static func startWith(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'startWith' is 'onStart'. Use 'onStart { emitAll(other) }'. Replacement: onStart { emitAll(other) }")
    public static func startWith(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        other: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    public static func stateIn(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        scope: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
    ) async throws -> any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_stateIn__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope__(receiver.wrapped.__externalRCRef(), scope.__externalRCRef(), {
                let originalBlock: (any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer) in
                    let _arg0: any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> = KotlinCoroutineSupport._KotlinTypedStateFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func stateIn(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        scope: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        started: any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted,
        initialValue: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedStateFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_stateIn__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_SharingStarted_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), scope.__externalRCRef(), started.__externalRCRef(), initialValue.map { it in it.__externalRCRef() } ?? nil), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @available(*, unavailable, message: "Use 'launchIn' with 'onEach', 'onCompletion' and 'catch' instead")
    public static func subscribe(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> Swift.Void {
        fatalError()
    }
    @available(*, unavailable, message: "Use 'launchIn' with 'onEach', 'onCompletion' and 'catch' instead")
    public static func subscribe(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        onEach: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Void
    ) -> Swift.Void {
        fatalError()
    }
    @available(*, unavailable, message: "Use 'launchIn' with 'onEach', 'onCompletion' and 'catch' instead")
    public static func subscribe(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        onEach: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Void,
        onError: @escaping (ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Void
    ) -> Swift.Void {
        fatalError()
    }
    @available(*, unavailable, message: "Use 'flowOn' instead")
    public static func subscribeOn(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogues of 'switchMap' are 'transformLatest', 'flatMapLatest' and 'mapLatest'. Replacement: this.flatMapLatest(transform)")
    public static func switchMap(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    public static func take(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        count: Swift.Int32
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_take__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Int32__(receiver.wrapped.__externalRCRef(), count), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func takeWhile(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        predicate: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_takeWhile__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Bool = predicate
            return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Bool) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static func timeout(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        timeout: ExportedKotlinPackages.kotlin.time.Duration
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_timeout__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___ExportedKotlinPackages_kotlin_time_Duration__(receiver.wrapped.__externalRCRef(), timeout.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func toCollection(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        destination: any ExportedKotlinPackages.kotlin.collections.MutableCollection
    ) async throws -> any ExportedKotlinPackages.kotlin.collections.MutableCollection {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_toCollection__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20ExportedKotlinPackages_kotlin_collections_MutableCollection__(receiver.wrapped.__externalRCRef(), destination.__externalRCRef(), {
                let originalBlock: (any ExportedKotlinPackages.kotlin.collections.MutableCollection) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer) in
                    let _arg0: any ExportedKotlinPackages.kotlin.collections.MutableCollection = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableCollection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableCollection
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func toList(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        destination: any ExportedKotlinPackages.kotlin.collections.MutableList
    ) async throws -> [(any KotlinRuntimeSupport._KotlinBridgeable)?] {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_toList__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20ExportedKotlinPackages_kotlin_collections_MutableList__(receiver.wrapped.__externalRCRef(), destination.__externalRCRef(), {
                let originalBlock: (Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>) -> Swift.Void = continuation
                return { (arg0: Any) in
                    let _arg0: Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> = arg0 as! Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    @available(*, deprecated, message: "SharedFlow never completes, so this terminal operation never completes.")
    public static func toList(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) async throws -> [(any KotlinRuntimeSupport._KotlinBridgeable)?] {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_toList__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
                let originalBlock: (Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>) -> Swift.Void = continuation
                return { (arg0: Any) in
                    let _arg0: Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> = arg0 as! Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func toList(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        destination: any ExportedKotlinPackages.kotlin.collections.MutableList
    ) async throws -> Swift.Never {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_toList__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20ExportedKotlinPackages_kotlin_collections_MutableList__(receiver.wrapped.__externalRCRef(), destination.__externalRCRef(), {
                let originalBlock: (Swift.Never) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Never = { arg0; fatalError() }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func toSet(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        destination: any ExportedKotlinPackages.kotlin.collections.MutableSet
    ) async throws -> Swift.Set<Swift.Optional<Swift.AnyHashable>> {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_toSet__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20ExportedKotlinPackages_kotlin_collections_MutableSet__(receiver.wrapped.__externalRCRef(), destination.__externalRCRef(), {
                let originalBlock: (Swift.Set<Swift.Optional<Swift.AnyHashable>>) -> Swift.Void = continuation
                return { (arg0: Any) in
                    let _arg0: Swift.Set<Swift.Optional<Swift.AnyHashable>> = arg0 as! Swift.Set<Swift.Optional<Swift.AnyHashable>>
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    @available(*, deprecated, message: "SharedFlow never completes, so this terminal operation never completes.")
    public static func toSet(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) async throws -> Swift.Set<Swift.Optional<Swift.AnyHashable>> {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_toSet__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
                let originalBlock: (Swift.Set<Swift.Optional<Swift.AnyHashable>>) -> Swift.Void = continuation
                return { (arg0: Any) in
                    let _arg0: Swift.Set<Swift.Optional<Swift.AnyHashable>> = arg0 as! Swift.Set<Swift.Optional<Swift.AnyHashable>>
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func toSet(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        destination: any ExportedKotlinPackages.kotlin.collections.MutableSet
    ) async throws -> Swift.Never {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_toSet__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20ExportedKotlinPackages_kotlin_collections_MutableSet__(receiver.wrapped.__externalRCRef(), destination.__externalRCRef(), {
                let originalBlock: (Swift.Never) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Never = { arg0; fatalError() }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func transform(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_transform__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Void = transform
            return { (arg0: Swift.UnsafeMutableRawPointer, arg1: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
                let _arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func transformLatest(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_transformLatest__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Void = transform
            return { (arg0: Swift.UnsafeMutableRawPointer, arg1: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
                let _arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func transformWhile(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_transformWhile__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Bool = transform
            return { (arg0: Swift.UnsafeMutableRawPointer, arg1: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
                let _arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Bool) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func update(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedMutableStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        function: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return { kotlinx_coroutines_flow_update__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedMutableStateFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = function
            return { (arg0: Swift.UnsafeMutableRawPointer?) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _result = originalBlock(_arg0)
                return _result.map { it in it.__externalRCRef() } ?? nil
            }
        }()); return () }()
    }
    public static func updateAndGet(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedMutableStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        function: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_flow_updateAndGet__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedMutableStateFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = function
            return { (arg0: Swift.UnsafeMutableRawPointer?) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _result = originalBlock(_arg0)
                return _result.map { it in it.__externalRCRef() } ?? nil
            }
        }()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public static func withIndex(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<ExportedKotlinPackages.kotlin.collections.IndexedValue> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<ExportedKotlinPackages.kotlin.collections.IndexedValue>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_withIndex__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, ExportedKotlinPackages.kotlin.collections.IndexedValue.Type.self)
    }
    public static func zip(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        other: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_zip__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), other.wrapped.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0, _arg1)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
    public static func flowCollector(
        function: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_FlowCollector__TypesOfArguments__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Void__({
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Void = function
            return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Void) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
    }
    public static func sharingStarted(
        function: @escaping (any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Int32>) -> any KotlinCoroutineSupport.KotlinTypedFlow<ExportedKotlinPackages.kotlinx.coroutines.flow.SharingCommand>
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_SharingStarted__TypesOfArguments__U28anyU20KotlinCoroutineSupport_KotlinTypedStateFlow_Swift_Int32_U29202D_U20anyU20KotlinCoroutineSupport_KotlinTypedFlow_ExportedKotlinPackages_kotlinx_coroutines_flow_SharingCommand___({
            let originalBlock: (any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Int32>) -> any KotlinCoroutineSupport.KotlinTypedFlow<ExportedKotlinPackages.kotlinx.coroutines.flow.SharingCommand> = function
            return { (arg0: Swift.UnsafeMutableRawPointer) in
                let _arg0: any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Int32> = KotlinCoroutineSupport._KotlinTypedStateFlowImpl<Swift.Int32>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow, Swift.Int32.Type.self)
                let _result = originalBlock(_arg0)
                return _result.wrapped.__externalRCRef()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public enum SelectClause_SealedType: KotlinRuntimeSupport.SealedType {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        case selectClause0(ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0_SealedType)
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        case selectClause1(ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1_SealedType)
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        case selectClause2(ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2_SealedType)
        public var value: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause {
            get {
                switch self {
                case let .selectClause0(type): type.value
                case let .selectClause1(type): type.value
                case let .selectClause2(type): type.value
                }
            }
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public enum SelectClause0_SealedType: KotlinRuntimeSupport.SealedType {
        case unknown(ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0_SealedType.Unknown)
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public struct Unknown: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0
            init(
                _ value: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0
            ) {
                self.value = value
            }
        }
        public var value: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0 {
            get {
                switch self {
                case let .unknown(type): type.value
                }
            }
        }
    }
    public protocol SelectBuilder: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.selects._SelectBuilder {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func invoke(
            _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0,
            block: @escaping () async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func invoke(
            _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1,
            block: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func invoke(
            _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2,
            param: (any KotlinRuntimeSupport._KotlinBridgeable)?,
            block: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func invoke(
            _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2,
            block: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_selects_SelectBuilder)
    public protocol _SelectBuilder {
    }
    public protocol __SelectBuilder: KotlinRuntimeSupport._KotlinBridgeable {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol SelectClause: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        var clauseObject: any KotlinRuntimeSupport._KotlinBridgeable {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause_SealedType
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause)
    public protocol _SelectClause {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol __SelectClause: KotlinRuntimeSupport._KotlinBridgeable {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol SelectClause0: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause, ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause0 {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0_SealedType
        @_spi(kotlinx$coroutines$InternalCoroutinesApi) @_disfavoredOverload
        func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause_SealedType
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause0)
    public protocol _SelectClause0: ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol __SelectClause0: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectClause {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol SelectClause1: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause, ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause1 {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause_SealedType
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause1)
    public protocol _SelectClause1: ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol __SelectClause1: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectClause {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol SelectClause2: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause, ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause2 {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause_SealedType
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause2)
    public protocol _SelectClause2: ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol __SelectClause2: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectClause {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol SelectInstance: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.selects._SelectInstance {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        var context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func disposeOnCompletion(
            disposableHandle: any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
        ) -> Swift.Void
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func selectInRegistrationPhase(
            internalResult: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func trySelect(
            clauseObject: any KotlinRuntimeSupport._KotlinBridgeable,
            result: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_selects_SelectInstance)
    public protocol _SelectInstance {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol __SelectInstance: KotlinRuntimeSupport._KotlinBridgeable {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public struct SelectClause1_SealedType: KotlinRuntimeSupport.SealedType {
        public let value: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1
        init(
            _ value: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1
        ) {
            self.value = value
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public struct SelectClause2_SealedType: KotlinRuntimeSupport.SealedType {
        public let value: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2
        init(
            _ value: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2
        ) {
            self.value = value
        }
    }
    public static func select(
        builder: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder) -> Swift.Void
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_selects_select__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectBuilderU29202D_U20Swift_Void__({
                let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder) -> Swift.Void = builder
                return { (arg0: Swift.UnsafeMutableRawPointer) in
                    let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func selectUnbiased(
        builder: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder) -> Swift.Void
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_selects_selectUnbiased__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectBuilderU29202D_U20Swift_Void__({
                let originalBlock: (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder) -> Swift.Void = builder
                return { (arg0: Swift.UnsafeMutableRawPointer) in
                    let _arg0: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func onTimeout(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder,
        timeMillis: Swift.Int64,
        block: @escaping () async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return { kotlinx_coroutines_selects_onTimeout__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectBuilder_Swift_Int64_U282920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.__externalRCRef(), timeMillis, {
            let originalBlock: () async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
            return { (continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock()
                }
                return { _result; return true }()
            }
        }()); return () }()
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func onTimeout(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder,
        timeout: ExportedKotlinPackages.kotlin.time.Duration,
        block: @escaping () async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return { kotlinx_coroutines_selects_onTimeout__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectBuilder_ExportedKotlinPackages_kotlin_time_Duration_U282920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.__externalRCRef(), timeout.__externalRCRef(), {
            let originalBlock: () async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
            return { (continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock()
                }
                return { _result; return true }()
            }
        }()); return () }()
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.`internal` {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public typealias SynchronizedObject = ExportedKotlinPackages.kotlinx.atomicfu.locks.SynchronizedObject
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol MainDispatcherFactory: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.`internal`._MainDispatcherFactory {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        var loadPriority: Swift.Int32 {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func createDispatcher(
            allFactories: [any ExportedKotlinPackages.kotlinx.coroutines.`internal`.MainDispatcherFactory]
        ) -> ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func hintOnError() -> Swift.String?
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_internal_MainDispatcherFactory)
    public protocol _MainDispatcherFactory {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol __MainDispatcherFactory: KotlinRuntimeSupport._KotlinBridgeable {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol ThreadSafeHeapNode: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.`internal`._ThreadSafeHeapNode {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        var index: Swift.Int32 {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            set
        }
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_internal_ThreadSafeHeapNode)
    public protocol _ThreadSafeHeapNode {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol __ThreadSafeHeapNode: KotlinRuntimeSupport._KotlinBridgeable {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    open class AtomicOp: ExportedKotlinPackages.kotlinx.coroutines.`internal`.OpDescriptor {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open override var atomicOp: ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                if Self.self == ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp.self {
                    return ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp.__createClassWrapper(externalRCRef: kotlinx_coroutines_internal_AtomicOp_atomicOp_get(self.__externalRCRef()))
                } else {
                    return ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp.__createClassWrapper(externalRCRef: kotlinx_coroutines_internal_AtomicOp_atomicOp_get_direct(self.__externalRCRef()))
                }
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func complete(
            affected: (any KotlinRuntimeSupport._KotlinBridgeable)?,
            failure: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp.self {
                return { kotlinx_coroutines_internal_AtomicOp_complete__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), affected.map { it in it.__externalRCRef() } ?? nil, failure.map { it in it.__externalRCRef() } ?? nil); return () }()
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp.complete': a Swift subclass must override it and must not call super.")
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final override func perform(
            affected: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlinx_coroutines_internal_AtomicOp_perform__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), affected.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func prepare(
            affected: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp.self {
                return { switch kotlinx_coroutines_internal_AtomicOp_prepare__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), affected.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp.prepare': a Swift subclass must override it and must not call super.")
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp.self, "ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_internal_AtomicOp_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    open class LockFreeLinkedListHead: ExportedKotlinPackages.kotlinx.coroutines.`internal`.LockFreeLinkedListNode {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final var isEmpty: Swift.Bool {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return kotlinx_coroutines_internal_LockFreeLinkedListHead_isEmpty_get(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open override var isRemoved: Swift.Bool {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                if Self.self == ExportedKotlinPackages.kotlinx.coroutines.`internal`.LockFreeLinkedListHead.self {
                    return kotlinx_coroutines_internal_LockFreeLinkedListHead_isRemoved_get(self.__externalRCRef())
                } else {
                    return kotlinx_coroutines_internal_LockFreeLinkedListHead_isRemoved_get_direct(self.__externalRCRef())
                }
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func remove() -> Swift.Never {
            return { kotlinx_coroutines_internal_LockFreeLinkedListHead_remove(self.__externalRCRef()); fatalError() }()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public override init() {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlinx.coroutines.`internal`.LockFreeLinkedListHead.self {
                 __kt = kotlinx_coroutines_internal_LockFreeLinkedListHead_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_internal_LockFreeLinkedListHead_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    open class LockFreeLinkedListNode: KotlinRuntime.KotlinBase {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open var isRemoved: Swift.Bool {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                if Self.self == ExportedKotlinPackages.kotlinx.coroutines.`internal`.LockFreeLinkedListNode.self {
                    return kotlinx_coroutines_internal_LockFreeLinkedListNode_isRemoved_get(self.__externalRCRef())
                } else {
                    return kotlinx_coroutines_internal_LockFreeLinkedListNode_isRemoved_get_direct(self.__externalRCRef())
                }
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final var next: any KotlinRuntimeSupport._KotlinBridgeable {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: kotlinx_coroutines_internal_LockFreeLinkedListNode_next_get(self.__externalRCRef()))
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi) @available(*, unavailable, message: "Declaration uses unsupported types")
        public final var nextNode: Swift.Never {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                fatalError()
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi) @available(*, unavailable, message: "Declaration uses unsupported types")
        public final var prevNode: Swift.Never {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                fatalError()
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi) @available(*, unavailable, message: "Declaration uses unsupported types")
        public final func addLast(
            node: Swift.Never
        ) -> Swift.Void {
            fatalError()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi) @available(*, unavailable, message: "Declaration uses unsupported types")
        public final func addLastIf(
            node: Swift.Never,
            condition: @escaping () -> Swift.Bool
        ) -> Swift.Bool {
            fatalError()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi) @available(*, unavailable, message: "Declaration uses unsupported types")
        public final func addOneIfEmpty(
            node: Swift.Never
        ) -> Swift.Bool {
            fatalError()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func remove() -> Swift.Bool {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.`internal`.LockFreeLinkedListNode.self {
                return kotlinx_coroutines_internal_LockFreeLinkedListNode_remove(self.__externalRCRef())
            } else {
                return kotlinx_coroutines_internal_LockFreeLinkedListNode_remove_direct(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func toString() -> Swift.String {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.`internal`.LockFreeLinkedListNode.self {
                return kotlinx_coroutines_internal_LockFreeLinkedListNode_toString(self.__externalRCRef())
            } else {
                return kotlinx_coroutines_internal_LockFreeLinkedListNode_toString_direct(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public init() {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlinx.coroutines.`internal`.LockFreeLinkedListNode.self {
                 __kt = kotlinx_coroutines_internal_LockFreeLinkedListNode_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_internal_LockFreeLinkedListNode_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class OpDescriptor: KotlinRuntime.KotlinBase {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open var atomicOp: ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp? {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                if Self.self == ExportedKotlinPackages.kotlinx.coroutines.`internal`.OpDescriptor.self {
                    return { switch kotlinx_coroutines_internal_OpDescriptor_atomicOp_get(self.__externalRCRef()) { case nil: .none; case let res?: ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp.__createClassWrapper(externalRCRef: res); } }()
                } else {
                    fatalError("Cannot invoke the inherited implementation of abstract property 'ExportedKotlinPackages.kotlinx.coroutines.`internal`.OpDescriptor.atomicOp': a Swift subclass must override it and must not call super.")
                }
            }
        }
        open func perform(
            affected: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.`internal`.OpDescriptor.self {
                return { switch kotlinx_coroutines_internal_OpDescriptor_perform__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), affected.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlinx.coroutines.`internal`.OpDescriptor.perform': a Swift subclass must override it and must not call super.")
            }
        }
        open func toString() -> Swift.String {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.`internal`.OpDescriptor.self {
                return kotlinx_coroutines_internal_OpDescriptor_toString(self.__externalRCRef())
            } else {
                return kotlinx_coroutines_internal_OpDescriptor_toString_direct(self.__externalRCRef())
            }
        }
        public init() {
            precondition(Self.self != ExportedKotlinPackages.kotlinx.coroutines.`internal`.OpDescriptor.self, "ExportedKotlinPackages.kotlinx.coroutines.`internal`.OpDescriptor is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_internal_OpDescriptor_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public static func synchronized(
        lock: ExportedKotlinPackages.kotlinx.coroutines.`internal`.SynchronizedObject,
        block: @escaping () -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_internal_synchronized__TypesOfArguments__ExportedKotlinPackages_kotlinx_atomicfu_locks_SynchronizedObject_U2829202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(lock.__externalRCRef(), {
            let originalBlock: () -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
            return {
                let _result = originalBlock()
                return _result.map { it in it.__externalRCRef() } ?? nil
            }
        }()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public static func synchronizedImpl(
        lock: ExportedKotlinPackages.kotlinx.coroutines.`internal`.SynchronizedObject,
        block: @escaping () -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_internal_synchronizedImpl__TypesOfArguments__ExportedKotlinPackages_kotlinx_atomicfu_locks_SynchronizedObject_U2829202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(lock.__externalRCRef(), {
            let originalBlock: () -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
            return {
                let _result = originalBlock()
                return _result.map { it in it.__externalRCRef() } ?? nil
            }
        }()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public static func resumeCancellableWith(
        _ receiver: any ExportedKotlinPackages.kotlin.coroutines.Continuation,
        result: ExportedKotlinPackages.kotlin.Result,
        onCancellation: ((ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void)?
    ) -> Swift.Void {
        return { kotlinx_coroutines_internal_resumeCancellableWith__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation_ExportedKotlinPackages_kotlin_Result_Swift_Optional_U28ExportedKotlinPackages_kotlin_ThrowableU29202D_U20Swift_Void___(receiver.__externalRCRef(), result.__externalRCRef(), onCancellation.map { it in {
            let originalBlock: (ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void = it
            return { (arg0: Swift.UnsafeMutableRawPointer) in
                let _arg0: ExportedKotlinPackages.kotlin.Throwable = ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: arg0)
                let _result = originalBlock(_arg0)
                return { _result; return true }()
            }
        }() } ?? nil); return () }()
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.sync {
    public protocol Mutex: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.sync._Mutex {
        var isLocked: Swift.Bool {
            get
        }
        @available(*, deprecated, message: "Mutex.onLock deprecated without replacement. For additional details please refer to #2794") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        var onLock: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2 {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get
        }
        func holdsLock(
            owner: any KotlinRuntimeSupport._KotlinBridgeable
        ) -> Swift.Bool
        func lock(
            owner: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) async throws -> Swift.Void
        func tryLock(
            owner: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func unlock(
            owner: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_sync_Mutex)
    public protocol _Mutex {
    }
    public protocol __Mutex: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol Semaphore: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.sync._Semaphore {
        var availablePermits: Swift.Int32 {
            get
        }
        func acquire() async throws -> Swift.Void
        func tryAcquire() -> Swift.Bool
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_sync_Semaphore)
    public protocol _Semaphore {
    }
    public protocol __Semaphore: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public static func mutex(
        locked: Swift.Bool
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_sync_Mutex__TypesOfArguments__Swift_Bool__(locked), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex
    }
    public static func semaphore(
        permits: Swift.Int32,
        acquiredPermits: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.sync.Semaphore {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_sync_Semaphore__TypesOfArguments__Swift_Int32_Swift_Int32__(permits, acquiredPermits), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.sync.Semaphore.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.sync.Semaphore
    }
    public static func withLock(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex,
        owner: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        action: @escaping () -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_sync_withLock__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_sync_Mutex_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U2829202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.__externalRCRef(), owner.map { it in it.__externalRCRef() } ?? nil, {
                let originalBlock: () -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = action
                return {
                    let _result = originalBlock()
                    return _result.map { it in it.__externalRCRef() } ?? nil
                }
            }(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public static func withPermit(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.sync.Semaphore,
        action: @escaping () -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_sync_withPermit__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_sync_Semaphore_U2829202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.__externalRCRef(), {
                let originalBlock: () -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = action
                return {
                    let _result = originalBlock()
                    return _result.map { it in it.__externalRCRef() } ?? nil
                }
            }(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.`internal` {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol FusibleFlow: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`._FusibleFlow {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func fuse(
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
            capacity: Swift.Int32,
            onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow
        ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_flow_internal_FusibleFlow)
    public protocol _FusibleFlow: ExportedKotlinPackages.kotlinx.coroutines.flow._Flow {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol __FusibleFlow: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.flow.__Flow {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    open class ChannelFlow: KotlinRuntime.KotlinBase {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final var capacity: Swift.Int32 {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return kotlinx_coroutines_flow_internal_ChannelFlow_capacity_get(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final var context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_internal_ChannelFlow_context_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final var onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow(__externalRCRefUnsafe: kotlinx_coroutines_flow_internal_ChannelFlow_onBufferOverflow_get(self.__externalRCRef()), options: .asBestFittingWrapper)
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func collect(
            collector: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
        ) async throws -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.ChannelFlow.self {
                try await withKotlinContinuation { continuation, exception, cancellation in
                let _: Bool = kotlinx_coroutines_flow_internal_ChannelFlow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector__(self.__externalRCRef(), collector.__externalRCRef(), {
                    let originalBlock: (Swift.Void) -> Swift.Void = continuation
                    return { (arg0: Swift.Bool) in
                        let _arg0: Swift.Void = { arg0; return () }()
                        let _result = originalBlock(_arg0)
                        return { _result; return true }()
                    }
                }(), {
                    let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                    return { (arg0: Swift.UnsafeMutableRawPointer?) in
                        let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                        let _result = originalBlock(_arg0)
                        return { _result; return true }()
                    }
                }(), cancellation.__externalRCRef())
            }
            } else {
                try await withKotlinContinuation { continuation, exception, cancellation in
                let _: Bool = kotlinx_coroutines_flow_internal_ChannelFlow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector___direct(self.__externalRCRef(), collector.__externalRCRef(), {
                    let originalBlock: (Swift.Void) -> Swift.Void = continuation
                    return { (arg0: Swift.Bool) in
                        let _arg0: Swift.Void = { arg0; return () }()
                        let _result = originalBlock(_arg0)
                        return { _result; return true }()
                    }
                }(), {
                    let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                    return { (arg0: Swift.UnsafeMutableRawPointer?) in
                        let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                        let _result = originalBlock(_arg0)
                        return { _result; return true }()
                    }
                }(), cancellation.__externalRCRef())
            }
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func dropChannelOperators() -> (any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>)? {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.ChannelFlow.self {
                return { switch kotlinx_coroutines_flow_internal_ChannelFlow_dropChannelOperators(self.__externalRCRef()) { case nil: .none; case let res?: KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self); } }()
            } else {
                return { switch kotlinx_coroutines_flow_internal_ChannelFlow_dropChannelOperators_direct(self.__externalRCRef()) { case nil: .none; case let res?: KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self); } }()
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func fuse(
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
            capacity: Swift.Int32,
            onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow
        ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.ChannelFlow.self {
                return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_internal_ChannelFlow_fuse__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow__(self.__externalRCRef(), context.__externalRCRef(), capacity, onBufferOverflow.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
            } else {
                return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_internal_ChannelFlow_fuse__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow___direct(self.__externalRCRef(), context.__externalRCRef(), capacity, onBufferOverflow.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func produceImpl(
            scope: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.ChannelFlow.self {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_internal_ChannelFlow_produceImpl__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope__(self.__externalRCRef(), scope.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
            } else {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_internal_ChannelFlow_produceImpl__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope___direct(self.__externalRCRef(), scope.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func toString() -> Swift.String {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.ChannelFlow.self {
                return kotlinx_coroutines_flow_internal_ChannelFlow_toString(self.__externalRCRef())
            } else {
                return kotlinx_coroutines_flow_internal_ChannelFlow_toString_direct(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public init(
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
            capacity: Swift.Int32,
            onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow
        ) {
            precondition(Self.self != ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.ChannelFlow.self, "ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.ChannelFlow is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_flow_internal_ChannelFlow_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow__(__kt, context.__externalRCRef(), capacity, onBufferOverflow.__externalRCRef()); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public final class SendingCollector: KotlinRuntime.KotlinBase {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public func emit(
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) async throws -> Swift.Void {
            try await withKotlinContinuation { continuation, exception, cancellation in
                let _: Bool = kotlinx_coroutines_flow_internal_SendingCollector_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil, {
                    let originalBlock: (Swift.Void) -> Swift.Void = continuation
                    return { (arg0: Swift.Bool) in
                        let _arg0: Swift.Void = { arg0; return () }()
                        let _result = originalBlock(_arg0)
                        return { _result; return true }()
                    }
                }(), {
                    let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                    return { (arg0: Swift.UnsafeMutableRawPointer?) in
                        let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                        let _result = originalBlock(_arg0)
                        return { _result; return true }()
                    }
                }(), cancellation.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public init(
            channel: any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel
        ) {
            let __kt = kotlinx_coroutines_flow_internal_SendingCollector_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_flow_internal_SendingCollector_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_SendChannel__(__kt, channel.__externalRCRef()); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.Dispatchers {
    public var IO: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher {
        get {
            let receiver = self
            return ExportedKotlinPackages.kotlinx.coroutines.getIO(receiver)
        }
    }
}
extension ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
    public var isActive: Swift.Bool {
        get {
            let receiver = self
            return ExportedKotlinPackages.kotlinx.coroutines.getIsActive(receiver)
        }
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope {
    public var isActive: Swift.Bool {
        get {
            let receiver = self
            return ExportedKotlinPackages.kotlinx.coroutines.getIsActive(receiver)
        }
    }
}
extension ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
    public var job: any ExportedKotlinPackages.kotlinx.coroutines.Job {
        get {
            let receiver = self
            return ExportedKotlinPackages.kotlinx.coroutines.getJob(receiver)
        }
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope {
    public func async(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        start: ExportedKotlinPackages.kotlinx.coroutines.CoroutineStart,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.Deferred {
        let receiver = self
        return ExportedKotlinPackages.kotlinx.coroutines.async(receiver, context: context, start: start, block: block)
    }
}
extension ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
    public func cancel(
        cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
    ) -> Swift.Void {
        let receiver = self
        return ExportedKotlinPackages.kotlinx.coroutines.cancel(receiver, cause: cause)
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.Job {
    public func cancel(
        message: Swift.String,
        cause: ExportedKotlinPackages.kotlin.Throwable?
    ) -> Swift.Void {
        let receiver = self
        return ExportedKotlinPackages.kotlinx.coroutines.cancel(receiver, message: message, cause: cause)
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope {
    public func cancel(
        message: Swift.String,
        cause: ExportedKotlinPackages.kotlin.Throwable?
    ) -> Swift.Void {
        let receiver = self
        return ExportedKotlinPackages.kotlinx.coroutines.cancel(receiver, message: message, cause: cause)
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope {
    public func cancel(
        cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
    ) -> Swift.Void {
        let receiver = self
        return ExportedKotlinPackages.kotlinx.coroutines.cancel(receiver, cause: cause)
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.Job {
    public func cancelAndJoin() async throws -> Swift.Void {
        let receiver = self
        return try await ExportedKotlinPackages.kotlinx.coroutines.cancelAndJoin(receiver)
    }
}
extension ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
    public func cancelChildren(
        cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
    ) -> Swift.Void {
        let receiver = self
        return ExportedKotlinPackages.kotlinx.coroutines.cancelChildren(receiver, cause: cause)
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.Job {
    public func cancelChildren(
        cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
    ) -> Swift.Void {
        let receiver = self
        return ExportedKotlinPackages.kotlinx.coroutines.cancelChildren(receiver, cause: cause)
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred {
    public func completeWith(
        result: ExportedKotlinPackages.kotlin.Result
    ) -> Swift.Bool {
        let receiver = self
        return ExportedKotlinPackages.kotlinx.coroutines.completeWith(receiver, result: result)
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func disposeOnCancellation(
        handle: any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
    ) -> Swift.Void {
        let receiver = self
        return ExportedKotlinPackages.kotlinx.coroutines.disposeOnCancellation(receiver, handle: handle)
    }
}
extension ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
    public func ensureActive() -> Swift.Void {
        let receiver = self
        return ExportedKotlinPackages.kotlinx.coroutines.ensureActive(receiver)
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.Job {
    public func ensureActive() -> Swift.Void {
        let receiver = self
        return ExportedKotlinPackages.kotlinx.coroutines.ensureActive(receiver)
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope {
    public func ensureActive() -> Swift.Void {
        let receiver = self
        return ExportedKotlinPackages.kotlinx.coroutines.ensureActive(receiver)
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher {
    public func invoke(
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        let receiver = self
        return try await ExportedKotlinPackages.kotlinx.coroutines.invoke(receiver, block: block)
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope {
    public func launch(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        start: ExportedKotlinPackages.kotlinx.coroutines.CoroutineStart,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.Job {
        let receiver = self
        return ExportedKotlinPackages.kotlinx.coroutines.launch(receiver, context: context, start: start, block: block)
    }
}
extension ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
    public func newCoroutineContext(
        addedContext: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        let receiver = self
        return ExportedKotlinPackages.kotlinx.coroutines.newCoroutineContext(receiver, addedContext: addedContext)
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope {
    public func newCoroutineContext(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        let receiver = self
        return ExportedKotlinPackages.kotlinx.coroutines.newCoroutineContext(receiver, context: context)
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope {
    public func plus(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope {
        let receiver = self
        return ExportedKotlinPackages.kotlinx.coroutines.plus(receiver, context: context)
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation where Self : ExportedKotlinPackages.kotlinx.coroutines.__CancellableContinuation {
    public var isActive: Swift.Bool {
        get {
            return kotlinx_coroutines_CancellableContinuation_isActive_get(self.__externalRCRef())
        }
    }
    public var isCancelled: Swift.Bool {
        get {
            return kotlinx_coroutines_CancellableContinuation_isCancelled_get(self.__externalRCRef())
        }
    }
    public var isCompleted: Swift.Bool {
        get {
            return kotlinx_coroutines_CancellableContinuation_isCompleted_get(self.__externalRCRef())
        }
    }
    public func cancel(
        cause: ExportedKotlinPackages.kotlin.Throwable?
    ) -> Swift.Bool {
        return kotlinx_coroutines_CancellableContinuation_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(self.__externalRCRef(), cause.map { it in it.__externalRCRef() } ?? nil)
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func completeResume(
        token: any KotlinRuntimeSupport._KotlinBridgeable
    ) -> Swift.Void {
        return { kotlinx_coroutines_CancellableContinuation_completeResume__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(self.__externalRCRef(), token.__externalRCRef()); return () }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func initCancellability() -> Swift.Void {
        return { kotlinx_coroutines_CancellableContinuation_initCancellability(self.__externalRCRef()); return () }()
    }
    public func invokeOnCancellation(
        handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
    ) -> Swift.Void {
        return { kotlinx_coroutines_CancellableContinuation_invokeOnCancellation__TypesOfArguments__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), {
            let originalBlock: (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void = handler
            return { (arg0: Swift.UnsafeMutableRawPointer?) in
                let _arg0: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable> = { switch arg0 { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
                let _result = originalBlock(_arg0)
                return { _result; return true }()
            }
        }()); return () }()
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func resume(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        onCancellation: ((ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void)?
    ) -> Swift.Void {
        return { kotlinx_coroutines_CancellableContinuation_resume__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_U28ExportedKotlinPackages_kotlin_ThrowableU29202D_U20Swift_Void___(self.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil, onCancellation.map { it in {
            let originalBlock: (ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void = it
            return { (arg0: Swift.UnsafeMutableRawPointer) in
                let _arg0: ExportedKotlinPackages.kotlin.Throwable = ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: arg0)
                let _result = originalBlock(_arg0)
                return { _result; return true }()
            }
        }() } ?? nil); return () }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func tryResume(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        idempotent: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_CancellableContinuation_tryResume__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil, idempotent.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func tryResume(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        idempotent: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        onCancellation: ((ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_CancellableContinuation_tryResume__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_U28ExportedKotlinPackages_kotlin_ThrowableU29202D_U20Swift_Void___(self.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil, idempotent.map { it in it.__externalRCRef() } ?? nil, onCancellation.map { it in {
            let originalBlock: (ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void = it
            return { (arg0: Swift.UnsafeMutableRawPointer) in
                let _arg0: ExportedKotlinPackages.kotlin.Throwable = ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: arg0)
                let _result = originalBlock(_arg0)
                return { _result; return true }()
            }
        }() } ?? nil) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func tryResumeWithException(
        exception: ExportedKotlinPackages.kotlin.Throwable
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_CancellableContinuation_tryResumeWithException__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable__(self.__externalRCRef(), exception.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func resumeUndispatched(
        _ receiver: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher,
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return { kotlinx_coroutines_CancellableContinuation_resumeUndispatched__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_CoroutineDispatcher_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), receiver.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func resumeUndispatchedWithException(
        _ receiver: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher,
        exception: ExportedKotlinPackages.kotlin.Throwable
    ) -> Swift.Void {
        return { kotlinx_coroutines_CancellableContinuation_resumeUndispatchedWithException__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_CoroutineDispatcher_ExportedKotlinPackages_kotlin_Throwable__(self.__externalRCRef(), receiver.__externalRCRef(), exception.__externalRCRef()); return () }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines._CancellableContinuation {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation, ExportedKotlinPackages.kotlinx.coroutines.__CancellableContinuation where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._CancellableContinuation {
}
extension ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func completeResume(
        token: any KotlinRuntimeSupport._KotlinBridgeable
    ) -> Swift.Void {
        fatalError("'completeResume' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func initCancellability() -> Swift.Void {
        fatalError("'initCancellability' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func resume(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        onCancellation: ((ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void)?
    ) -> Swift.Void {
        fatalError("'resume' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func tryResume(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        idempotent: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        fatalError("'tryResume' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func tryResume(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        idempotent: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        onCancellation: ((ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        fatalError("'tryResume' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func tryResumeWithException(
        exception: ExportedKotlinPackages.kotlin.Throwable
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        fatalError("'tryResumeWithException' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func resumeUndispatched(
        _ receiver: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher,
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        fatalError("'resumeUndispatched' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func resumeUndispatchedWithException(
        _ receiver: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher,
        exception: ExportedKotlinPackages.kotlin.Throwable
    ) -> Swift.Void {
        fatalError("'resumeUndispatchedWithException' is an @_spi requirement that must be implemented by Swift conformers")
    }
}
@available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ChildHandle") @_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.ChildHandle where Self : ExportedKotlinPackages.kotlinx.coroutines.__ChildHandle {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)? {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return { switch kotlinx_coroutines_ChildHandle_parent_get(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job; } }()
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func childCancelled(
        cause: ExportedKotlinPackages.kotlin.Throwable
    ) -> Swift.Bool {
        return kotlinx_coroutines_ChildHandle_childCancelled__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable__(self.__externalRCRef(), cause.__externalRCRef())
    }
}
@available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ChildHandle") @_documentation(visibility: internal)
package extension KotlinRuntimeSupport._KotlinExistentialPenBox {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi) @available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ChildHandle") @_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.ChildHandle, ExportedKotlinPackages.kotlinx.coroutines.__ChildHandle where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._ChildHandle {
}
@available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ChildHandle")
extension ExportedKotlinPackages.kotlinx.coroutines.ChildHandle {
}
@available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ChildJob") @_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.ChildJob where Self : ExportedKotlinPackages.kotlinx.coroutines.__ChildJob {
}
@available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ChildJob") @_documentation(visibility: internal)
package extension KotlinRuntimeSupport._KotlinExistentialPenBox {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi) @available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ChildJob") @_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.ChildJob, ExportedKotlinPackages.kotlinx.coroutines.__ChildJob where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._ChildJob {
}
@available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ChildJob")
extension ExportedKotlinPackages.kotlinx.coroutines.ChildJob {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred where Self : ExportedKotlinPackages.kotlinx.coroutines.__CompletableDeferred {
    public func complete(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlinx_coroutines_CompletableDeferred_complete__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil)
    }
    public func completeExceptionally(
        exception: ExportedKotlinPackages.kotlin.Throwable
    ) -> Swift.Bool {
        return kotlinx_coroutines_CompletableDeferred_completeExceptionally__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable__(self.__externalRCRef(), exception.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines._CompletableDeferred {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred, ExportedKotlinPackages.kotlinx.coroutines.__CompletableDeferred where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._CompletableDeferred {
}
extension ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.CompletableJob where Self : ExportedKotlinPackages.kotlinx.coroutines.__CompletableJob {
    public func complete() -> Swift.Bool {
        return kotlinx_coroutines_CompletableJob_complete(self.__externalRCRef())
    }
    public func completeExceptionally(
        exception: ExportedKotlinPackages.kotlin.Throwable
    ) -> Swift.Bool {
        return kotlinx_coroutines_CompletableJob_completeExceptionally__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable__(self.__externalRCRef(), exception.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines._CompletableJob {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.CompletableJob, ExportedKotlinPackages.kotlinx.coroutines.__CompletableJob where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._CompletableJob {
}
extension ExportedKotlinPackages.kotlinx.coroutines.CompletableJob {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.CoroutineExceptionHandler where Self : ExportedKotlinPackages.kotlinx.coroutines.__CoroutineExceptionHandler {
    public func handleException(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        exception: ExportedKotlinPackages.kotlin.Throwable
    ) -> Swift.Void {
        return { kotlinx_coroutines_CoroutineExceptionHandler_handleException__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ExportedKotlinPackages_kotlin_Throwable__(self.__externalRCRef(), context.__externalRCRef(), exception.__externalRCRef()); return () }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines._CoroutineExceptionHandler {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.CoroutineExceptionHandler, ExportedKotlinPackages.kotlinx.coroutines.__CoroutineExceptionHandler where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._CoroutineExceptionHandler {
}
extension ExportedKotlinPackages.kotlinx.coroutines.CoroutineExceptionHandler {
    public typealias Key = KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_CoroutineExceptionHandler_Key
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope where Self : ExportedKotlinPackages.kotlinx.coroutines.__CoroutineScope {
    public var coroutineContext: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_CoroutineScope_coroutineContext_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines._CoroutineScope {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope, ExportedKotlinPackages.kotlinx.coroutines.__CoroutineScope where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._CoroutineScope {
}
extension ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.Deferred where Self : ExportedKotlinPackages.kotlinx.coroutines.__Deferred {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var onAwait: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1 {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_Deferred_onAwait_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1
        }
    }
    public func `await`() async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_Deferred_await(self.__externalRCRef(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func getCompleted() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_Deferred_getCompleted(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func getCompletionExceptionOrNull() -> ExportedKotlinPackages.kotlin.Throwable? {
        return { switch kotlinx_coroutines_Deferred_getCompletionExceptionOrNull(self.__externalRCRef()) { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines._Deferred {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.Deferred, ExportedKotlinPackages.kotlinx.coroutines.__Deferred where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._Deferred {
}
extension ExportedKotlinPackages.kotlinx.coroutines.Deferred {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var onAwait: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1 {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            fatalError("'onAwait' is an @_spi requirement that must be implemented by Swift conformers")
        }
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func getCompleted() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        fatalError("'getCompleted' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func getCompletionExceptionOrNull() -> ExportedKotlinPackages.kotlin.Throwable? {
        fatalError("'getCompletionExceptionOrNull' is an @_spi requirement that must be implemented by Swift conformers")
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.Delay where Self : ExportedKotlinPackages.kotlinx.coroutines.__Delay {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invokeOnTimeout(
        timeMillis: Swift.Int64,
        block: any ExportedKotlinPackages.kotlinx.coroutines.Runnable,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_Delay_invokeOnTimeout__TypesOfArguments__Swift_Int64_anyU20ExportedKotlinPackages_kotlinx_coroutines_Runnable_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(self.__externalRCRef(), timeMillis, block.__externalRCRef(), context.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines._Delay {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi) @_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.Delay, ExportedKotlinPackages.kotlinx.coroutines.__Delay where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._Delay {
}
extension ExportedKotlinPackages.kotlinx.coroutines.Delay {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invokeOnTimeout(
        timeMillis: Swift.Int64,
        block: any ExportedKotlinPackages.kotlinx.coroutines.Runnable,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_Delay_invokeOnTimeout__TypesOfArguments__Swift_Int64_anyU20ExportedKotlinPackages_kotlinx_coroutines_Runnable_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext___direct(self.__externalRCRef(), timeMillis, block.__externalRCRef(), context.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle where Self : ExportedKotlinPackages.kotlinx.coroutines.__DisposableHandle {
    public func dispose() -> Swift.Void {
        return { kotlinx_coroutines_DisposableHandle_dispose(self.__externalRCRef()); return () }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines._DisposableHandle {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle, ExportedKotlinPackages.kotlinx.coroutines.__DisposableHandle where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._DisposableHandle {
}
extension ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.Job where Self : ExportedKotlinPackages.kotlinx.coroutines.__Job {
    public var children: any ExportedKotlinPackages.kotlin.sequences.Sequence {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_Job_children_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.sequences.Sequence.Type.self) as! any ExportedKotlinPackages.kotlin.sequences.Sequence
        }
    }
    public var isActive: Swift.Bool {
        get {
            return kotlinx_coroutines_Job_isActive_get(self.__externalRCRef())
        }
    }
    public var isCancelled: Swift.Bool {
        get {
            return kotlinx_coroutines_Job_isCancelled_get(self.__externalRCRef())
        }
    }
    public var isCompleted: Swift.Bool {
        get {
            return kotlinx_coroutines_Job_isCompleted_get(self.__externalRCRef())
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var onJoin: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0 {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_Job_onJoin_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0
        }
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public var parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)? {
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        get {
            return { switch kotlinx_coroutines_Job_parent_get(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job; } }()
        }
    }
    public func cancel(
        cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
    ) -> Swift.Void {
        return { kotlinx_coroutines_Job_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException___(self.__externalRCRef(), cause.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func getCancellationException() -> ExportedKotlinPackages.kotlinx.coroutines.CancellationException {
        return ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException.__createClassWrapper(externalRCRef: kotlinx_coroutines_Job_getCancellationException(self.__externalRCRef()))
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invokeOnCompletion(
        onCancelling: Swift.Bool,
        invokeImmediately: Swift.Bool,
        handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_Job_invokeOnCompletion__TypesOfArguments__Swift_Bool_Swift_Bool_U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), onCancelling, invokeImmediately, {
            let originalBlock: (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void = handler
            return { (arg0: Swift.UnsafeMutableRawPointer?) in
                let _arg0: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable> = { switch arg0 { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
                let _result = originalBlock(_arg0)
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
    }
    public func invokeOnCompletion(
        handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_Job_invokeOnCompletion__TypesOfArguments__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), {
            let originalBlock: (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void = handler
            return { (arg0: Swift.UnsafeMutableRawPointer?) in
                let _arg0: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable> = { switch arg0 { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
                let _result = originalBlock(_arg0)
                return { _result; return true }()
            }
        }()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
    }
    public func join() async throws -> Swift.Void {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_Job_join(self.__externalRCRef(), {
                let originalBlock: (Swift.Void) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Void = { arg0; return () }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public func start() -> Swift.Bool {
        return kotlinx_coroutines_Job_start(self.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines._Job {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.Job, ExportedKotlinPackages.kotlinx.coroutines.__Job where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._Job {
}
extension ExportedKotlinPackages.kotlinx.coroutines.Job {
    public typealias Key = KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_Job_Key
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var onJoin: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0 {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            fatalError("'onJoin' is an @_spi requirement that must be implemented by Swift conformers")
        }
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public var parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)? {
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        get {
            fatalError("'parent' is an @_spi requirement that must be implemented by Swift conformers")
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func getCancellationException() -> ExportedKotlinPackages.kotlinx.coroutines.CancellationException {
        fatalError("'getCancellationException' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invokeOnCompletion(
        onCancelling: Swift.Bool,
        invokeImmediately: Swift.Bool,
        handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
        fatalError("'invokeOnCompletion' is an @_spi requirement that must be implemented by Swift conformers")
    }
}
@available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ParentJob") @_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.ParentJob where Self : ExportedKotlinPackages.kotlinx.coroutines.__ParentJob {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func getChildJobCancellationCause() -> ExportedKotlinPackages.kotlinx.coroutines.CancellationException {
        return ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException.__createClassWrapper(externalRCRef: kotlinx_coroutines_ParentJob_getChildJobCancellationCause(self.__externalRCRef()))
    }
}
@available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ParentJob") @_documentation(visibility: internal)
package extension KotlinRuntimeSupport._KotlinExistentialPenBox {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi) @available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ParentJob") @_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.ParentJob, ExportedKotlinPackages.kotlinx.coroutines.__ParentJob where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._ParentJob {
}
@available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ParentJob")
extension ExportedKotlinPackages.kotlinx.coroutines.ParentJob {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.Runnable where Self : ExportedKotlinPackages.kotlinx.coroutines.__Runnable {
    public func run() -> Swift.Void {
        return { kotlinx_coroutines_Runnable_run(self.__externalRCRef()); return () }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines._Runnable {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.Runnable, ExportedKotlinPackages.kotlinx.coroutines.__Runnable where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._Runnable {
}
extension ExportedKotlinPackages.kotlinx.coroutines.Runnable {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel where Self : ExportedKotlinPackages.kotlinx.coroutines.channels.__BroadcastChannel {
    @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public func cancel(
        cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
    ) -> Swift.Void {
        return { kotlinx_coroutines_channels_BroadcastChannel_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException___(self.__externalRCRef(), cause.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public func openSubscription() -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_BroadcastChannel_openSubscription(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.channels._BroadcastChannel {
}
@_spi(kotlinx$coroutines$ObsoleteCoroutinesApi) @_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel, ExportedKotlinPackages.kotlinx.coroutines.channels.__BroadcastChannel where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.channels._BroadcastChannel {
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.channels.Channel where Self : ExportedKotlinPackages.kotlinx.coroutines.channels.__Channel {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.channels._Channel {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.channels.Channel, ExportedKotlinPackages.kotlinx.coroutines.channels.__Channel where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.channels._Channel {
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.Channel {
    public typealias Factory = KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_channels_Channel_Factory
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator where Self : ExportedKotlinPackages.kotlinx.coroutines.channels.__ChannelIterator {
    public func hasNext() async throws -> Swift.Bool {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_channels_ChannelIterator_hasNext(self.__externalRCRef(), {
                let originalBlock: (Swift.Bool) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Bool = arg0
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_channels_ChannelIterator_next(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.channels._ChannelIterator {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator, ExportedKotlinPackages.kotlinx.coroutines.channels.__ChannelIterator where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.channels._ChannelIterator {
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope where Self : ExportedKotlinPackages.kotlinx.coroutines.channels.__ProducerScope {
    public var channel: any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_ProducerScope_channel_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.channels._ProducerScope {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope, ExportedKotlinPackages.kotlinx.coroutines.channels.__ProducerScope where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.channels._ProducerScope {
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel where Self : ExportedKotlinPackages.kotlinx.coroutines.channels.__ReceiveChannel {
    @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
    public var isClosedForReceive: Swift.Bool {
        @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
        get {
            return kotlinx_coroutines_channels_ReceiveChannel_isClosedForReceive_get(self.__externalRCRef())
        }
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public var isEmpty: Swift.Bool {
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        get {
            return kotlinx_coroutines_channels_ReceiveChannel_isEmpty_get(self.__externalRCRef())
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var onReceive: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1 {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_ReceiveChannel_onReceive_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var onReceiveCatching: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1 {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_ReceiveChannel_onReceiveCatching_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1
        }
    }
    public func cancel(
        cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
    ) -> Swift.Void {
        return { kotlinx_coroutines_channels_ReceiveChannel_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException___(self.__externalRCRef(), cause.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    public func iterator() -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_ReceiveChannel_iterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator
    }
    public func receive() async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_channels_ReceiveChannel_receive(self.__externalRCRef(), {
                let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public func receiveCatching() async throws -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_channels_ReceiveChannel_receiveCatching(self.__externalRCRef(), {
                let originalBlock: (ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult) -> Swift.Void = continuation
                return { (arg0: Swift.UnsafeMutableRawPointer) in
                    let _arg0: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult = ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult.__createClassWrapper(externalRCRef: arg0)
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public func tryReceive() -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult {
        return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult.__createClassWrapper(externalRCRef: kotlinx_coroutines_channels_ReceiveChannel_tryReceive(self.__externalRCRef()))
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.channels._ReceiveChannel {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel, ExportedKotlinPackages.kotlinx.coroutines.channels.__ReceiveChannel where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.channels._ReceiveChannel {
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel {
    @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
    public var isClosedForReceive: Swift.Bool {
        @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
        get {
            fatalError("'isClosedForReceive' is an @_spi requirement that must be implemented by Swift conformers")
        }
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public var isEmpty: Swift.Bool {
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        get {
            fatalError("'isEmpty' is an @_spi requirement that must be implemented by Swift conformers")
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var onReceive: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1 {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            fatalError("'onReceive' is an @_spi requirement that must be implemented by Swift conformers")
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var onReceiveCatching: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1 {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            fatalError("'onReceiveCatching' is an @_spi requirement that must be implemented by Swift conformers")
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel where Self : ExportedKotlinPackages.kotlinx.coroutines.channels.__SendChannel {
    @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
    public var isClosedForSend: Swift.Bool {
        @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
        get {
            return kotlinx_coroutines_channels_SendChannel_isClosedForSend_get(self.__externalRCRef())
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var onSend: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2 {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_SendChannel_onSend_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2
        }
    }
    public func close(
        cause: ExportedKotlinPackages.kotlin.Throwable?
    ) -> Swift.Bool {
        return kotlinx_coroutines_channels_SendChannel_close__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(self.__externalRCRef(), cause.map { it in it.__externalRCRef() } ?? nil)
    }
    public func invokeOnClose(
        handler: @escaping (ExportedKotlinPackages.kotlin.Throwable?) -> Swift.Void
    ) -> Swift.Void {
        return { kotlinx_coroutines_channels_SendChannel_invokeOnClose__TypesOfArguments__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), {
            let originalBlock: (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void = handler
            return { (arg0: Swift.UnsafeMutableRawPointer?) in
                let _arg0: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable> = { switch arg0 { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
                let _result = originalBlock(_arg0)
                return { _result; return true }()
            }
        }()); return () }()
    }
    public func send(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> Swift.Void {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_channels_SendChannel_send__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil, {
                let originalBlock: (Swift.Void) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Void = { arg0; return () }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public func trySend(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult {
        return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult.__createClassWrapper(externalRCRef: kotlinx_coroutines_channels_SendChannel_trySend__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil))
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.channels._SendChannel {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel, ExportedKotlinPackages.kotlinx.coroutines.channels.__SendChannel where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.channels._SendChannel {
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel {
    @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
    public var isClosedForSend: Swift.Bool {
        @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
        get {
            fatalError("'isClosedForSend' is an @_spi requirement that must be implemented by Swift conformers")
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var onSend: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2 {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            fatalError("'onSend' is an @_spi requirement that must be implemented by Swift conformers")
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.flow.Flow where Self : ExportedKotlinPackages.kotlinx.coroutines.flow.__Flow {
    public func collect(
        collector: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
    ) async throws -> Swift.Void {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_Flow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector__(self.__externalRCRef(), collector.__externalRCRef(), {
                let originalBlock: (Swift.Void) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Void = { arg0; return () }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.flow._Flow {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, ExportedKotlinPackages.kotlinx.coroutines.flow.__Flow, KotlinCoroutineSupport.KotlinFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._Flow {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.Flow {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector where Self : ExportedKotlinPackages.kotlinx.coroutines.flow.__FlowCollector {
    public func emit(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> Swift.Void {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_FlowCollector_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil, {
                let originalBlock: (Swift.Void) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Void = { arg0; return () }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.flow._FlowCollector {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, ExportedKotlinPackages.kotlinx.coroutines.flow.__FlowCollector where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._FlowCollector {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow where Self : ExportedKotlinPackages.kotlinx.coroutines.flow.__MutableSharedFlow {
    public var subscriptionCount: any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Int32> {
        get {
            return KotlinCoroutineSupport._KotlinTypedStateFlowImpl<Swift.Int32>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_MutableSharedFlow_subscriptionCount_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow, Swift.Int32.Type.self)
        }
    }
    public func emit(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> Swift.Void {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_MutableSharedFlow_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil, {
                let originalBlock: (Swift.Void) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Void = { arg0; return () }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func resetReplayCache() -> Swift.Void {
        return { kotlinx_coroutines_flow_MutableSharedFlow_resetReplayCache(self.__externalRCRef()); return () }()
    }
    public func tryEmit(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlinx_coroutines_flow_MutableSharedFlow_tryEmit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil)
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.flow._MutableSharedFlow {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow, ExportedKotlinPackages.kotlinx.coroutines.flow.__MutableSharedFlow, KotlinCoroutineSupport.KotlinMutableSharedFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._MutableSharedFlow {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow {
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func resetReplayCache() -> Swift.Void {
        fatalError("'resetReplayCache' is an @_spi requirement that must be implemented by Swift conformers")
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow where Self : ExportedKotlinPackages.kotlinx.coroutines.flow.__MutableStateFlow {
    public var value: (any KotlinRuntimeSupport._KotlinBridgeable)? {
        get {
            return { switch kotlinx_coroutines_flow_MutableStateFlow_value_get(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
        set {
            return { kotlinx_coroutines_flow_MutableStateFlow_value_set__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), newValue.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
    }
    public func compareAndSet(
        expect: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        update: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlinx_coroutines_flow_MutableStateFlow_compareAndSet__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), expect.map { it in it.__externalRCRef() } ?? nil, update.map { it in it.__externalRCRef() } ?? nil)
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.flow._MutableStateFlow {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow, ExportedKotlinPackages.kotlinx.coroutines.flow.__MutableStateFlow, KotlinCoroutineSupport.KotlinMutableStateFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._MutableStateFlow {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow where Self : ExportedKotlinPackages.kotlinx.coroutines.flow.__SharedFlow {
    public var replayCache: [(any KotlinRuntimeSupport._KotlinBridgeable)?] {
        get {
            return kotlinx_coroutines_flow_SharedFlow_replayCache_get(self.__externalRCRef()) as! Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
        }
    }
    public func collect(
        collector: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
    ) async throws -> Swift.Never {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_SharedFlow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector__(self.__externalRCRef(), collector.__externalRCRef(), {
                let originalBlock: (Swift.Never) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Never = { arg0; fatalError() }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.flow._SharedFlow {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow, ExportedKotlinPackages.kotlinx.coroutines.flow.__SharedFlow, KotlinCoroutineSupport.KotlinSharedFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._SharedFlow {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted where Self : ExportedKotlinPackages.kotlinx.coroutines.flow.__SharingStarted {
    public func command(
        subscriptionCount: any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Int32>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<ExportedKotlinPackages.kotlinx.coroutines.flow.SharingCommand> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<ExportedKotlinPackages.kotlinx.coroutines.flow.SharingCommand>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_SharingStarted_command__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedStateFlow_Swift_Int32___(self.__externalRCRef(), subscriptionCount.wrapped.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, ExportedKotlinPackages.kotlinx.coroutines.flow.SharingCommand.Type.self)
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.flow._SharingStarted {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted, ExportedKotlinPackages.kotlinx.coroutines.flow.__SharingStarted where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._SharingStarted {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted {
    public typealias Companion = KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_flow_SharingStarted_Companion
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow where Self : ExportedKotlinPackages.kotlinx.coroutines.flow.__StateFlow {
    public var value: (any KotlinRuntimeSupport._KotlinBridgeable)? {
        get {
            return { switch kotlinx_coroutines_flow_StateFlow_value_get(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.flow._StateFlow {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow, ExportedKotlinPackages.kotlinx.coroutines.flow.__StateFlow, KotlinCoroutineSupport.KotlinStateFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._StateFlow {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.FusibleFlow where Self : ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.__FusibleFlow {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func fuse(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        capacity: Swift.Int32,
        onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_internal_FusibleFlow_fuse__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow__(self.__externalRCRef(), context.__externalRCRef(), capacity, onBufferOverflow.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinRuntimeSupport._KotlinBridgeable.Type.self)
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`._FusibleFlow {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi) @_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.FusibleFlow, ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.__FusibleFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`._FusibleFlow {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.FusibleFlow {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder where Self : ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectBuilder {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invoke(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0,
        block: @escaping () async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return { kotlinx_coroutines_selects_SelectBuilder_invoke__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause0_U282920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), receiver.__externalRCRef(), {
            let originalBlock: () async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
            return { (continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock()
                }
                return { _result; return true }()
            }
        }()); return () }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invoke(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1,
        block: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return { kotlinx_coroutines_selects_SelectBuilder_invoke__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause1_U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), receiver.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
            return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()); return () }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invoke(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2,
        param: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        block: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return { kotlinx_coroutines_selects_SelectBuilder_invoke__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause2_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), receiver.__externalRCRef(), param.map { it in it.__externalRCRef() } ?? nil, {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
            return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()); return () }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invoke(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2,
        block: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return { kotlinx_coroutines_selects_SelectBuilder_invoke__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause2_U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), receiver.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
            return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()); return () }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.selects._SelectBuilder {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder, ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectBuilder where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.selects._SelectBuilder {
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invoke(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0,
        block: @escaping () async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        fatalError("'invoke' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invoke(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1,
        block: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        fatalError("'invoke' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invoke(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2,
        param: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        block: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        fatalError("'invoke' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invoke(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2,
        block: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return { kotlinx_coroutines_selects_SelectBuilder_invoke__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause2_U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____direct(self.__externalRCRef(), receiver.__externalRCRef(), {
            let originalBlock: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) async throws -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = block
            return { (arg0: Swift.UnsafeMutableRawPointer?, continuation: Swift.UnsafeMutableRawPointer, exception: Swift.UnsafeMutableRawPointer, cancellation: Swift.UnsafeMutableRawPointer) in
                let _arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let _continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                }()
                let _exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
                    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }()); return () }()
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause where Self : ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectClause {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var clauseObject: any KotlinRuntimeSupport._KotlinBridgeable {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: kotlinx_coroutines_selects_SelectClause_clauseObject_get(self.__externalRCRef()))
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause_SealedType {
        switch self {
        case let value as ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0: .selectClause0(value.sealedType())
        case let value as ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1: .selectClause1(.init(value))
        case let value as ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2: .selectClause2(.init(value))
        default: fatalError("missing sealedType for \(self)")
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi) @_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause, ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectClause where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause {
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0 where Self : ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectClause0 {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0_SealedType {
        switch self {
        default: .unknown(.init(self))
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause0 {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi) @_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0, ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectClause0 where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause0 {
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0 {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi) @_disfavoredOverload
    public func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause_SealedType {
        .selectClause0(sealedType())
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1 where Self : ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectClause1 {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause1 {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi) @_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1, ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectClause1 where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause1 {
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1 {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause_SealedType {
        .selectClause1(.init(self))
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2 where Self : ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectClause2 {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause2 {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi) @_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2, ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectClause2 where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause2 {
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2 {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause_SealedType {
        .selectClause2(.init(self))
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance where Self : ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectInstance {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_selects_SelectInstance_context_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func disposeOnCompletion(
        disposableHandle: any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
    ) -> Swift.Void {
        return { kotlinx_coroutines_selects_SelectInstance_disposeOnCompletion__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_DisposableHandle__(self.__externalRCRef(), disposableHandle.__externalRCRef()); return () }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func selectInRegistrationPhase(
        internalResult: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return { kotlinx_coroutines_selects_SelectInstance_selectInRegistrationPhase__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), internalResult.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func trySelect(
        clauseObject: any KotlinRuntimeSupport._KotlinBridgeable,
        result: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlinx_coroutines_selects_SelectInstance_trySelect__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), clauseObject.__externalRCRef(), result.map { it in it.__externalRCRef() } ?? nil)
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.selects._SelectInstance {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi) @_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance, ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectInstance where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.selects._SelectInstance {
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.`internal`.MainDispatcherFactory where Self : ExportedKotlinPackages.kotlinx.coroutines.`internal`.__MainDispatcherFactory {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var loadPriority: Swift.Int32 {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return kotlinx_coroutines_internal_MainDispatcherFactory_loadPriority_get(self.__externalRCRef())
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func createDispatcher(
        allFactories: [any ExportedKotlinPackages.kotlinx.coroutines.`internal`.MainDispatcherFactory]
    ) -> ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher {
        return ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher.__createClassWrapper(externalRCRef: kotlinx_coroutines_internal_MainDispatcherFactory_createDispatcher__TypesOfArguments__Swift_Array_anyU20ExportedKotlinPackages_kotlinx_coroutines_U60internalU60_MainDispatcherFactory___(self.__externalRCRef(), allFactories))
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func hintOnError() -> Swift.String? {
        return kotlinx_coroutines_internal_MainDispatcherFactory_hintOnError(self.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.`internal`._MainDispatcherFactory {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi) @_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.`internal`.MainDispatcherFactory, ExportedKotlinPackages.kotlinx.coroutines.`internal`.__MainDispatcherFactory where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.`internal`._MainDispatcherFactory {
}
extension ExportedKotlinPackages.kotlinx.coroutines.`internal`.MainDispatcherFactory {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func hintOnError() -> Swift.String? {
        return kotlinx_coroutines_internal_MainDispatcherFactory_hintOnError_direct(self.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.`internal`.ThreadSafeHeapNode where Self : ExportedKotlinPackages.kotlinx.coroutines.`internal`.__ThreadSafeHeapNode {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var index: Swift.Int32 {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return kotlinx_coroutines_internal_ThreadSafeHeapNode_index_get(self.__externalRCRef())
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        set {
            return { kotlinx_coroutines_internal_ThreadSafeHeapNode_index_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue); return () }()
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.`internal`._ThreadSafeHeapNode {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi) @_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.`internal`.ThreadSafeHeapNode, ExportedKotlinPackages.kotlinx.coroutines.`internal`.__ThreadSafeHeapNode where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.`internal`._ThreadSafeHeapNode {
}
extension ExportedKotlinPackages.kotlinx.coroutines.`internal`.ThreadSafeHeapNode {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex where Self : ExportedKotlinPackages.kotlinx.coroutines.sync.__Mutex {
    public var isLocked: Swift.Bool {
        get {
            return kotlinx_coroutines_sync_Mutex_isLocked_get(self.__externalRCRef())
        }
    }
    @available(*, deprecated, message: "Mutex.onLock deprecated without replacement. For additional details please refer to #2794") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var onLock: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2 {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_sync_Mutex_onLock_get(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2
        }
    }
    public func holdsLock(
        owner: any KotlinRuntimeSupport._KotlinBridgeable
    ) -> Swift.Bool {
        return kotlinx_coroutines_sync_Mutex_holdsLock__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(self.__externalRCRef(), owner.__externalRCRef())
    }
    public func lock(
        owner: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> Swift.Void {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_sync_Mutex_lock__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), owner.map { it in it.__externalRCRef() } ?? nil, {
                let originalBlock: (Swift.Void) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Void = { arg0; return () }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public func tryLock(
        owner: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlinx_coroutines_sync_Mutex_tryLock__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), owner.map { it in it.__externalRCRef() } ?? nil)
    }
    public func unlock(
        owner: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return { kotlinx_coroutines_sync_Mutex_unlock__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), owner.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.sync._Mutex {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex, ExportedKotlinPackages.kotlinx.coroutines.sync.__Mutex where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.sync._Mutex {
}
extension ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex {
    @available(*, deprecated, message: "Mutex.onLock deprecated without replacement. For additional details please refer to #2794") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var onLock: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2 {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            fatalError("'onLock' is an @_spi requirement that must be implemented by Swift conformers")
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.sync.Semaphore where Self : ExportedKotlinPackages.kotlinx.coroutines.sync.__Semaphore {
    public var availablePermits: Swift.Int32 {
        get {
            return kotlinx_coroutines_sync_Semaphore_availablePermits_get(self.__externalRCRef())
        }
    }
    public func acquire() async throws -> Swift.Void {
        try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_sync_Semaphore_acquire(self.__externalRCRef(), {
                let originalBlock: (Swift.Void) -> Swift.Void = continuation
                return { (arg0: Swift.Bool) in
                    let _arg0: Swift.Void = { arg0; return () }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), {
                let originalBlock: (Swift.Optional<Swift.Error>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<Swift.Error> = { switch arg0 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)!); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
    public func tryAcquire() -> Swift.Bool {
        return kotlinx_coroutines_sync_Semaphore_tryAcquire(self.__externalRCRef())
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.sync._Semaphore {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.sync.Semaphore, ExportedKotlinPackages.kotlinx.coroutines.sync.__Semaphore where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.sync._Semaphore {
}
extension ExportedKotlinPackages.kotlinx.coroutines.sync.Semaphore {
}
@_cdecl("kotlinx_coroutines_CancellableContinuation_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_____reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ cause: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation
    let _result: Swift.Bool = _self.cancel(cause: { switch cause { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_completeResume__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable____reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_completeResume__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ token: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation
    let _result: Swift.Void = _self.completeResume(token: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: token))
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_initCancellability__reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_initCancellability__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation
    let _result: Swift.Void = _self.initCancellability()
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_isActive_get__reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_isActive_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation
    let _result: Swift.Bool = _self.isActive
    return _result
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_isCancelled_get__reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_isCancelled_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation
    let _result: Swift.Bool = _self.isCancelled
    return _result
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_isCompleted_get__reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_isCompleted_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation
    let _result: Swift.Bool = _self.isCompleted
    return _result
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_resumeUndispatchedWithException__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_CoroutineDispatcher_ExportedKotlinPackages_kotlin_Throwable____reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_resumeUndispatchedWithException__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_CoroutineDispatcher_ExportedKotlinPackages_kotlin_Throwable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ receiver: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation
    let _result: Swift.Void = _self.resumeUndispatchedWithException(ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: receiver), exception: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: exception))
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_resumeUndispatched__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_CoroutineDispatcher_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_resumeUndispatched__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_CoroutineDispatcher_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ receiver: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation
    let _result: Swift.Void = _self.resumeUndispatched(ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: receiver), value: { switch value { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_tryResumeWithException__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable____reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_tryResumeWithException__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.tryResumeWithException(exception: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: exception))
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_tryResume__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_tryResume__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?, _ idempotent: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.tryResume(value: { switch value { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }(), idempotent: { switch idempotent { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlinx_coroutines_CloseableCoroutineDispatcher_close__reverse_swift")
package func kotlinx_coroutines_CloseableCoroutineDispatcher_close__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.CloseableCoroutineDispatcher.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.close()
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_CompletableDeferred_completeExceptionally__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable____reverse_swift")
package func kotlinx_coroutines_CompletableDeferred_completeExceptionally__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred
    let _result: Swift.Bool = _self.completeExceptionally(exception: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: exception))
    return _result
}

@_cdecl("kotlinx_coroutines_CompletableDeferred_complete__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_CompletableDeferred_complete__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred
    let _result: Swift.Bool = _self.complete(value: { switch value { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlinx_coroutines_CompletableJob_completeExceptionally__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable____reverse_swift")
package func kotlinx_coroutines_CompletableJob_completeExceptionally__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CompletableJob.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CompletableJob
    let _result: Swift.Bool = _self.completeExceptionally(exception: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: exception))
    return _result
}

@_cdecl("kotlinx_coroutines_CompletableJob_complete__reverse_swift")
package func kotlinx_coroutines_CompletableJob_complete__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CompletableJob.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CompletableJob
    let _result: Swift.Bool = _self.complete()
    return _result
}

@_cdecl("kotlinx_coroutines_CoroutineDispatcher_dispatchYield__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_anyU20ExportedKotlinPackages_kotlinx_coroutines_Runnable____reverse_swift")
package func kotlinx_coroutines_CoroutineDispatcher_dispatchYield__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_anyU20ExportedKotlinPackages_kotlinx_coroutines_Runnable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ context: Swift.UnsafeMutableRawPointer, _ block: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.dispatchYield(context: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: context, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext, block: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: block, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Runnable.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Runnable)
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_CoroutineDispatcher_dispatch__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_anyU20ExportedKotlinPackages_kotlinx_coroutines_Runnable____reverse_swift")
package func kotlinx_coroutines_CoroutineDispatcher_dispatch__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_anyU20ExportedKotlinPackages_kotlinx_coroutines_Runnable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ context: Swift.UnsafeMutableRawPointer, _ block: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.dispatch(context: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: context, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext, block: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: block, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Runnable.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Runnable)
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_CoroutineDispatcher_isDispatchNeeded__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext____reverse_swift")
package func kotlinx_coroutines_CoroutineDispatcher_isDispatchNeeded__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ context: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Bool = _self.isDispatchNeeded(context: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: context, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext)
    return _result
}

@_cdecl("kotlinx_coroutines_CoroutineDispatcher_limitedParallelism__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlinx_coroutines_CoroutineDispatcher_limitedParallelism__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ parallelism: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: `self`)!
    let _result: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher = _self.limitedParallelism(parallelism: parallelism)
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_CoroutineDispatcher_toString__reverse_swift")
package func kotlinx_coroutines_CoroutineDispatcher_toString__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.String = _self.toString()
    return _result
}

@_cdecl("kotlinx_coroutines_CoroutineExceptionHandler_handleException__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ExportedKotlinPackages_kotlin_Throwable____reverse_swift")
package func kotlinx_coroutines_CoroutineExceptionHandler_handleException__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ExportedKotlinPackages_kotlin_Throwable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ context: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineExceptionHandler.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineExceptionHandler
    let _result: Swift.Void = _self.handleException(context: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: context, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext, exception: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: exception))
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_CoroutineScope_coroutineContext_get__reverse_swift")
package func kotlinx_coroutines_CoroutineScope_coroutineContext_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
    let _result: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext = _self.coroutineContext
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_Deferred_await__reverse_swift")
package func kotlinx_coroutines_Deferred_await__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Deferred.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Deferred
    let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.await()
    }
    return true
}

@_cdecl("kotlinx_coroutines_Deferred_getCompleted__reverse_swift")
package func kotlinx_coroutines_Deferred_getCompleted__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Deferred.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Deferred
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.getCompleted()
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlinx_coroutines_Deferred_getCompletionExceptionOrNull__reverse_swift")
package func kotlinx_coroutines_Deferred_getCompletionExceptionOrNull__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Deferred.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Deferred
    let _result: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable> = _self.getCompletionExceptionOrNull()
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlinx_coroutines_Deferred_onAwait_get__reverse_swift")
package func kotlinx_coroutines_Deferred_onAwait_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Deferred.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Deferred
    let _result: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1 = _self.onAwait
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_Delay_invokeOnTimeout__TypesOfArguments__Swift_Int64_anyU20ExportedKotlinPackages_kotlinx_coroutines_Runnable_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext____reverse_swift")
package func kotlinx_coroutines_Delay_invokeOnTimeout__TypesOfArguments__Swift_Int64_anyU20ExportedKotlinPackages_kotlinx_coroutines_Runnable_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ timeMillis: Swift.Int64, _ block: Swift.UnsafeMutableRawPointer, _ context: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Delay.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Delay
    let _result: any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle = _self.invokeOnTimeout(timeMillis: timeMillis, block: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: block, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Runnable.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Runnable, context: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: context, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext)
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_DisposableHandle_dispose__reverse_swift")
package func kotlinx_coroutines_DisposableHandle_dispose__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
    let _result: Swift.Void = _self.dispose()
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_Job_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException_____reverse_swift")
package func kotlinx_coroutines_Job_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ cause: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job
    let _result: Swift.Void = _self.cancel(cause: { switch cause { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException.__createClassWrapper(externalRCRef: res); } }())
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_Job_children_get__reverse_swift")
package func kotlinx_coroutines_Job_children_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job
    let _result: any ExportedKotlinPackages.kotlin.sequences.Sequence = _self.children
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_Job_getCancellationException__reverse_swift")
package func kotlinx_coroutines_Job_getCancellationException__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job
    let _result: ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException = _self.getCancellationException()
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_Job_isActive_get__reverse_swift")
package func kotlinx_coroutines_Job_isActive_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job
    let _result: Swift.Bool = _self.isActive
    return _result
}

@_cdecl("kotlinx_coroutines_Job_isCancelled_get__reverse_swift")
package func kotlinx_coroutines_Job_isCancelled_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job
    let _result: Swift.Bool = _self.isCancelled
    return _result
}

@_cdecl("kotlinx_coroutines_Job_isCompleted_get__reverse_swift")
package func kotlinx_coroutines_Job_isCompleted_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job
    let _result: Swift.Bool = _self.isCompleted
    return _result
}

@_cdecl("kotlinx_coroutines_Job_join__reverse_swift")
package func kotlinx_coroutines_Job_join__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.join()
    }
    return true
}

@_cdecl("kotlinx_coroutines_Job_onJoin_get__reverse_swift")
package func kotlinx_coroutines_Job_onJoin_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job
    let _result: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0 = _self.onJoin
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_Job_parent_get__reverse_swift")
package func kotlinx_coroutines_Job_parent_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job
    let _result: Swift.Optional<any ExportedKotlinPackages.kotlinx.coroutines.Job> = _self.parent
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlinx_coroutines_Job_start__reverse_swift")
package func kotlinx_coroutines_Job_start__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job
    let _result: Swift.Bool = _self.start()
    return _result
}

@_cdecl("kotlinx_coroutines_MainCoroutineDispatcher_immediate_get__reverse_swift")
package func kotlinx_coroutines_MainCoroutineDispatcher_immediate_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher.__createClassWrapper(externalRCRef: `self`)!
    let _result: ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher = _self.immediate
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_MainCoroutineDispatcher_limitedParallelism__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlinx_coroutines_MainCoroutineDispatcher_limitedParallelism__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ parallelism: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher.__createClassWrapper(externalRCRef: `self`)!
    let _result: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher = _self.limitedParallelism(parallelism: parallelism)
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_MainCoroutineDispatcher_toString__reverse_swift")
package func kotlinx_coroutines_MainCoroutineDispatcher_toString__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.String = _self.toString()
    return _result
}

@_cdecl("kotlinx_coroutines_Runnable_run__reverse_swift")
package func kotlinx_coroutines_Runnable_run__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Runnable.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Runnable
    let _result: Swift.Void = _self.run()
    return { _result; return true }()
}

@available(*, deprecated, message: "BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported")
@_cdecl("kotlinx_coroutines_channels_BroadcastChannel_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException_____reverse_swift")
package func kotlinx_coroutines_channels_BroadcastChannel_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ cause: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel
    let _result: Swift.Void = _self.cancel(cause: { switch cause { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException.__createClassWrapper(externalRCRef: res); } }())
    return { _result; return true }()
}

@available(*, deprecated, message: "BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported")
@_cdecl("kotlinx_coroutines_channels_BroadcastChannel_openSubscription__reverse_swift")
package func kotlinx_coroutines_channels_BroadcastChannel_openSubscription__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel
    let _result: any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel = _self.openSubscription()
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_channels_ChannelIterator_hasNext__reverse_swift")
package func kotlinx_coroutines_channels_ChannelIterator_hasNext__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator
    let __continuation: (Swift.Bool) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.hasNext()
    }
    return true
}

@_cdecl("kotlinx_coroutines_channels_ChannelIterator_next__reverse_swift")
package func kotlinx_coroutines_channels_ChannelIterator_next__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.next()
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlinx_coroutines_channels_ProducerScope_channel_get__reverse_swift")
package func kotlinx_coroutines_channels_ProducerScope_channel_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope
    let _result: any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel = _self.channel
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_channels_ReceiveChannel_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException_____reverse_swift")
package func kotlinx_coroutines_channels_ReceiveChannel_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ cause: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
    let _result: Swift.Void = _self.cancel(cause: { switch cause { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException.__createClassWrapper(externalRCRef: res); } }())
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_channels_ReceiveChannel_isClosedForReceive_get__reverse_swift")
package func kotlinx_coroutines_channels_ReceiveChannel_isClosedForReceive_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
    let _result: Swift.Bool = _self.isClosedForReceive
    return _result
}

@_cdecl("kotlinx_coroutines_channels_ReceiveChannel_isEmpty_get__reverse_swift")
package func kotlinx_coroutines_channels_ReceiveChannel_isEmpty_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
    let _result: Swift.Bool = _self.isEmpty
    return _result
}

@_cdecl("kotlinx_coroutines_channels_ReceiveChannel_iterator__reverse_swift")
package func kotlinx_coroutines_channels_ReceiveChannel_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
    let _result: any ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator = _self.iterator()
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_channels_ReceiveChannel_onReceiveCatching_get__reverse_swift")
package func kotlinx_coroutines_channels_ReceiveChannel_onReceiveCatching_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
    let _result: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1 = _self.onReceiveCatching
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_channels_ReceiveChannel_onReceive_get__reverse_swift")
package func kotlinx_coroutines_channels_ReceiveChannel_onReceive_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
    let _result: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1 = _self.onReceive
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_channels_ReceiveChannel_receiveCatching__reverse_swift")
package func kotlinx_coroutines_channels_ReceiveChannel_receiveCatching__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
    let __continuation: (ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlinx_coroutines_channels_ChannelResult__(pointerToBlock.__externalRCRef()!, _1.__externalRCRef()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.receiveCatching()
    }
    return true
}

@_cdecl("kotlinx_coroutines_channels_ReceiveChannel_receive__reverse_swift")
package func kotlinx_coroutines_channels_ReceiveChannel_receive__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
    let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.receive()
    }
    return true
}

@_cdecl("kotlinx_coroutines_channels_ReceiveChannel_tryReceive__reverse_swift")
package func kotlinx_coroutines_channels_ReceiveChannel_tryReceive__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
    let _result: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult = _self.tryReceive()
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_channels_SendChannel_close__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_____reverse_swift")
package func kotlinx_coroutines_channels_SendChannel_close__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ cause: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel
    let _result: Swift.Bool = _self.close(cause: { switch cause { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlinx_coroutines_channels_SendChannel_isClosedForSend_get__reverse_swift")
package func kotlinx_coroutines_channels_SendChannel_isClosedForSend_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel
    let _result: Swift.Bool = _self.isClosedForSend
    return _result
}

@_cdecl("kotlinx_coroutines_channels_SendChannel_onSend_get__reverse_swift")
package func kotlinx_coroutines_channels_SendChannel_onSend_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel
    let _result: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2 = _self.onSend
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_channels_SendChannel_send__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_channels_SendChannel_send__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.send(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    }
    return true
}

@_cdecl("kotlinx_coroutines_channels_SendChannel_trySend__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_channels_SendChannel_trySend__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel
    let _result: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult = _self.trySend(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_flow_AbstractFlow_collectSafely__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector____reverse_swift")
package func kotlinx_coroutines_flow_AbstractFlow_collectSafely__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ collector: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.flow.AbstractFlow.__createClassWrapper(externalRCRef: `self`)!
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.collectSafely(collector: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: collector, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector)
    }
    return true
}

@_cdecl("kotlinx_coroutines_flow_FlowCollector_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_flow_FlowCollector_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.emit(value: { switch value { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    }
    return true
}

@_cdecl("kotlinx_coroutines_flow_Flow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector____reverse_swift")
package func kotlinx_coroutines_flow_Flow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ collector: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.collect(collector: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: collector, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector)
    }
    return true
}

@_cdecl("kotlinx_coroutines_flow_MutableSharedFlow_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_flow_MutableSharedFlow_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.emit(value: { switch value { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    }
    return true
}

@_cdecl("kotlinx_coroutines_flow_MutableSharedFlow_resetReplayCache__reverse_swift")
package func kotlinx_coroutines_flow_MutableSharedFlow_resetReplayCache__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow
    let _result: Swift.Void = _self.resetReplayCache()
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_flow_MutableSharedFlow_subscriptionCount_get__reverse_swift")
package func kotlinx_coroutines_flow_MutableSharedFlow_subscriptionCount_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow
    let _result: any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Int32> = _self.subscriptionCount
    return _result.wrapped.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_flow_MutableSharedFlow_tryEmit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_flow_MutableSharedFlow_tryEmit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow
    let _result: Swift.Bool = _self.tryEmit(value: { switch value { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlinx_coroutines_flow_MutableStateFlow_compareAndSet__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_flow_MutableStateFlow_compareAndSet__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ expect: Swift.UnsafeMutableRawPointer?, _ update: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow
    let _result: Swift.Bool = _self.compareAndSet(expect: { switch expect { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }(), update: { switch update { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlinx_coroutines_flow_MutableStateFlow_value_get__reverse_swift")
package func kotlinx_coroutines_flow_MutableStateFlow_value_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.value
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlinx_coroutines_flow_MutableStateFlow_value_set__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_flow_MutableStateFlow_value_set__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ newValue: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow
    let _result: Swift.Void = { _self.value = { switch newValue { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }() }()
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_flow_SharedFlow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector____reverse_swift")
package func kotlinx_coroutines_flow_SharedFlow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ collector: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow
    let __continuation: (Swift.Never) -> Swift.Void = {
    let _ = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _ in fatalError() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.collect(collector: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: collector, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector)
    }
    return true
}

@_cdecl("kotlinx_coroutines_flow_SharedFlow_replayCache_get__reverse_swift")
package func kotlinx_coroutines_flow_SharedFlow_replayCache_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Any {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow
    let _result: Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> = _self.replayCache
    return _result.map { it in it as! NSObject? ?? NSNull() }
}

@_cdecl("kotlinx_coroutines_flow_SharingStarted_command__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedStateFlow_Swift_Int32_____reverse_swift")
package func kotlinx_coroutines_flow_SharingStarted_command__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedStateFlow_Swift_Int32_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ subscriptionCount: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted
    let _result: any KotlinCoroutineSupport.KotlinTypedFlow<ExportedKotlinPackages.kotlinx.coroutines.flow.SharingCommand> = _self.command(subscriptionCount: KotlinCoroutineSupport._KotlinTypedStateFlowImpl<Swift.Int32>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: subscriptionCount, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow, Swift.Int32.Type.self))
    return _result.wrapped.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_flow_StateFlow_value_get__reverse_swift")
package func kotlinx_coroutines_flow_StateFlow_value_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.value
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlinx_coroutines_flow_internal_ChannelFlow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector____reverse_swift")
package func kotlinx_coroutines_flow_internal_ChannelFlow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ collector: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.ChannelFlow.__createClassWrapper(externalRCRef: `self`)!
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.collect(collector: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: collector, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector)
    }
    return true
}

@_cdecl("kotlinx_coroutines_flow_internal_ChannelFlow_dropChannelOperators__reverse_swift")
package func kotlinx_coroutines_flow_internal_ChannelFlow_dropChannelOperators__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.ChannelFlow.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Optional<any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>> = _self.dropChannelOperators()
    return _result.map { it in it.wrapped.__externalRCRef() } ?? nil
}

@_cdecl("kotlinx_coroutines_flow_internal_ChannelFlow_fuse__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow____reverse_swift")
package func kotlinx_coroutines_flow_internal_ChannelFlow_fuse__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ context: Swift.UnsafeMutableRawPointer, _ capacity: Swift.Int32, _ onBufferOverflow: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.ChannelFlow.__createClassWrapper(externalRCRef: `self`)!
    let _result: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> = _self.fuse(context: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: context, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext, capacity: capacity, onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow(__externalRCRefUnsafe: onBufferOverflow, options: .asBestFittingWrapper))
    return _result.wrapped.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_flow_internal_ChannelFlow_produceImpl__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope____reverse_swift")
package func kotlinx_coroutines_flow_internal_ChannelFlow_produceImpl__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ scope: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.ChannelFlow.__createClassWrapper(externalRCRef: `self`)!
    let _result: any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel = _self.produceImpl(scope: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: scope, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope)
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_flow_internal_ChannelFlow_toString__reverse_swift")
package func kotlinx_coroutines_flow_internal_ChannelFlow_toString__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.ChannelFlow.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.String = _self.toString()
    return _result
}

@_cdecl("kotlinx_coroutines_flow_internal_FusibleFlow_fuse__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow____reverse_swift")
package func kotlinx_coroutines_flow_internal_FusibleFlow_fuse__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ context: Swift.UnsafeMutableRawPointer, _ capacity: Swift.Int32, _ onBufferOverflow: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.FusibleFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.FusibleFlow
    let _result: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> = _self.fuse(context: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: context, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext, capacity: capacity, onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow(__externalRCRefUnsafe: onBufferOverflow, options: .asBestFittingWrapper))
    return _result.wrapped.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_internal_AtomicOp_atomicOp_get__reverse_swift")
package func kotlinx_coroutines_internal_AtomicOp_atomicOp_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp.__createClassWrapper(externalRCRef: `self`)!
    let _result: ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp = _self.atomicOp
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_internal_AtomicOp_complete__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_internal_AtomicOp_complete__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ affected: Swift.UnsafeMutableRawPointer?, _ failure: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Void = _self.complete(affected: { switch affected { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }(), failure: { switch failure { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_internal_AtomicOp_prepare__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_internal_AtomicOp_prepare__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ affected: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.prepare(affected: { switch affected { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlinx_coroutines_internal_LockFreeLinkedListHead_isRemoved_get__reverse_swift")
package func kotlinx_coroutines_internal_LockFreeLinkedListHead_isRemoved_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.`internal`.LockFreeLinkedListHead.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Bool = _self.isRemoved
    return _result
}

@_cdecl("kotlinx_coroutines_internal_LockFreeLinkedListNode_isRemoved_get__reverse_swift")
package func kotlinx_coroutines_internal_LockFreeLinkedListNode_isRemoved_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.`internal`.LockFreeLinkedListNode.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Bool = _self.isRemoved
    return _result
}

@_cdecl("kotlinx_coroutines_internal_LockFreeLinkedListNode_remove__reverse_swift")
package func kotlinx_coroutines_internal_LockFreeLinkedListNode_remove__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.`internal`.LockFreeLinkedListNode.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Bool = _self.remove()
    return _result
}

@_cdecl("kotlinx_coroutines_internal_LockFreeLinkedListNode_toString__reverse_swift")
package func kotlinx_coroutines_internal_LockFreeLinkedListNode_toString__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.`internal`.LockFreeLinkedListNode.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.String = _self.toString()
    return _result
}

@_cdecl("kotlinx_coroutines_internal_MainDispatcherFactory_createDispatcher__TypesOfArguments__Swift_Array_anyU20ExportedKotlinPackages_kotlinx_coroutines_U60internalU60_MainDispatcherFactory_____reverse_swift")
package func kotlinx_coroutines_internal_MainDispatcherFactory_createDispatcher__TypesOfArguments__Swift_Array_anyU20ExportedKotlinPackages_kotlinx_coroutines_U60internalU60_MainDispatcherFactory_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ allFactories: Any) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.`internal`.MainDispatcherFactory.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.`internal`.MainDispatcherFactory
    let _result: ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher = _self.createDispatcher(allFactories: allFactories as! Swift.Array<any ExportedKotlinPackages.kotlinx.coroutines.`internal`.MainDispatcherFactory>)
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_internal_MainDispatcherFactory_hintOnError__reverse_swift")
package func kotlinx_coroutines_internal_MainDispatcherFactory_hintOnError__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.`internal`.MainDispatcherFactory.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.`internal`.MainDispatcherFactory
    let _result: Swift.Optional<Swift.String> = _self.hintOnError()
    return _result ?? nil
}

@_cdecl("kotlinx_coroutines_internal_MainDispatcherFactory_loadPriority_get__reverse_swift")
package func kotlinx_coroutines_internal_MainDispatcherFactory_loadPriority_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.`internal`.MainDispatcherFactory.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.`internal`.MainDispatcherFactory
    let _result: Swift.Int32 = _self.loadPriority
    return _result
}

@_cdecl("kotlinx_coroutines_internal_OpDescriptor_atomicOp_get__reverse_swift")
package func kotlinx_coroutines_internal_OpDescriptor_atomicOp_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.`internal`.OpDescriptor.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Optional<ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp> = _self.atomicOp
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlinx_coroutines_internal_OpDescriptor_perform__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_internal_OpDescriptor_perform__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ affected: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.`internal`.OpDescriptor.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.perform(affected: { switch affected { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlinx_coroutines_internal_OpDescriptor_toString__reverse_swift")
package func kotlinx_coroutines_internal_OpDescriptor_toString__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.String {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.`internal`.OpDescriptor.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.String = _self.toString()
    return _result
}

@_cdecl("kotlinx_coroutines_internal_ThreadSafeHeapNode_index_get__reverse_swift")
package func kotlinx_coroutines_internal_ThreadSafeHeapNode_index_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.`internal`.ThreadSafeHeapNode.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.`internal`.ThreadSafeHeapNode
    let _result: Swift.Int32 = _self.index
    return _result
}

@_cdecl("kotlinx_coroutines_internal_ThreadSafeHeapNode_index_set__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlinx_coroutines_internal_ThreadSafeHeapNode_index_set__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ newValue: Swift.Int32) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.`internal`.ThreadSafeHeapNode.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.`internal`.ThreadSafeHeapNode
    let _result: Swift.Void = { _self.index = newValue }()
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_selects_SelectClause_clauseObject_get__reverse_swift")
package func kotlinx_coroutines_selects_SelectClause_clauseObject_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause
    let _result: any KotlinRuntimeSupport._KotlinBridgeable = _self.clauseObject
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_selects_SelectInstance_context_get__reverse_swift")
package func kotlinx_coroutines_selects_SelectInstance_context_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance
    let _result: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext = _self.context
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_selects_SelectInstance_disposeOnCompletion__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_DisposableHandle____reverse_swift")
package func kotlinx_coroutines_selects_SelectInstance_disposeOnCompletion__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_DisposableHandle____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ disposableHandle: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance
    let _result: Swift.Void = _self.disposeOnCompletion(disposableHandle: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: disposableHandle, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle)
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_selects_SelectInstance_selectInRegistrationPhase__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_selects_SelectInstance_selectInRegistrationPhase__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ internalResult: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance
    let _result: Swift.Void = _self.selectInRegistrationPhase(internalResult: { switch internalResult { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_selects_SelectInstance_trySelect__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_selects_SelectInstance_trySelect__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ clauseObject: Swift.UnsafeMutableRawPointer, _ result: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance
    let _result: Swift.Bool = _self.trySelect(clauseObject: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: clauseObject), result: { switch result { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlinx_coroutines_sync_Mutex_holdsLock__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable____reverse_swift")
package func kotlinx_coroutines_sync_Mutex_holdsLock__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ owner: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex
    let _result: Swift.Bool = _self.holdsLock(owner: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: owner))
    return _result
}

@_cdecl("kotlinx_coroutines_sync_Mutex_isLocked_get__reverse_swift")
package func kotlinx_coroutines_sync_Mutex_isLocked_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex
    let _result: Swift.Bool = _self.isLocked
    return _result
}

@_cdecl("kotlinx_coroutines_sync_Mutex_lock__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_sync_Mutex_lock__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ owner: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.lock(owner: { switch owner { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    }
    return true
}

@available(*, deprecated, message: "Mutex.onLock deprecated without replacement. For additional details please refer to #2794")
@_cdecl("kotlinx_coroutines_sync_Mutex_onLock_get__reverse_swift")
package func kotlinx_coroutines_sync_Mutex_onLock_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex
    let _result: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2 = _self.onLock
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_sync_Mutex_tryLock__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_sync_Mutex_tryLock__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ owner: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex
    let _result: Swift.Bool = _self.tryLock(owner: { switch owner { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlinx_coroutines_sync_Mutex_unlock__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_sync_Mutex_unlock__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ owner: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex
    let _result: Swift.Void = _self.unlock(owner: { switch owner { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_sync_Semaphore_acquire__reverse_swift")
package func kotlinx_coroutines_sync_Semaphore_acquire__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.sync.Semaphore.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.sync.Semaphore
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.acquire()
    }
    return true
}

@_cdecl("kotlinx_coroutines_sync_Semaphore_availablePermits_get__reverse_swift")
package func kotlinx_coroutines_sync_Semaphore_availablePermits_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.sync.Semaphore.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.sync.Semaphore
    let _result: Swift.Int32 = _self.availablePermits
    return _result
}

@_cdecl("kotlinx_coroutines_sync_Semaphore_tryAcquire__reverse_swift")
package func kotlinx_coroutines_sync_Semaphore_tryAcquire__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.sync.Semaphore.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.sync.Semaphore
    let _result: Swift.Bool = _self.tryAcquire()
    return _result
}

@_exported import ExportedKotlinPackages
import KotlinRuntime
import KotlinxAtomicFu
import KotlinRuntimeSupport
@_exported import KotlinCoroutineSupport
@_implementationOnly import KotlinBridges_KotlinxCoroutinesCore
@_spi(kotlin$ExperimentalStdlibApi) import KotlinStdlib

public typealias channels = ExportedKotlinPackages.kotlinx.coroutines.channels
public typealias flow = ExportedKotlinPackages.kotlinx.coroutines.flow
public typealias `internal` = ExportedKotlinPackages.kotlinx.coroutines.`internal`
public typealias intrinsics = ExportedKotlinPackages.kotlinx.coroutines.intrinsics
public typealias selects = ExportedKotlinPackages.kotlinx.coroutines.selects
public typealias sync = ExportedKotlinPackages.kotlinx.coroutines.sync
@_spi(kotlinx$coroutines$InternalCoroutinesApi) @available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.JobSupport")
public typealias AbstractCoroutine<T: KotlinRuntimeSupport._KotlinBridgeable> = ExportedKotlinPackages.kotlinx.coroutines.AbstractCoroutine<T>
@_spi(kotlinx$coroutines$InternalCoroutinesApi) @available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.JobSupport")
public typealias _AbstractCoroutine_Base = ExportedKotlinPackages.kotlinx.coroutines._AbstractCoroutine_Base
public typealias CancellableContinuation = ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation
public typealias AnyCancellableContinuation<T: KotlinRuntimeSupport._KotlinBridgeable> = ExportedKotlinPackages.kotlinx.coroutines.AnyCancellableContinuation<T>
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
public typealias AnyCompletableDeferred<T: KotlinRuntimeSupport._KotlinBridgeable> = ExportedKotlinPackages.kotlinx.coroutines.AnyCompletableDeferred<T>
public typealias _CompletableDeferred = ExportedKotlinPackages.kotlinx.coroutines._CompletableDeferred
public typealias __CompletableDeferred = ExportedKotlinPackages.kotlinx.coroutines.__CompletableDeferred
public typealias CompletableJob = ExportedKotlinPackages.kotlinx.coroutines.CompletableJob
public typealias _CompletableJob = ExportedKotlinPackages.kotlinx.coroutines._CompletableJob
public typealias __CompletableJob = ExportedKotlinPackages.kotlinx.coroutines.__CompletableJob
@_spi(kotlinx$coroutines$InternalCoroutinesApi)
public typealias CompletionHandlerException = ExportedKotlinPackages.kotlinx.coroutines.CompletionHandlerException
@_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
public typealias CopyableThrowable = ExportedKotlinPackages.kotlinx.coroutines.CopyableThrowable
@_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
public typealias AnyCopyableThrowable<T: ExportedKotlinPackages.kotlin.Throwable> = ExportedKotlinPackages.kotlinx.coroutines.AnyCopyableThrowable<T>
public typealias _CopyableThrowable = ExportedKotlinPackages.kotlinx.coroutines._CopyableThrowable
@_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
public typealias __CopyableThrowable = ExportedKotlinPackages.kotlinx.coroutines.__CopyableThrowable
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
public typealias AnyDeferred<T: KotlinRuntimeSupport._KotlinBridgeable> = ExportedKotlinPackages.kotlinx.coroutines.AnyDeferred<T>
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
@_spi(kotlinx$coroutines$InternalCoroutinesApi)
public final class _ExportedKotlinPackages_kotlinx_coroutines_channels_ChannelResult_Companion: KotlinRuntime.KotlinBase {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public static var shared: KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_channels_ChannelResult_Companion {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_channels_ChannelResult_Companion.__createClassWrapper(externalRCRef: kotlinx_coroutines_channels_ChannelResult_Companion_get())
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func closed<E: KotlinRuntimeSupport._KotlinBridgeable>(
        cause: ExportedKotlinPackages.kotlin.Throwable?
    ) -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<E> {
        return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<E>.__fromKotlinPointer(kotlinx_coroutines_channels_ChannelResult_Companion_closed__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(self.__externalRCRef(), cause.map { it in it.__externalRCRef() } ?? nil))
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func failure<E: KotlinRuntimeSupport._KotlinBridgeable>() -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<E> {
        return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<E>.__fromKotlinPointer(kotlinx_coroutines_channels_ChannelResult_Companion_failure(self.__externalRCRef()))
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func success<E: KotlinRuntimeSupport._KotlinBridgeable>(
        value: E
    ) -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<E> {
        return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<E>.__fromKotlinPointer(kotlinx_coroutines_channels_ChannelResult_Companion_success__TypesOfArguments__E__(self.__externalRCRef(), value.__retainedToKotlinPointer()))
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    private override init() {
        fatalError()
    }
}
public final class _ExportedKotlinPackages_kotlinx_coroutines_CoroutineExceptionHandler_Key: KotlinRuntime.KotlinBase, KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key, KotlinStdlib.___ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
    public typealias E = KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines._CoroutineExceptionHandler>
    public static var shared: KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_CoroutineExceptionHandler_Key {
        get {
            return KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_CoroutineExceptionHandler_Key.__createClassWrapper(externalRCRef: kotlinx_coroutines_CoroutineExceptionHandler_Key_get())
        }
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    private override init() {
        fatalError()
    }
}
public final class _ExportedKotlinPackages_kotlinx_coroutines_Job_Key: KotlinRuntime.KotlinBase, KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key, KotlinStdlib.___ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
    public typealias E = KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines._Job>
    public static var shared: KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_Job_Key {
        get {
            return KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_Job_Key.__createClassWrapper(externalRCRef: kotlinx_coroutines_Job_Key_get())
        }
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    private override init() {
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
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    private override init() {
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
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    private override init() {
        fatalError()
    }
}
public func cancellationException(
    message: Swift.String?,
    cause: ExportedKotlinPackages.kotlin.Throwable?
) -> ExportedKotlinPackages.kotlinx.coroutines.CancellationException {
    return ExportedKotlinPackages.kotlinx.coroutines.cancellationException(message: message, cause: cause)
}
public func completableDeferred<T: KotlinRuntimeSupport._KotlinBridgeable>(
    value: T
) -> any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred<T> {
    return ExportedKotlinPackages.kotlinx.coroutines.completableDeferred(value: value)
}
public func completableDeferred<T: KotlinRuntimeSupport._KotlinBridgeable>(
    parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)?
) -> any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred<T> {
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
public func awaitAll<T: KotlinRuntimeSupport._KotlinBridgeable>(
    deferreds: any ExportedKotlinPackages.kotlinx.coroutines.Deferred<T>...
) async throws -> [T] {
    return try await withKotlinContinuation { continuation, exception, cancellation in
        let _: Bool = kotlinx_coroutines_awaitAll__TypesOfArguments__Swift_Array_anyU20ExportedKotlinPackages_kotlinx_coroutines_Deferred_T___Vararg___(deferreds, Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(Swift.Array<T>.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
    }
}
public func awaitAll<T: KotlinRuntimeSupport._KotlinBridgeable>(
    _ receiver: any ExportedKotlinPackages.kotlin.collections.Collection<ExportedKotlinPackages.kotlinx.coroutines.AnyDeferred<T>>
) async throws -> [T] {
    return try await ExportedKotlinPackages.kotlinx.coroutines.awaitAll(receiver)
}
public func awaitCancellation() async throws -> Swift.Never {
    return try await ExportedKotlinPackages.kotlinx.coroutines.awaitCancellation()
}
public func coroutineScope<R: KotlinRuntimeSupport._KotlinBridgeable>(
    block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> R
) async throws -> R {
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
    return try await withKotlinContinuation { continuation, exception, cancellation in
        let _: Bool = kotlinx_coroutines_joinAll__TypesOfArguments__Swift_Array_anyU20ExportedKotlinPackages_kotlinx_coroutines_Job__Vararg___(jobs, Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
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
public func runBlocking<T: KotlinRuntimeSupport._KotlinBridgeable>(
    context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
    block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> T
) -> T {
    return ExportedKotlinPackages.kotlinx.coroutines.runBlocking(context: context, block: block)
}
public func supervisorScope<R: KotlinRuntimeSupport._KotlinBridgeable>(
    block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> R
) async throws -> R {
    return try await ExportedKotlinPackages.kotlinx.coroutines.supervisorScope(block: block)
}
public func suspendCancellableCoroutine<T: KotlinRuntimeSupport._KotlinBridgeable>(
    block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation<T>) -> Swift.Void
) async throws -> T {
    return try await ExportedKotlinPackages.kotlinx.coroutines.suspendCancellableCoroutine(block: block)
}
public func withContext<T: KotlinRuntimeSupport._KotlinBridgeable>(
    context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
    block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> T
) async throws -> T {
    return try await ExportedKotlinPackages.kotlinx.coroutines.withContext(context: context, block: block)
}
public func withTimeout<T: KotlinRuntimeSupport._KotlinBridgeable>(
    timeMillis: Swift.Int64,
    block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> T
) async throws -> T {
    return try await ExportedKotlinPackages.kotlinx.coroutines.withTimeout(timeMillis: timeMillis, block: block)
}
public func withTimeout<T: KotlinRuntimeSupport._KotlinBridgeable>(
    timeout: ExportedKotlinPackages.kotlin.time.Duration,
    block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> T
) async throws -> T {
    return try await ExportedKotlinPackages.kotlinx.coroutines.withTimeout(timeout: timeout, block: block)
}
public func withTimeoutOrNull<T: KotlinRuntimeSupport._KotlinBridgeable>(
    timeMillis: Swift.Int64,
    block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> T
) async throws -> T? {
    return try await ExportedKotlinPackages.kotlinx.coroutines.withTimeoutOrNull(timeMillis: timeMillis, block: block)
}
public func withTimeoutOrNull<T: KotlinRuntimeSupport._KotlinBridgeable>(
    timeout: ExportedKotlinPackages.kotlin.time.Duration,
    block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> T
) async throws -> T? {
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
        public func callAsFunction<R: KotlinRuntimeSupport._KotlinBridgeable, T: KotlinRuntimeSupport._KotlinBridgeable>(
            block: @escaping (R) async throws -> T,
            receiver: R,
            completion: any ExportedKotlinPackages.kotlin.coroutines.Continuation<T>
        ) -> Swift.Void {
            return { kotlinx_coroutines_CoroutineStart_invoke__TypesOfArguments__U28RU2920asyncU20throwsU202D_U20T_R_anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation_T___(self.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = try await (block)(R.__fromKotlinPointer(_p0)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque(), receiver.__retainedToKotlinPointer(), completion.__externalRCRef()); return () }()
        }
        public init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
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
        public func __externalRCRef() -> Swift.UnsafeMutableRawPointer {
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
    public protocol CancellableContinuation<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.coroutines.Continuation, ExportedKotlinPackages.kotlinx.coroutines._CancellableContinuation {
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
            value: T,
            onCancellation: ((ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void)?
        ) -> Swift.Void
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func tryResume(
            value: T,
            idempotent: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func tryResume(
            value: T,
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
            value: T
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
    public protocol CompletableDeferred<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.Deferred, ExportedKotlinPackages.kotlinx.coroutines._CompletableDeferred {
        func complete(
            value: T
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
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public protocol CopyableThrowable<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines._CopyableThrowable {
        associatedtype T: ExportedKotlinPackages.kotlin.Throwable
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        func createCopy() -> T?
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_CopyableThrowable)
    public protocol _CopyableThrowable {
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public protocol __CopyableThrowable: KotlinRuntimeSupport._KotlinBridgeable {
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
    public protocol Deferred<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.Job, ExportedKotlinPackages.kotlinx.coroutines._Deferred {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        var onAwait: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1<T> {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get
        }
        func `await`() async throws -> T
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        func getCompleted() -> T
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
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func scheduleResumeAfterDelay(
            timeMillis: Swift.Int64,
            continuation: any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation<KotlinRuntimeSupport.KotlinUnit>
        ) -> Swift.Void
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
        var children: any ExportedKotlinPackages.kotlin.sequences.Sequence<KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines._Job>> {
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
    open class AbstractCoroutine<T: KotlinRuntimeSupport._KotlinBridgeable>: ExportedKotlinPackages.kotlinx.coroutines._AbstractCoroutine_Base, KotlinRuntimeSupport._KotlinOpenGenericBridgeable, ExportedKotlinPackages.kotlin.coroutines.Continuation, ExportedKotlinPackages.kotlin.coroutines.__Continuation, ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope, ExportedKotlinPackages.kotlinx.coroutines.__CoroutineScope {
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
            result: ExportedKotlinPackages.kotlin.Result<T>
        ) -> Swift.Void {
            return { kotlinx_coroutines_AbstractCoroutine_resumeWith__TypesOfArguments__ExportedKotlinPackages_kotlin_Result_T___(self.__externalRCRef(), result.__externalRCRef()); return () }()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func start<R: KotlinRuntimeSupport._KotlinBridgeable>(
            start: ExportedKotlinPackages.kotlinx.coroutines.CoroutineStart,
            receiver: R,
            block: @escaping (R) async throws -> T
        ) -> Swift.Void {
            return { kotlinx_coroutines_AbstractCoroutine_start__TypesOfArguments__ExportedKotlinPackages_kotlinx_coroutines_CoroutineStart_R_U28RU2920asyncU20throwsU202D_U20T__(self.__externalRCRef(), start.__externalRCRef(), receiver.__retainedToKotlinPointer(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = try await (block)(R.__fromKotlinPointer(_p0)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()); return () }()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public init(
            parentContext: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
            initParentJob: Swift.Bool,
            active: Swift.Bool
        ) {
            precondition(Self.self != ExportedKotlinPackages.kotlinx.coroutines.AbstractCoroutine<T>.self, "ExportedKotlinPackages.kotlinx.coroutines.AbstractCoroutine is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_AbstractCoroutine_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Bool_Swift_Bool__(__kt, parentContext.__externalRCRef(), initParentJob, active); return () }()
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi) @available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.JobSupport") @_documentation(visibility: internal)
    open class _AbstractCoroutine_Base: ExportedKotlinPackages.kotlinx.coroutines.JobSupport {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnyCancellableContinuation<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation, ExportedKotlinPackages.kotlinx.coroutines.__CancellableContinuation, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var contravariant: KotlinRuntimeSupport.KotlinContravariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._noop)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinContravariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
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
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnyCompletableDeferred<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred, ExportedKotlinPackages.kotlinx.coroutines.__CompletableDeferred, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
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
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public final class AnyCopyableThrowable<T: ExportedKotlinPackages.kotlin.Throwable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.CopyableThrowable, ExportedKotlinPackages.kotlinx.coroutines.__CopyableThrowable, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class CoroutineDispatcher: ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextElement, ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor, ExportedKotlinPackages.kotlin.coroutines.__ContinuationInterceptor {
        @_spi(kotlin$ExperimentalStdlibApi)
        public final class Key: ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextKey<KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlin.coroutines._ContinuationInterceptor>,ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher> {
            @_spi(kotlin$ExperimentalStdlibApi)
            public static var shared: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.Key {
                @_spi(kotlin$ExperimentalStdlibApi)
                get {
                    return ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.Key.__createClassWrapper(externalRCRef: kotlinx_coroutines_CoroutineDispatcher_Key_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
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
        @inlinable
        public final func interceptContinuation<T: KotlinRuntimeSupport._KotlinBridgeable>(
            continuation: any ExportedKotlinPackages.kotlin.coroutines.Continuation<T>
        ) -> any ExportedKotlinPackages.kotlin.coroutines.Continuation<T> {
            return { let _ptr = _cbridge_kotlinx_coroutines_CoroutineDispatcher_interceptContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation_T___(self.__externalRCRef(), continuation.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.coroutines.Continuation<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.coroutines.AnyContinuation<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
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
        public init() {
            precondition(Self.self != ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.self, "ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_CoroutineDispatcher_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class CoroutineName: ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextElement {
        public final class Key: KotlinRuntime.KotlinBase, KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key, KotlinStdlib.___ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key {
            public typealias E = ExportedKotlinPackages.kotlinx.coroutines.CoroutineName
            public static var shared: ExportedKotlinPackages.kotlinx.coroutines.CoroutineName.Key {
                get {
                    return ExportedKotlinPackages.kotlinx.coroutines.CoroutineName.Key.__createClassWrapper(externalRCRef: kotlinx_coroutines_CoroutineName_Key_get())
                }
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            private override init() {
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
        public init(
            name: Swift.String
        ) {
            let __kt = kotlinx_coroutines_CoroutineName_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_CoroutineName_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(__kt, name); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnyDeferred<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.Deferred, ExportedKotlinPackages.kotlinx.coroutines.__Deferred, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
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
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        private override init() {
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
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        private override init() {
            fatalError()
        }
    }
    @available(*, unavailable, message: "This is internal API and may be removed in the future releases") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    open class JobSupport: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.Job, ExportedKotlinPackages.kotlinx.coroutines.__Job, ExportedKotlinPackages.kotlinx.coroutines.ChildJob, ExportedKotlinPackages.kotlinx.coroutines.__ChildJob, ExportedKotlinPackages.kotlinx.coroutines.ParentJob, ExportedKotlinPackages.kotlinx.coroutines.__ParentJob {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final var children: any ExportedKotlinPackages.kotlin.sequences.Sequence<KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines._Job>> {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return { let _ptr = kotlinx_coroutines_JobSupport_children_get(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.sequences.Sequence<KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines._Job>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.sequences.AnySequence<KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines._Job>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
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
                return { let _ptr = kotlinx_coroutines_JobSupport_key_get(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_AnyKey<KotlinRuntimeSupport._KotlinExistential<any KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
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
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_JobSupport_invokeOnCompletion__TypesOfArguments__Swift_Bool_Swift_Bool_U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), onCancelling, invokeImmediately, Unmanaged.passRetained((handler as (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func invokeOnCompletion(
            handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_JobSupport_invokeOnCompletion__TypesOfArguments__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), Unmanaged.passRetained((handler as (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func join() async throws -> Swift.Void {
            return try await withKotlinContinuation { continuation, exception, cancellation in
                let _: Bool = kotlinx_coroutines_JobSupport_join(self.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
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
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
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
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher.self, "ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_MainCoroutineDispatcher_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class NonCancellable: ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextElement, ExportedKotlinPackages.kotlinx.coroutines.Job, ExportedKotlinPackages.kotlinx.coroutines.__Job {
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")
        public var children: any ExportedKotlinPackages.kotlin.sequences.Sequence<KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines._Job>> {
            get {
                return { let _ptr = kotlinx_coroutines_NonCancellable_children_get(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.sequences.Sequence<KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines._Job>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.sequences.AnySequence<KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines._Job>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
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
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_NonCancellable_invokeOnCompletion__TypesOfArguments__Swift_Bool_Swift_Bool_U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), onCancelling, invokeImmediately, Unmanaged.passRetained((handler as (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
        }
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")
        public func invokeOnCompletion(
            handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_NonCancellable_invokeOnCompletion__TypesOfArguments__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), Unmanaged.passRetained((handler as (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
        }
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")
        public func join() async throws -> Swift.Void {
            return try await withKotlinContinuation { continuation, exception, cancellation in
                let _: Bool = kotlinx_coroutines_NonCancellable_join(self.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
            }
        }
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")
        public func start() -> Swift.Bool {
            return kotlinx_coroutines_NonCancellable_start(self.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
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
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        private override init() {
            fatalError()
        }
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public final class TimeoutCancellationException: ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException, ExportedKotlinPackages.kotlinx.coroutines.CopyableThrowable, ExportedKotlinPackages.kotlinx.coroutines.__CopyableThrowable {
        public typealias T = ExportedKotlinPackages.kotlinx.coroutines.TimeoutCancellationException
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        public func createCopy() -> ExportedKotlinPackages.kotlinx.coroutines.TimeoutCancellationException {
            return ExportedKotlinPackages.kotlinx.coroutines.TimeoutCancellationException.__createClassWrapper(externalRCRef: kotlinx_coroutines_TimeoutCancellationException_createCopy(self.__externalRCRef()))
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
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
    @inlinable
    public static func completableDeferred<T: KotlinRuntimeSupport._KotlinBridgeable>(
        value: T
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred<T> {
        return { let _ptr = _cbridge_kotlinx_coroutines_CompletableDeferred__TypesOfArguments__T__(value.__retainedToKotlinPointer()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.AnyCompletableDeferred<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public static func completableDeferred<T: KotlinRuntimeSupport._KotlinBridgeable>(
        parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)?
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred<T> {
        return { let _ptr = kotlinx_coroutines_CompletableDeferred__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_kotlinx_coroutines_Job___(parent.map { it in it.__externalRCRef() } ?? nil); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.AnyCompletableDeferred<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public static func coroutineExceptionHandler(
        handler: @escaping (any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext, ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.CoroutineExceptionHandler {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_CoroutineExceptionHandler__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_U20ExportedKotlinPackages_kotlin_ThrowableU29202D_U20Swift_Void__(Unmanaged.passRetained((handler as (any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext, ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineExceptionHandler.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineExceptionHandler
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
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_Runnable__TypesOfArguments__U2829202D_U20Swift_Void__(Unmanaged.passRetained((block as () -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Runnable.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Runnable
    }
    public static func supervisorJob(
        parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)?
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.CompletableJob {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_SupervisorJob__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_kotlinx_coroutines_Job___(parent.map { it in it.__externalRCRef() } ?? nil), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CompletableJob.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CompletableJob
    }
    public static func awaitAll<T: KotlinRuntimeSupport._KotlinBridgeable>(
        deferreds: any ExportedKotlinPackages.kotlinx.coroutines.Deferred<T>...
    ) async throws -> [T] {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_awaitAll__TypesOfArguments__Swift_Array_anyU20ExportedKotlinPackages_kotlinx_coroutines_Deferred_T___Vararg___(deferreds, Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(Swift.Array<T>.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func awaitAll<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlin.collections.Collection<ExportedKotlinPackages.kotlinx.coroutines.AnyDeferred<T>>
    ) async throws -> [T] {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_awaitAll__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_collections_Collection_ExportedKotlinPackages_kotlinx_coroutines_AnyDeferred_T____(receiver.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(Swift.Array<T>.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func awaitCancellation() async throws -> Swift.Never {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_awaitCancellation(Unmanaged.passRetained((continuation as (Swift.Never) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func coroutineScope<R: KotlinRuntimeSupport._KotlinBridgeable>(
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> R
    ) async throws -> R {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_coroutineScope__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20R__(Unmanaged.passRetained(({ (_p0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = try await (block)(_p0); return _res.__retainedToKotlinPointer() } as (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(R.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func currentCoroutineContext() async throws -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_currentCoroutineContext(Unmanaged.passRetained((continuation as (any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func delay(
        timeMillis: Swift.Int64
    ) async throws -> Swift.Void {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_delay__TypesOfArguments__Swift_Int64__(timeMillis, Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func delay(
        duration: ExportedKotlinPackages.kotlin.time.Duration
    ) async throws -> Swift.Void {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_delay__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__(duration.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
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
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_joinAll__TypesOfArguments__Swift_Array_anyU20ExportedKotlinPackages_kotlinx_coroutines_Job__Vararg___(jobs, Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func joinAll(
        _ receiver: any ExportedKotlinPackages.kotlin.collections.Collection<KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines._Job>>
    ) async throws -> Swift.Void {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_joinAll__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_collections_Collection_KotlinRuntimeSupport__KotlinExistential_anyU20ExportedKotlinPackages_kotlinx_coroutines__Job____(receiver.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
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
    public static func runBlocking<T: KotlinRuntimeSupport._KotlinBridgeable>(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> T
    ) -> T {
        return T.__fromKotlinPointer(kotlinx_coroutines_runBlocking__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20T__(context.__externalRCRef(), Unmanaged.passRetained(({ (_p0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = try await (block)(_p0); return _res.__retainedToKotlinPointer() } as (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()))
    }
    public static func supervisorScope<R: KotlinRuntimeSupport._KotlinBridgeable>(
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> R
    ) async throws -> R {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_supervisorScope__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20R__(Unmanaged.passRetained(({ (_p0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = try await (block)(_p0); return _res.__retainedToKotlinPointer() } as (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(R.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func suspendCancellableCoroutine<T: KotlinRuntimeSupport._KotlinBridgeable>(
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation<T>) -> Swift.Void
    ) async throws -> T {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_suspendCancellableCoroutine__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CancellableContinuation_T_U29202D_U20Swift_Void__(Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (block)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.AnyCancellableContinuation<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func withContext<T: KotlinRuntimeSupport._KotlinBridgeable>(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> T
    ) async throws -> T {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_withContext__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20T__(context.__externalRCRef(), Unmanaged.passRetained(({ (_p0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = try await (block)(_p0); return _res.__retainedToKotlinPointer() } as (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func withTimeout<T: KotlinRuntimeSupport._KotlinBridgeable>(
        timeMillis: Swift.Int64,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> T
    ) async throws -> T {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_withTimeout__TypesOfArguments__Swift_Int64_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20T__(timeMillis, Unmanaged.passRetained(({ (_p0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = try await (block)(_p0); return _res.__retainedToKotlinPointer() } as (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func withTimeout<T: KotlinRuntimeSupport._KotlinBridgeable>(
        timeout: ExportedKotlinPackages.kotlin.time.Duration,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> T
    ) async throws -> T {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_withTimeout__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20T__(timeout.__externalRCRef(), Unmanaged.passRetained(({ (_p0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = try await (block)(_p0); return _res.__retainedToKotlinPointer() } as (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func withTimeoutOrNull<T: KotlinRuntimeSupport._KotlinBridgeable>(
        timeMillis: Swift.Int64,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> T
    ) async throws -> T? {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_withTimeoutOrNull__TypesOfArguments__Swift_Int64_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20T__(timeMillis, Unmanaged.passRetained(({ (_p0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = try await (block)(_p0); return _res.__retainedToKotlinPointer() } as (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(Swift.Optional<T>.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func withTimeoutOrNull<T: KotlinRuntimeSupport._KotlinBridgeable>(
        timeout: ExportedKotlinPackages.kotlin.time.Duration,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> T
    ) async throws -> T? {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_withTimeoutOrNull__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20T__(timeout.__externalRCRef(), Unmanaged.passRetained(({ (_p0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = try await (block)(_p0); return _res.__retainedToKotlinPointer() } as (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(Swift.Optional<T>.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func yield() async throws -> Swift.Void {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_yield(Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func async<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        start: ExportedKotlinPackages.kotlinx.coroutines.CoroutineStart,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> T
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.Deferred<T> {
        return { let _ptr = kotlinx_coroutines_async__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ExportedKotlinPackages_kotlinx_coroutines_CoroutineStart_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20T__(receiver.__externalRCRef(), context.__externalRCRef(), start.__externalRCRef(), Unmanaged.passRetained(({ (_p0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = try await (block)(_p0); return _res.__retainedToKotlinPointer() } as (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.Deferred<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.AnyDeferred<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
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
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_cancelAndJoin__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_Job__(receiver.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
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
    @inlinable
    public static func completeWith<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred<T>,
        result: ExportedKotlinPackages.kotlin.Result<T>
    ) -> Swift.Bool {
        return _cbridge_kotlinx_coroutines_completeWith__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CompletableDeferred_T__ExportedKotlinPackages_kotlin_Result_T___(receiver.__externalRCRef(), result.__externalRCRef())
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
    public static func invoke<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> T
    ) async throws -> T {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_invoke__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_CoroutineDispatcher_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20T__(receiver.__externalRCRef(), Unmanaged.passRetained(({ (_p0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = try await (block)(_p0); return _res.__retainedToKotlinPointer() } as (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func launch(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        start: ExportedKotlinPackages.kotlinx.coroutines.CoroutineStart,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.Job {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_launch__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ExportedKotlinPackages_kotlinx_coroutines_CoroutineStart_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Void__(receiver.__externalRCRef(), context.__externalRCRef(), start.__externalRCRef(), Unmanaged.passRetained((block as (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job
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
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_DisposableHandle__TypesOfArguments__U2829202D_U20Swift_Void__(Unmanaged.passRetained((function as () -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
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
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            switch kotlinx_coroutines_channels_BufferOverflow_ordinal(__externalRCRefUnsafe) {
            case 0: self = .SUSPEND
            case 1: self = .DROP_OLDEST
            case 2: self = .DROP_LATEST
            default: fatalError()
            }
        }
        public func __externalRCRef() -> Swift.UnsafeMutableRawPointer {
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
    public protocol BroadcastChannel<E>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel, ExportedKotlinPackages.kotlinx.coroutines.channels._BroadcastChannel {
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        func cancel(
            cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
        ) -> Swift.Void
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        func openSubscription() -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<E>
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_channels_BroadcastChannel)
    public protocol _BroadcastChannel: ExportedKotlinPackages.kotlinx.coroutines.channels._SendChannel {
    }
    @available(*, deprecated, message: "BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public protocol __BroadcastChannel: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.channels.__SendChannel {
    }
    public protocol Channel<E>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel, ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel, ExportedKotlinPackages.kotlinx.coroutines.channels._Channel {
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_channels_Channel)
    public protocol _Channel: ExportedKotlinPackages.kotlinx.coroutines.channels._SendChannel, ExportedKotlinPackages.kotlinx.coroutines.channels._ReceiveChannel {
    }
    public protocol __Channel: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.channels.__SendChannel, ExportedKotlinPackages.kotlinx.coroutines.channels.__ReceiveChannel {
    }
    public protocol ChannelIterator<E>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.channels._ChannelIterator {
        associatedtype E: KotlinRuntimeSupport._KotlinBridgeable
        func hasNext() async throws -> Swift.Bool
        func next() -> E
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_channels_ChannelIterator)
    public protocol _ChannelIterator {
    }
    public protocol __ChannelIterator: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol ProducerScope<E>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope, ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel, ExportedKotlinPackages.kotlinx.coroutines.channels._ProducerScope {
        var channel: any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel<E> {
            get
        }
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_channels_ProducerScope)
    public protocol _ProducerScope: ExportedKotlinPackages.kotlinx.coroutines._CoroutineScope, ExportedKotlinPackages.kotlinx.coroutines.channels._SendChannel {
    }
    public protocol __ProducerScope: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.__CoroutineScope, ExportedKotlinPackages.kotlinx.coroutines.channels.__SendChannel {
    }
    public protocol ReceiveChannel<E>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.channels._ReceiveChannel {
        associatedtype E: KotlinRuntimeSupport._KotlinBridgeable
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
        var onReceive: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1<E> {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        var onReceiveCatching: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1<ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<E>> {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get
        }
        func cancel(
            cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
        ) -> Swift.Void
        func iterator() -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator<E>
        func receive() async throws -> E
        func receiveCatching() async throws -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<E>
        func tryReceive() -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<E>
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_channels_ReceiveChannel)
    public protocol _ReceiveChannel {
    }
    public protocol __ReceiveChannel: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol SendChannel<E>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.channels._SendChannel {
        associatedtype E: KotlinRuntimeSupport._KotlinBridgeable
        @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
        var isClosedForSend: Swift.Bool {
            @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
            get
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        var onSend: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2<E,ExportedKotlinPackages.kotlinx.coroutines.channels.AnySendChannel<E>> {
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
            element: E
        ) async throws -> Swift.Void
        func trySend(
            element: E
        ) -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<KotlinRuntimeSupport.KotlinUnit>
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_channels_SendChannel)
    public protocol _SendChannel {
    }
    public protocol __SendChannel: KotlinRuntimeSupport._KotlinBridgeable {
    }
    @available(*, deprecated, message: "BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public final class AnyBroadcastChannel<E: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel, ExportedKotlinPackages.kotlinx.coroutines.channels.__BroadcastChannel, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnyChannel<E: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.channels.Channel, ExportedKotlinPackages.kotlinx.coroutines.channels.__Channel, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnyChannelIterator<E: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator, ExportedKotlinPackages.kotlinx.coroutines.channels.__ChannelIterator, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<E> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<E>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    public final class ChannelResult<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public typealias Companion = KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_channels_ChannelResult_Companion
        @inlinable
        public var isClosed: Swift.Bool {
            get {
                return _cbridge_kotlinx_coroutines_channels_ChannelResult_isClosed_get(self.__externalRCRef())
            }
        }
        @inlinable
        public var isFailure: Swift.Bool {
            get {
                return _cbridge_kotlinx_coroutines_channels_ChannelResult_isFailure_get(self.__externalRCRef())
            }
        }
        @inlinable
        public var isSuccess: Swift.Bool {
            get {
                return _cbridge_kotlinx_coroutines_channels_ChannelResult_isSuccess_get(self.__externalRCRef())
            }
        }
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @inlinable
        public func exceptionOrNull() -> ExportedKotlinPackages.kotlin.Throwable? {
            return { switch _cbridge_kotlinx_coroutines_channels_ChannelResult_exceptionOrNull(self.__externalRCRef()) { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
        }
        @inlinable
        public func getOrNull() -> T? {
            return Swift.Optional<T>.__fromKotlinPointer(_cbridge_kotlinx_coroutines_channels_ChannelResult_getOrNull(self.__externalRCRef()))
        }
        @inlinable
        public func getOrThrow() -> T {
            return T.__fromKotlinPointer(_cbridge_kotlinx_coroutines_channels_ChannelResult_getOrThrow(self.__externalRCRef()))
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
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
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
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
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @available(*, deprecated, message: "ConflatedBroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public final class ConflatedBroadcastChannel<E: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinCustomPointerBridgeable, ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel, ExportedKotlinPackages.kotlinx.coroutines.channels.__BroadcastChannel {
        @_spi(kotlinx$coroutines$DelicateCoroutinesApi) @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public var isClosedForSend: Swift.Bool {
            @_spi(kotlinx$coroutines$DelicateCoroutinesApi) @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
            get {
                return kotlinx_coroutines_channels_ConflatedBroadcastChannel_isClosedForSend_get(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi) @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public var onSend: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2<E,ExportedKotlinPackages.kotlinx.coroutines.channels.AnySendChannel<E>> {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi) @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
            get {
                return { let _ptr = kotlinx_coroutines_channels_ConflatedBroadcastChannel_onSend_get(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2<E,ExportedKotlinPackages.kotlinx.coroutines.channels.AnySendChannel<E>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectClause2<E,ExportedKotlinPackages.kotlinx.coroutines.channels.AnySendChannel<E>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
            }
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public var value: E {
            @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
            get {
                return E.__fromKotlinPointer(kotlinx_coroutines_channels_ConflatedBroadcastChannel_value_get(self.__externalRCRef()))
            }
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public var valueOrNull: E? {
            @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
            get {
                return Swift.Optional<E>.__fromKotlinPointer(kotlinx_coroutines_channels_ConflatedBroadcastChannel_valueOrNull_get(self.__externalRCRef()))
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
            return { kotlinx_coroutines_channels_ConflatedBroadcastChannel_invokeOnClose__TypesOfArguments__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), Unmanaged.passRetained((handler as (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void) as AnyObject).toOpaque()); return () }()
        }
        @available(*, unavailable, message: "Deprecated in the favour of 'trySend' method. Replacement: trySend(element).isSuccess") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public func offer(
            element: E
        ) -> Swift.Bool {
            fatalError()
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public func openSubscription() -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<E> {
            return { let _ptr = kotlinx_coroutines_channels_ConflatedBroadcastChannel_openSubscription(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.channels.AnyReceiveChannel<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public func send(
            element: E
        ) async throws -> Swift.Void {
            return try await withKotlinContinuation { continuation, exception, cancellation in
                let _: Bool = kotlinx_coroutines_channels_ConflatedBroadcastChannel_send__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public func trySend(
            element: E
        ) -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<KotlinRuntimeSupport.KotlinUnit> {
            return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<KotlinRuntimeSupport.KotlinUnit>.__fromKotlinPointer(kotlinx_coroutines_channels_ConflatedBroadcastChannel_trySend__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer()))
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public override init() {
            let __kt = kotlinx_coroutines_channels_ConflatedBroadcastChannel_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asExistentialWrapper);
            { kotlinx_coroutines_channels_ConflatedBroadcastChannel_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public init(
            value: E
        ) {
            let __kt = kotlinx_coroutines_channels_ConflatedBroadcastChannel_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asExistentialWrapper);
            { kotlinx_coroutines_channels_ConflatedBroadcastChannel_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_E__(__kt, value.__retainedToKotlinPointer()); return () }()
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnyProducerScope<E: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope, ExportedKotlinPackages.kotlinx.coroutines.channels.__ProducerScope, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var contravariant: KotlinRuntimeSupport.KotlinContravariant<E> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._noop)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinContravariant<E>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    public final class AnyReceiveChannel<E: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel, ExportedKotlinPackages.kotlinx.coroutines.channels.__ReceiveChannel, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<E> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<E>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    public final class AnySendChannel<E: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel, ExportedKotlinPackages.kotlinx.coroutines.channels.__SendChannel, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var contravariant: KotlinRuntimeSupport.KotlinContravariant<E> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._noop)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinContravariant<E>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    @available(*, deprecated, message: "BroadcastChannel is deprecated in the favour of SharedFlow and StateFlow, and is no longer supported") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public static func broadcastChannel<E: KotlinRuntimeSupport._KotlinBridgeable>(
        capacity: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel<E> {
        return { let _ptr = kotlinx_coroutines_channels_BroadcastChannel__TypesOfArguments__Swift_Int32__(capacity); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.channels.AnyBroadcastChannel<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public static func channel<E: KotlinRuntimeSupport._KotlinBridgeable>(
        capacity: Swift.Int32,
        onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow,
        onUndeliveredElement: ((E) -> Swift.Void)?
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.channels.Channel<E> {
        return { let _ptr = kotlinx_coroutines_channels_Channel__TypesOfArguments__Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow_Swift_Optional_U28EU29202D_U20Swift_Void___(capacity, onBufferOverflow.__externalRCRef(), onUndeliveredElement.map { it in Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (it)(E.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque() } ?? nil); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.channels.Channel<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.channels.AnyChannel<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public static func awaitClose(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope,
        block: @escaping () -> Swift.Void
    ) async throws -> Swift.Void {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_channels_awaitClose__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ProducerScope_U2829202D_U20Swift_Void__(receiver.__externalRCRef(), Unmanaged.passRetained((block as () -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    @available(*, deprecated, message: "BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public static func broadcast<E: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        capacity: Swift.Int32,
        start: ExportedKotlinPackages.kotlinx.coroutines.CoroutineStart,
        onCompletion: ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler?,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope<E>) async throws -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel<E> {
        return { let _ptr = kotlinx_coroutines_channels_broadcast__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_CoroutineStart_Swift_Optional_U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ProducerScope_E_U2920asyncU20throwsU202D_U20Swift_Void__(receiver.__externalRCRef(), context.__externalRCRef(), capacity, start.__externalRCRef(), onCompletion.map { it in Unmanaged.passRetained((it as (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void) as AnyObject).toOpaque() } ?? nil, Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void in return try await (block)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.channels.AnyProducerScope<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void) as AnyObject).toOpaque()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.channels.AnyBroadcastChannel<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    @available(*, deprecated, message: "BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public static func broadcast<E: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<E>,
        capacity: Swift.Int32,
        start: ExportedKotlinPackages.kotlinx.coroutines.CoroutineStart
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel<E> {
        return { let _ptr = kotlinx_coroutines_channels_broadcast__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ReceiveChannel_E__Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_CoroutineStart__(receiver.__externalRCRef(), capacity, start.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.channels.AnyBroadcastChannel<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    @available(*, unavailable, message: "BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public static func consume<E: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel<E>,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<E>) -> R
    ) -> R {
        fatalError()
    }
    public static func consume<E: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<E>,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<E>) -> R
    ) -> R {
        return R.__fromKotlinPointer(kotlinx_coroutines_channels_consume__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ReceiveChannel_E__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ReceiveChannel_E_U29202D_U20R__(receiver.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = (block)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.channels.AnyReceiveChannel<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()))
    }
    @available(*, unavailable, message: "BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public static func consumeEach<E: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel<E>,
        action: @escaping (E) -> Swift.Void
    ) async throws -> Swift.Void {
        fatalError()
    }
    public static func consumeEach<E: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<E>,
        action: @escaping (E) -> Swift.Void
    ) async throws -> Swift.Void {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_channels_consumeEach__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ReceiveChannel_E__U28EU29202D_U20Swift_Void__(receiver.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (action)(E.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func getOrElse<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<T>,
        onFailure: @escaping (ExportedKotlinPackages.kotlin.Throwable?) -> T
    ) -> T {
        return T.__fromKotlinPointer(kotlinx_coroutines_channels_getOrElse__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_channels_ChannelResult_T__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20T__(receiver.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = (onFailure)(_p0); return _res.__retainedToKotlinPointer() } as (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()))
    }
    public static func onClosed<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<T>,
        action: @escaping (ExportedKotlinPackages.kotlin.Throwable?) -> Swift.Void
    ) -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<T> {
        return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<T>.__fromKotlinPointer(kotlinx_coroutines_channels_onClosed__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_channels_ChannelResult_T__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(receiver.__externalRCRef(), Unmanaged.passRetained((action as (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void) as AnyObject).toOpaque()))
    }
    public static func onFailure<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<T>,
        action: @escaping (ExportedKotlinPackages.kotlin.Throwable?) -> Swift.Void
    ) -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<T> {
        return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<T>.__fromKotlinPointer(kotlinx_coroutines_channels_onFailure__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_channels_ChannelResult_T__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(receiver.__externalRCRef(), Unmanaged.passRetained((action as (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void) as AnyObject).toOpaque()))
    }
    public static func onSuccess<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<T>,
        action: @escaping (T) -> Swift.Void
    ) -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<T> {
        return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<T>.__fromKotlinPointer(kotlinx_coroutines_channels_onSuccess__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_channels_ChannelResult_T__U28TU29202D_U20Swift_Void__(receiver.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (action)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque()))
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public static func produce<E: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        capacity: Swift.Int32,
        start: ExportedKotlinPackages.kotlinx.coroutines.CoroutineStart,
        onCompletion: ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler?,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope<E>) async throws -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<E> {
        return { let _ptr = kotlinx_coroutines_channels_produce__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_CoroutineStart_Swift_Optional_U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ProducerScope_E_U2920asyncU20throwsU202D_U20Swift_Void__(receiver.__externalRCRef(), context.__externalRCRef(), capacity, start.__externalRCRef(), onCompletion.map { it in Unmanaged.passRetained((it as (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void) as AnyObject).toOpaque() } ?? nil, Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void in return try await (block)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.channels.AnyProducerScope<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void) as AnyObject).toOpaque()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.channels.AnyReceiveChannel<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func produce<E: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        capacity: Swift.Int32,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope<E>) async throws -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<E> {
        return { let _ptr = kotlinx_coroutines_channels_produce__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ProducerScope_E_U2920asyncU20throwsU202D_U20Swift_Void__(receiver.__externalRCRef(), context.__externalRCRef(), capacity, Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void in return try await (block)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.channels.AnyProducerScope<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void) as AnyObject).toOpaque()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.channels.AnyReceiveChannel<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public static func toList<E: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<E>
    ) async throws -> [E] {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_channels_toList__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ReceiveChannel_E___(receiver.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(Swift.Array<E>.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    @inlinable
    public static func trySendBlocking<E: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel<E>,
        element: E
    ) -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<KotlinRuntimeSupport.KotlinUnit> {
        return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<KotlinRuntimeSupport.KotlinUnit>.__fromKotlinPointer(_cbridge_kotlinx_coroutines_channels_trySendBlocking__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_SendChannel_E__E__(receiver.__externalRCRef(), element.__retainedToKotlinPointer()))
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
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            switch kotlinx_coroutines_flow_SharingCommand_ordinal(__externalRCRefUnsafe) {
            case 0: self = .START
            case 1: self = .STOP
            case 2: self = .STOP_AND_RESET_REPLAY_CACHE
            default: fatalError()
            }
        }
        public func __externalRCRef() -> Swift.UnsafeMutableRawPointer {
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
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi) @_documentation(visibility: internal)
    public protocol _AbstractFlow_ReverseDispatch<T>: KotlinRuntime.KotlinBase {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        func collectSafely(
            collector: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>
        ) async throws -> Swift.Void
    }
    public protocol Flow: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow._Flow, KotlinCoroutineSupport.KotlinFlow {
        func collect(
            collector: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>
        ) async throws -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_flow_Flow)
    public protocol _Flow {
    }
    public protocol __Flow: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol FlowCollector<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow._FlowCollector {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        func emit(
            value: T
        ) async throws -> Swift.Void
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector)
    public protocol _FlowCollector {
    }
    public protocol __FlowCollector: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol MutableSharedFlow: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow, ExportedKotlinPackages.kotlinx.coroutines.flow._MutableSharedFlow, KotlinCoroutineSupport.KotlinMutableSharedFlow {
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
    public protocol _MutableSharedFlow: ExportedKotlinPackages.kotlinx.coroutines.flow._SharedFlow {
    }
    public protocol __MutableSharedFlow: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.flow.__SharedFlow {
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
            collector: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>
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
    open class AbstractFlow<T: KotlinRuntimeSupport._KotlinBridgeable>: ExportedKotlinPackages.kotlinx.coroutines.flow._AbstractFlow_Base, KotlinRuntimeSupport._KotlinOpenGenericBridgeable, ExportedKotlinPackages.kotlinx.coroutines.flow._AbstractFlow_ReverseDispatch, ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, ExportedKotlinPackages.kotlinx.coroutines.flow.__Flow {
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        public final func collect(
            collector: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>
        ) async throws -> Swift.Void {
            return try await withKotlinContinuation { continuation, exception, cancellation in
                let _: Bool = kotlinx_coroutines_flow_AbstractFlow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_T___(self.__externalRCRef(), collector.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        open func collectSafely(
            collector: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>
        ) async throws -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.flow.AbstractFlow<T>.self {
                return try await withKotlinContinuation { continuation, exception, cancellation in
                let _: Bool = kotlinx_coroutines_flow_AbstractFlow_collectSafely__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_T___(self.__externalRCRef(), collector.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
            }
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlinx.coroutines.flow.AbstractFlow.collectSafely': a Swift subclass must override it and must not call super.")
            }
        }
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        public init() {
            precondition(Self.self != ExportedKotlinPackages.kotlinx.coroutines.flow.AbstractFlow<T>.self, "ExportedKotlinPackages.kotlinx.coroutines.flow.AbstractFlow is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_flow_AbstractFlow_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi) @_documentation(visibility: internal)
    open class _AbstractFlow_Base: KotlinRuntime.KotlinBase {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnyFlowCollector<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, ExportedKotlinPackages.kotlinx.coroutines.flow.__FlowCollector, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var contravariant: KotlinRuntimeSupport.KotlinContravariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._noop)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinContravariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
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
    public static func mutableSharedFlow<T: KotlinRuntimeSupport._KotlinBridgeable>(
        replay: Swift.Int32,
        extraBufferCapacity: Swift.Int32,
        onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow
    ) -> any KotlinCoroutineSupport.KotlinTypedMutableSharedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedMutableSharedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_MutableSharedFlow__TypesOfArguments__Swift_Int32_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow__(replay, extraBufferCapacity, onBufferOverflow.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow, T.Type.self)
    }
    public static func mutableStateFlow<T: KotlinRuntimeSupport._KotlinBridgeable>(
        value: T
    ) -> any KotlinCoroutineSupport.KotlinTypedMutableStateFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedMutableStateFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_MutableStateFlow__TypesOfArguments__T__(value.__retainedToKotlinPointer()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow, T.Type.self)
    }
    public static func callbackFlow<T: KotlinRuntimeSupport._KotlinBridgeable>(
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope<T>) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_callbackFlow__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ProducerScope_T_U2920asyncU20throwsU202D_U20Swift_Void__(Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void in return try await (block)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.channels.AnyProducerScope<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func channelFlow<T: KotlinRuntimeSupport._KotlinBridgeable>(
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope<T>) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_channelFlow__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ProducerScope_T_U2920asyncU20throwsU202D_U20Swift_Void__(Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void in return try await (block)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.channels.AnyProducerScope<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func combine<T1: KotlinRuntimeSupport._KotlinBridgeable, T2: KotlinRuntimeSupport._KotlinBridgeable, T3: KotlinRuntimeSupport._KotlinBridgeable, T4: KotlinRuntimeSupport._KotlinBridgeable, T5: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<T1>,
        flow2: any KotlinCoroutineSupport.KotlinTypedFlow<T2>,
        flow3: any KotlinCoroutineSupport.KotlinTypedFlow<T3>,
        flow4: any KotlinCoroutineSupport.KotlinTypedFlow<T4>,
        flow5: any KotlinCoroutineSupport.KotlinTypedFlow<T5>,
        transform: @escaping (T1, T2, T3, T4, T5) async throws -> R
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combine__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T1__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T2__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T3__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T4__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T5__U28T1_U20T2_U20T3_U20T4_U20T5U2920asyncU20throwsU202D_U20R__(flow.wrapped.__externalRCRef(), flow2.wrapped.__externalRCRef(), flow3.wrapped.__externalRCRef(), flow4.wrapped.__externalRCRef(), flow5.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p2: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p3: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p4: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = try await (transform)(T1.__fromKotlinPointer(_p0), T2.__fromKotlinPointer(_p1), T3.__fromKotlinPointer(_p2), T4.__fromKotlinPointer(_p3), T5.__fromKotlinPointer(_p4)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    public static func combine<T1: KotlinRuntimeSupport._KotlinBridgeable, T2: KotlinRuntimeSupport._KotlinBridgeable, T3: KotlinRuntimeSupport._KotlinBridgeable, T4: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<T1>,
        flow2: any KotlinCoroutineSupport.KotlinTypedFlow<T2>,
        flow3: any KotlinCoroutineSupport.KotlinTypedFlow<T3>,
        flow4: any KotlinCoroutineSupport.KotlinTypedFlow<T4>,
        transform: @escaping (T1, T2, T3, T4) async throws -> R
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combine__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T1__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T2__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T3__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T4__U28T1_U20T2_U20T3_U20T4U2920asyncU20throwsU202D_U20R__(flow.wrapped.__externalRCRef(), flow2.wrapped.__externalRCRef(), flow3.wrapped.__externalRCRef(), flow4.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p2: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p3: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = try await (transform)(T1.__fromKotlinPointer(_p0), T2.__fromKotlinPointer(_p1), T3.__fromKotlinPointer(_p2), T4.__fromKotlinPointer(_p3)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    public static func combine<T1: KotlinRuntimeSupport._KotlinBridgeable, T2: KotlinRuntimeSupport._KotlinBridgeable, T3: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<T1>,
        flow2: any KotlinCoroutineSupport.KotlinTypedFlow<T2>,
        flow3: any KotlinCoroutineSupport.KotlinTypedFlow<T3>,
        transform: @escaping (T1, T2, T3) async throws -> R
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combine__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T1__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T2__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T3__U28T1_U20T2_U20T3U2920asyncU20throwsU202D_U20R__(flow.wrapped.__externalRCRef(), flow2.wrapped.__externalRCRef(), flow3.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p2: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = try await (transform)(T1.__fromKotlinPointer(_p0), T2.__fromKotlinPointer(_p1), T3.__fromKotlinPointer(_p2)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    public static func combine<T1: KotlinRuntimeSupport._KotlinBridgeable, T2: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<T1>,
        flow2: any KotlinCoroutineSupport.KotlinTypedFlow<T2>,
        transform: @escaping (T1, T2) async throws -> R
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combine__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T1__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T2__U28T1_U20T2U2920asyncU20throwsU202D_U20R__(flow.wrapped.__externalRCRef(), flow2.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = try await (transform)(T1.__fromKotlinPointer(_p0), T2.__fromKotlinPointer(_p1)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    public static func combine<T1: KotlinRuntimeSupport._KotlinBridgeable, T2: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T1>,
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<T2>,
        transform: @escaping (T1, T2) async throws -> R
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combine__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T1__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T2__U28T1_U20T2U2920asyncU20throwsU202D_U20R__(receiver.wrapped.__externalRCRef(), flow.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = try await (transform)(T1.__fromKotlinPointer(_p0), T2.__fromKotlinPointer(_p1)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    public static func combineTransform<T1: KotlinRuntimeSupport._KotlinBridgeable, T2: KotlinRuntimeSupport._KotlinBridgeable, T3: KotlinRuntimeSupport._KotlinBridgeable, T4: KotlinRuntimeSupport._KotlinBridgeable, T5: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<T1>,
        flow2: any KotlinCoroutineSupport.KotlinTypedFlow<T2>,
        flow3: any KotlinCoroutineSupport.KotlinTypedFlow<T3>,
        flow4: any KotlinCoroutineSupport.KotlinTypedFlow<T4>,
        flow5: any KotlinCoroutineSupport.KotlinTypedFlow<T5>,
        transform: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<R>, T1, T2, T3, T4, T5) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combineTransform__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T1__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T2__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T3__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T4__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T5__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_R__U20T1_U20T2_U20T3_U20T4_U20T5U2920asyncU20throwsU202D_U20Swift_Void__(flow.wrapped.__externalRCRef(), flow2.wrapped.__externalRCRef(), flow3.wrapped.__externalRCRef(), flow4.wrapped.__externalRCRef(), flow5.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p2: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p3: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p4: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p5: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void in return try await (transform)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<R>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<R>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }(), T1.__fromKotlinPointer(_p1), T2.__fromKotlinPointer(_p2), T3.__fromKotlinPointer(_p3), T4.__fromKotlinPointer(_p4), T5.__fromKotlinPointer(_p5)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    public static func combineTransform<T1: KotlinRuntimeSupport._KotlinBridgeable, T2: KotlinRuntimeSupport._KotlinBridgeable, T3: KotlinRuntimeSupport._KotlinBridgeable, T4: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<T1>,
        flow2: any KotlinCoroutineSupport.KotlinTypedFlow<T2>,
        flow3: any KotlinCoroutineSupport.KotlinTypedFlow<T3>,
        flow4: any KotlinCoroutineSupport.KotlinTypedFlow<T4>,
        transform: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<R>, T1, T2, T3, T4) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combineTransform__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T1__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T2__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T3__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T4__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_R__U20T1_U20T2_U20T3_U20T4U2920asyncU20throwsU202D_U20Swift_Void__(flow.wrapped.__externalRCRef(), flow2.wrapped.__externalRCRef(), flow3.wrapped.__externalRCRef(), flow4.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p2: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p3: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p4: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void in return try await (transform)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<R>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<R>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }(), T1.__fromKotlinPointer(_p1), T2.__fromKotlinPointer(_p2), T3.__fromKotlinPointer(_p3), T4.__fromKotlinPointer(_p4)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    public static func combineTransform<T1: KotlinRuntimeSupport._KotlinBridgeable, T2: KotlinRuntimeSupport._KotlinBridgeable, T3: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<T1>,
        flow2: any KotlinCoroutineSupport.KotlinTypedFlow<T2>,
        flow3: any KotlinCoroutineSupport.KotlinTypedFlow<T3>,
        transform: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<R>, T1, T2, T3) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combineTransform__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T1__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T2__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T3__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_R__U20T1_U20T2_U20T3U2920asyncU20throwsU202D_U20Swift_Void__(flow.wrapped.__externalRCRef(), flow2.wrapped.__externalRCRef(), flow3.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p2: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p3: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void in return try await (transform)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<R>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<R>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }(), T1.__fromKotlinPointer(_p1), T2.__fromKotlinPointer(_p2), T3.__fromKotlinPointer(_p3)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    public static func combineTransform<T1: KotlinRuntimeSupport._KotlinBridgeable, T2: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<T1>,
        flow2: any KotlinCoroutineSupport.KotlinTypedFlow<T2>,
        transform: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<R>, T1, T2) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combineTransform__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T1__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T2__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_R__U20T1_U20T2U2920asyncU20throwsU202D_U20Swift_Void__(flow.wrapped.__externalRCRef(), flow2.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p2: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void in return try await (transform)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<R>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<R>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }(), T1.__fromKotlinPointer(_p1), T2.__fromKotlinPointer(_p2)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    public static func combineTransform<T1: KotlinRuntimeSupport._KotlinBridgeable, T2: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T1>,
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<T2>,
        transform: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<R>, T1, T2) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combineTransform__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T1__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T2__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_R__U20T1_U20T2U2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), flow.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p2: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void in return try await (transform)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<R>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<R>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }(), T1.__fromKotlinPointer(_p1), T2.__fromKotlinPointer(_p2)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    public static func emptyFlow<T: KotlinRuntimeSupport._KotlinBridgeable>() -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_emptyFlow(), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func flow<T: KotlinRuntimeSupport._KotlinBridgeable>(
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flow__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_T_U2920asyncU20throwsU202D_U20Swift_Void__(Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void in return try await (block)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func flowOf<T: KotlinRuntimeSupport._KotlinBridgeable>(
        value: T
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flowOf__TypesOfArguments__T__(value.__retainedToKotlinPointer()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func flowOf<T: KotlinRuntimeSupport._KotlinBridgeable>(
        elements: T...
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flowOf__TypesOfArguments__Swift_Array_T__Vararg___(elements), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func merge<T: KotlinRuntimeSupport._KotlinBridgeable>(
        flows: any KotlinCoroutineSupport.KotlinTypedFlow<T>...
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_merge__TypesOfArguments__Swift_Array_anyU20KotlinCoroutineSupport_KotlinTypedFlow_T___Vararg___(flows), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func merge<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlin.collections.Iterable<any KotlinCoroutineSupport.KotlinTypedFlow<T>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_merge__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_collections_Iterable_anyU20KotlinCoroutineSupport_KotlinTypedFlow_T____(receiver.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func WhileSubscribed(
        _ receiver: KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_flow_SharingStarted_Companion,
        stopTimeout: ExportedKotlinPackages.kotlin.time.Duration,
        replayExpiration: ExportedKotlinPackages.kotlin.time.Duration
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_WhileSubscribed__TypesOfArgumentsE__KotlinxCoroutinesCore__ExportedKotlinPackages_kotlinx_coroutines_flow_SharingStarted_Companion_ExportedKotlinPackages_kotlin_time_Duration_ExportedKotlinPackages_kotlin_time_Duration__(receiver.__externalRCRef(), stopTimeout.__externalRCRef(), replayExpiration.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted
    }
    public static func asFlow<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: @escaping () -> T
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__U2829202D_U20T__(Unmanaged.passRetained(({ let _res: T = (receiver)(); return _res.__retainedToKotlinPointer() } as () -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func asFlow<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: ExportedKotlinPackages.kotlin.Array<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__ExportedKotlinPackages_kotlin_Array_T___(receiver.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
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
    public static func asFlow<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlin.collections.Iterable<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_collections_Iterable_T___(receiver.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func asFlow<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlin.collections.Iterator<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_collections_Iterator_T___(receiver.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
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
    public static func asFlow<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlin.sequences.Sequence<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_sequences_Sequence_T___(receiver.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func asFlow<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: @escaping () async throws -> T
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__U282920asyncU20throwsU202D_U20T__(Unmanaged.passRetained(({ () async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = try await (receiver)(); return _res.__retainedToKotlinPointer() } as () async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    @available(*, unavailable, message: "'BroadcastChannel' is obsolete and all corresponding operators are deprecated in the favour of StateFlow and SharedFlow") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public static func asFlow<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    public static func asSharedFlow<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedMutableSharedFlow<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedSharedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedSharedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asSharedFlow__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedMutableSharedFlow_T___(receiver.wrapped.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow, T.Type.self)
    }
    public static func asStateFlow<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedMutableStateFlow<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedStateFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedStateFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asStateFlow__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedMutableStateFlow_T___(receiver.wrapped.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow, T.Type.self)
    }
    public static func buffer<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        capacity: Swift.Int32,
        onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_buffer__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow__(receiver.wrapped.__externalRCRef(), capacity, onBufferOverflow.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    @available(*, unavailable, message: "Flow analogue of 'cache()' is 'shareIn' with unlimited replay and 'started = SharingStared.Lazily' argument'. Replacement: this.shareIn(scope, Int.MAX_VALUE, started = SharingStared.Lazily)")
    public static func cache<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    @available(*, unavailable, message: "cancel() is resolved into the extension of outer CoroutineScope which is likely to be an error.Use currentCoroutineContext().cancel() instead or specify the receiver of cancel() explicitly. Replacement: currentCoroutineContext().cancel(cause)")
    public static func cancel(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector,
        cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
    ) -> Swift.Void {
        fatalError()
    }
    public static func cancellable<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_cancellable__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T___(receiver.wrapped.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    @available(*, unavailable, message: "Applying 'cancellable' to a SharedFlow has no effect. See the SharedFlow documentation on Operator Fusion.. Replacement: this")
    public static func cancellable<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    public static func `catch`<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        action: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>, ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_catch__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_T__U20ExportedKotlinPackages_kotlin_ThrowableU2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Void in return try await (action)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }(), _p1) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    @available(*, deprecated, message: "SharedFlow never completes, so this operator typically has not effect, it can only catch exceptions from 'onSubscribe' operator. Replacement: this")
    public static func `catch`<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<T>,
        action: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>, ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_catch__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_T__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_T__U20ExportedKotlinPackages_kotlin_ThrowableU2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Void in return try await (action)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }(), _p1) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func collect(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow
    ) async throws -> Swift.Void {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_collect__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_Flow__(receiver.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func collectIndexed<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        action: @escaping (Swift.Int32, T) async throws -> Swift.Void
    ) async throws -> Swift.Void {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_collectIndexed__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28Swift_Int32_U20TU2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Int32, _p1: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void in return try await (action)(_p0, T.__fromKotlinPointer(_p1)) } as (Swift.Int32, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func collectLatest<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        action: @escaping (T) async throws -> Swift.Void
    ) async throws -> Swift.Void {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_collectLatest__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28TU2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void in return try await (action)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    @available(*, unavailable, message: "Flow analogue of 'combineLatest' is 'combine'. Replacement: combine(this, other, other2, other3, transform)")
    public static func combineLatest<T1: KotlinRuntimeSupport._KotlinBridgeable, T2: KotlinRuntimeSupport._KotlinBridgeable, T3: KotlinRuntimeSupport._KotlinBridgeable, T4: KotlinRuntimeSupport._KotlinBridgeable, T5: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T1>,
        other: any KotlinCoroutineSupport.KotlinTypedFlow<T2>,
        other2: any KotlinCoroutineSupport.KotlinTypedFlow<T3>,
        other3: any KotlinCoroutineSupport.KotlinTypedFlow<T4>,
        other4: any KotlinCoroutineSupport.KotlinTypedFlow<T5>,
        transform: @escaping (T1, T2, T3, T4, T5) async throws -> R
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'combineLatest' is 'combine'. Replacement: combine(this, other, other2, other3, transform)")
    public static func combineLatest<T1: KotlinRuntimeSupport._KotlinBridgeable, T2: KotlinRuntimeSupport._KotlinBridgeable, T3: KotlinRuntimeSupport._KotlinBridgeable, T4: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T1>,
        other: any KotlinCoroutineSupport.KotlinTypedFlow<T2>,
        other2: any KotlinCoroutineSupport.KotlinTypedFlow<T3>,
        other3: any KotlinCoroutineSupport.KotlinTypedFlow<T4>,
        transform: @escaping (T1, T2, T3, T4) async throws -> R
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'combineLatest' is 'combine'. Replacement: combine(this, other, other2, transform)")
    public static func combineLatest<T1: KotlinRuntimeSupport._KotlinBridgeable, T2: KotlinRuntimeSupport._KotlinBridgeable, T3: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T1>,
        other: any KotlinCoroutineSupport.KotlinTypedFlow<T2>,
        other2: any KotlinCoroutineSupport.KotlinTypedFlow<T3>,
        transform: @escaping (T1, T2, T3) async throws -> R
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'combineLatest' is 'combine'. Replacement: this.combine(other, transform)")
    public static func combineLatest<T1: KotlinRuntimeSupport._KotlinBridgeable, T2: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T1>,
        other: any KotlinCoroutineSupport.KotlinTypedFlow<T2>,
        transform: @escaping (T1, T2) async throws -> R
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'compose' is 'let'. Replacement: let(transformer)")
    public static func compose<T: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        transformer: @escaping (any KotlinCoroutineSupport.KotlinTypedFlow<T>) -> any KotlinCoroutineSupport.KotlinTypedFlow<R>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'concatMap' is 'flatMapConcat'. Replacement: flatMapConcat(mapper)")
    public static func concatMap<T: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        mapper: @escaping (T) -> any KotlinCoroutineSupport.KotlinTypedFlow<R>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'concatWith' is 'onCompletion'. Use 'onCompletion { emit(value) }'. Replacement: onCompletion { emit(value) }")
    public static func concatWith<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        value: T
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'concatWith' is 'onCompletion'. Use 'onCompletion { if (it == null) emitAll(other) }'. Replacement: onCompletion { if (it == null) emitAll(other) }")
    public static func concatWith<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        other: any KotlinCoroutineSupport.KotlinTypedFlow<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    public static func conflate<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_conflate__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T___(receiver.wrapped.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    @available(*, unavailable, message: "Applying 'conflate' to StateFlow has no effect. See the StateFlow documentation on Operator Fusion.. Replacement: this")
    public static func conflate<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedStateFlow<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    public static func consumeAsFlow<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_consumeAsFlow__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ReceiveChannel_T___(receiver.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func count<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>
    ) async throws -> Swift.Int32 {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_count__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T___(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Int32) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func count<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        predicate: @escaping (T) async throws -> Swift.Bool
    ) async throws -> Swift.Int32 {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_count__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28TU2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Bool in return try await (predicate)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Bool) as AnyObject).toOpaque(), Unmanaged.passRetained((continuation as (Swift.Int32) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    @available(*, deprecated, message: "SharedFlow never completes, so this terminal operation never completes.")
    public static func count<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<T>
    ) async throws -> Swift.Int32 {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_count__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_T___(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Int32) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static func debounce<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        timeoutMillis: @escaping (T) -> Swift.Int64
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_debounce__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28TU29202D_U20Swift_Int64__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Int64 in return (timeoutMillis)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Int64) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static func debounce<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        timeout: @escaping (T) -> ExportedKotlinPackages.kotlin.time.Duration
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_debounce__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28TU29202D_U20ExportedKotlinPackages_kotlin_time_Duration__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> ExportedKotlinPackages.kotlin.time.Duration in return (timeout)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> ExportedKotlinPackages.kotlin.time.Duration) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static func debounce<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        timeoutMillis: Swift.Int64
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_debounce__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__Swift_Int64__(receiver.wrapped.__externalRCRef(), timeoutMillis), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static func debounce<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        timeout: ExportedKotlinPackages.kotlin.time.Duration
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_debounce__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__ExportedKotlinPackages_kotlin_time_Duration__(receiver.wrapped.__externalRCRef(), timeout.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    @available(*, unavailable, message: "Use 'onEach { delay(timeMillis) }'. Replacement: onEach { delay(timeMillis) }")
    public static func delayEach<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        timeMillis: Swift.Int64
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    @available(*, unavailable, message: "Use 'onStart { delay(timeMillis) }'. Replacement: onStart { delay(timeMillis) }")
    public static func delayFlow<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        timeMillis: Swift.Int64
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    public static func distinctUntilChanged<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_distinctUntilChanged__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T___(receiver.wrapped.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func distinctUntilChanged<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        areEquivalent: @escaping (T, T) -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_distinctUntilChanged__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28T_U20TU29202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Bool in return (areEquivalent)(T.__fromKotlinPointer(_p0), T.__fromKotlinPointer(_p1)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Bool) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    @available(*, unavailable, message: "Applying 'distinctUntilChanged' to StateFlow has no effect. See the StateFlow documentation on Operator Fusion.. Replacement: this")
    public static func distinctUntilChanged<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedStateFlow<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    public static func distinctUntilChangedBy<T: KotlinRuntimeSupport._KotlinBridgeable, K: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        keySelector: @escaping (T) -> K
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_distinctUntilChangedBy__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28TU29202D_U20K__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: K = (keySelector)(T.__fromKotlinPointer(_p0)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func drop<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        count: Swift.Int32
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_drop__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__Swift_Int32__(receiver.wrapped.__externalRCRef(), count), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func dropWhile<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        predicate: @escaping (T) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_dropWhile__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28TU2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Bool in return try await (predicate)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Bool) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func emitAll<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>,
        channel: any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<T>
    ) async throws -> Swift.Void {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_emitAll__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_T__anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ReceiveChannel_T___(receiver.__externalRCRef(), channel.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func emitAll<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>,
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<T>
    ) async throws -> Swift.Void {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_emitAll__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_T__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T___(receiver.__externalRCRef(), flow.wrapped.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func filter<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        predicate: @escaping (T) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_filter__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28TU2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Bool in return try await (predicate)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Bool) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    @available(*, unavailable, message: "Declaration uses unsupported types")
    public static func filterIsInstance<R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow,
        klass: Swift.Never
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        fatalError()
    }
    public static func filterNot<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        predicate: @escaping (T) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_filterNot__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28TU2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Bool in return try await (predicate)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Bool) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func filterNotNull<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<T>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_filterNotNull__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_T____(receiver.wrapped.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func first<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>
    ) async throws -> T {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_first__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T___(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func first<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        predicate: @escaping (T) async throws -> Swift.Bool
    ) async throws -> T {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_first__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28TU2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Bool in return try await (predicate)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Bool) as AnyObject).toOpaque(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func firstOrNull<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>
    ) async throws -> T? {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_firstOrNull__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T___(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(Swift.Optional<T>.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func firstOrNull<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        predicate: @escaping (T) async throws -> Swift.Bool
    ) async throws -> T? {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_firstOrNull__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28TU2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Bool in return try await (predicate)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Bool) as AnyObject).toOpaque(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(Swift.Optional<T>.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    @available(*, unavailable, message: "Flow analogue is 'flatMapConcat'. Replacement: flatMapConcat(mapper)")
    public static func flatMap<T: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        mapper: @escaping (T) async throws -> any KotlinCoroutineSupport.KotlinTypedFlow<R>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        fatalError()
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func flatMapConcat<T: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        transform: @escaping (T) async throws -> any KotlinCoroutineSupport.KotlinTypedFlow<R>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flatMapConcat__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28TU2920asyncU20throwsU202D_U20anyU20KotlinCoroutineSupport_KotlinTypedFlow_R___(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: any KotlinCoroutineSupport.KotlinTypedFlow<R> = try await (transform)(T.__fromKotlinPointer(_p0)); return KotlinRuntimeSupport._cloneBridgeableRef((_res).wrapped.__externalRCRef()) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func flatMapLatest<T: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        transform: @escaping (T) async throws -> any KotlinCoroutineSupport.KotlinTypedFlow<R>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flatMapLatest__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28TU2920asyncU20throwsU202D_U20anyU20KotlinCoroutineSupport_KotlinTypedFlow_R___(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: any KotlinCoroutineSupport.KotlinTypedFlow<R> = try await (transform)(T.__fromKotlinPointer(_p0)); return KotlinRuntimeSupport._cloneBridgeableRef((_res).wrapped.__externalRCRef()) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func flatMapMerge<T: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        concurrency: Swift.Int32,
        transform: @escaping (T) async throws -> any KotlinCoroutineSupport.KotlinTypedFlow<R>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flatMapMerge__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__Swift_Int32_U28TU2920asyncU20throwsU202D_U20anyU20KotlinCoroutineSupport_KotlinTypedFlow_R___(receiver.wrapped.__externalRCRef(), concurrency, Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: any KotlinCoroutineSupport.KotlinTypedFlow<R> = try await (transform)(T.__fromKotlinPointer(_p0)); return KotlinRuntimeSupport._cloneBridgeableRef((_res).wrapped.__externalRCRef()) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    @available(*, unavailable, message: "Flow analogue of 'flatten' is 'flattenConcat'. Replacement: flattenConcat()")
    public static func flatten<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<any KotlinCoroutineSupport.KotlinTypedFlow<T>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func flattenConcat<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<any KotlinCoroutineSupport.KotlinTypedFlow<T>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flattenConcat__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_anyU20KotlinCoroutineSupport_KotlinTypedFlow_T____(receiver.wrapped.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func flattenMerge<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<any KotlinCoroutineSupport.KotlinTypedFlow<T>>,
        concurrency: Swift.Int32
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flattenMerge__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_anyU20KotlinCoroutineSupport_KotlinTypedFlow_T___Swift_Int32__(receiver.wrapped.__externalRCRef(), concurrency), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func flowOn<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flowOn__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(receiver.wrapped.__externalRCRef(), context.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    @available(*, unavailable, message: "Applying 'flowOn' to SharedFlow has no effect. See the SharedFlow documentation on Operator Fusion.. Replacement: this")
    public static func flowOn<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<T>,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    public static func fold<T: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        initial: R,
        operation: @escaping (R, T) async throws -> R
    ) async throws -> R {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_fold__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__R_U28R_U20TU2920asyncU20throwsU202D_U20R__(receiver.wrapped.__externalRCRef(), initial.__retainedToKotlinPointer(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = try await (operation)(R.__fromKotlinPointer(_p0), T.__fromKotlinPointer(_p1)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(R.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    @available(*, unavailable, message: "Flow analogue of 'forEach' is 'collect'. Replacement: collect(action)")
    public static func forEach<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        action: @escaping (T) async throws -> Swift.Void
    ) -> Swift.Void {
        fatalError()
    }
    public static func getAndUpdate<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedMutableStateFlow<T>,
        function: @escaping (T) -> T
    ) -> T {
        return T.__fromKotlinPointer(kotlinx_coroutines_flow_getAndUpdate__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedMutableStateFlow_T__U28TU29202D_U20T__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = (function)(T.__fromKotlinPointer(_p0)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()))
    }
    public static func last<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>
    ) async throws -> T {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_last__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T___(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func lastOrNull<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>
    ) async throws -> T? {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_lastOrNull__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T___(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(Swift.Optional<T>.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func launchIn<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        scope: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.Job {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_launchIn__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope__(receiver.wrapped.__externalRCRef(), scope.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job
    }
    public static func map<T: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        transform: @escaping (T) async throws -> R
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_map__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28TU2920asyncU20throwsU202D_U20R__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = try await (transform)(T.__fromKotlinPointer(_p0)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func mapLatest<T: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        transform: @escaping (T) async throws -> R
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_mapLatest__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28TU2920asyncU20throwsU202D_U20R__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = try await (transform)(T.__fromKotlinPointer(_p0)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    public static func mapNotNull<T: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        transform: @escaping (T) async throws -> R?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_mapNotNull__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28TU2920asyncU20throwsU202D_U20Swift_Optional_R___(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: Swift.Optional<R> = try await (transform)(T.__fromKotlinPointer(_p0)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    @available(*, unavailable, message: "Flow analogue of 'merge' is 'flattenConcat'. Replacement: flattenConcat()")
    public static func merge<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<any KotlinCoroutineSupport.KotlinTypedFlow<T>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    @available(*, unavailable, message: "Collect flow in the desired context instead")
    public static func observeOn<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    public static func onCompletion<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        action: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>, ExportedKotlinPackages.kotlin.Throwable?) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_onCompletion__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_T__U20Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) async throws -> Swift.Void in return try await (action)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }(), _p1) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) async throws -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func onEach<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        action: @escaping (T) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_onEach__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28TU2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void in return try await (action)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func onEmpty<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        action: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_onEmpty__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_T_U2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void in return try await (action)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    @available(*, unavailable, message: "Flow analogue of 'onErrorXxx' is 'catch'. Use 'catch { emitAll(fallback) }'. Replacement: catch { emitAll(fallback) }")
    public static func onErrorResume<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        fallback: any KotlinCoroutineSupport.KotlinTypedFlow<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'onErrorXxx' is 'catch'. Use 'catch { emitAll(fallback) }'. Replacement: catch { emitAll(fallback) }")
    public static func onErrorResumeNext<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        fallback: any KotlinCoroutineSupport.KotlinTypedFlow<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'onErrorXxx' is 'catch'. Use 'catch { emit(fallback) }'. Replacement: catch { emit(fallback) }")
    public static func onErrorReturn<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        fallback: T
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'onErrorXxx' is 'catch'. Use 'catch { e -> if (predicate(e)) emit(fallback) else throw e }'. Replacement: catch { e -> if (predicate(e)) emit(fallback) else throw e }")
    public static func onErrorReturn<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        fallback: T,
        predicate: @escaping (ExportedKotlinPackages.kotlin.Throwable) -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    public static func onStart<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        action: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_onStart__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_T_U2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void in return try await (action)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func onSubscription<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<T>,
        action: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedSharedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedSharedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_onSubscription__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_T__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_T_U2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void in return try await (action)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow, T.Type.self)
    }
    public static func produceIn<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        scope: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<T> {
        return { let _ptr = kotlinx_coroutines_flow_produceIn__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope__(receiver.wrapped.__externalRCRef(), scope.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.channels.AnyReceiveChannel<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    @available(*, unavailable, message: """
Flow analogue of 'publish()' is 'shareIn'.
publish().connect() is the default strategy (no extra call is needed),
publish().autoConnect() translates to 'started = SharingStared.Lazily' argument,
publish().refCount() translates to 'started = SharingStared.WhileSubscribed()' argument.. Replacement: this.shareIn(scope, 0)
""")
    public static func publish<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    @available(*, unavailable, message: """
Flow analogue of 'publish(bufferSize)' is 'buffer' followed by 'shareIn'.
publish().connect() is the default strategy (no extra call is needed),
publish().autoConnect() translates to 'started = SharingStared.Lazily' argument,
publish().refCount() translates to 'started = SharingStared.WhileSubscribed()' argument.. Replacement: this.buffer(bufferSize).shareIn(scope, 0)
""")
    public static func publish<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        bufferSize: Swift.Int32
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    @available(*, unavailable, message: "Collect flow in the desired context instead")
    public static func publishOn<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    public static func receiveAsFlow<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_receiveAsFlow__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ReceiveChannel_T___(receiver.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func reduce<S: KotlinRuntimeSupport._KotlinBridgeable, T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        operation: @escaping (S, T) async throws -> S
    ) async throws -> S {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_reduce__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28S_U20TU2920asyncU20throwsU202D_U20S__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: S = try await (operation)(S.__fromKotlinPointer(_p0), T.__fromKotlinPointer(_p1)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(S.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    @available(*, unavailable, message: """
Flow analogue of 'replay()' is 'shareIn' with unlimited replay.
replay().connect() is the default strategy (no extra call is needed),
replay().autoConnect() translates to 'started = SharingStared.Lazily' argument,
replay().refCount() translates to 'started = SharingStared.WhileSubscribed()' argument.. Replacement: this.shareIn(scope, Int.MAX_VALUE)
""")
    public static func replay<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    @available(*, unavailable, message: """
Flow analogue of 'replay(bufferSize)' is 'shareIn' with the specified replay parameter.
replay().connect() is the default strategy (no extra call is needed),
replay().autoConnect() translates to 'started = SharingStared.Lazily' argument,
replay().refCount() translates to 'started = SharingStared.WhileSubscribed()' argument.. Replacement: this.shareIn(scope, bufferSize)
""")
    public static func replay<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        bufferSize: Swift.Int32
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    public static func retry<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        retries: Swift.Int64,
        predicate: @escaping (ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_retry__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__Swift_Int64_U28ExportedKotlinPackages_kotlin_ThrowableU2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), retries, Unmanaged.passRetained((predicate as (ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Bool) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    @available(*, deprecated, message: "SharedFlow never completes, so this operator has no effect.. Replacement: this")
    public static func retry<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<T>,
        retries: Swift.Int64,
        predicate: @escaping (ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_retry__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_T__Swift_Int64_U28ExportedKotlinPackages_kotlin_ThrowableU2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), retries, Unmanaged.passRetained((predicate as (ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Bool) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func retryWhen<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        predicate: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>, ExportedKotlinPackages.kotlin.Throwable, Swift.Int64) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_retryWhen__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_T__U20ExportedKotlinPackages_kotlin_Throwable_U20Swift_Int64U2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: ExportedKotlinPackages.kotlin.Throwable, _p2: Swift.Int64) async throws -> Swift.Bool in return try await (predicate)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }(), _p1, _p2) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, ExportedKotlinPackages.kotlin.Throwable, Swift.Int64) async throws -> Swift.Bool) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    @available(*, deprecated, message: "SharedFlow never completes, so this operator has no effect.. Replacement: this")
    public static func retryWhen<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<T>,
        predicate: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>, ExportedKotlinPackages.kotlin.Throwable, Swift.Int64) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_retryWhen__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_T__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_T__U20ExportedKotlinPackages_kotlin_Throwable_U20Swift_Int64U2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: ExportedKotlinPackages.kotlin.Throwable, _p2: Swift.Int64) async throws -> Swift.Bool in return try await (predicate)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }(), _p1, _p2) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, ExportedKotlinPackages.kotlin.Throwable, Swift.Int64) async throws -> Swift.Bool) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func runningFold<T: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        initial: R,
        operation: @escaping (R, T) async throws -> R
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_runningFold__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__R_U28R_U20TU2920asyncU20throwsU202D_U20R__(receiver.wrapped.__externalRCRef(), initial.__retainedToKotlinPointer(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = try await (operation)(R.__fromKotlinPointer(_p0), T.__fromKotlinPointer(_p1)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    public static func runningReduce<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        operation: @escaping (T, T) async throws -> T
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_runningReduce__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28T_U20TU2920asyncU20throwsU202D_U20T__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = try await (operation)(T.__fromKotlinPointer(_p0), T.__fromKotlinPointer(_p1)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static func sample<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        periodMillis: Swift.Int64
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_sample__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__Swift_Int64__(receiver.wrapped.__externalRCRef(), periodMillis), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static func sample<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        period: ExportedKotlinPackages.kotlin.time.Duration
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_sample__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__ExportedKotlinPackages_kotlin_time_Duration__(receiver.wrapped.__externalRCRef(), period.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func scan<T: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        initial: R,
        operation: @escaping (R, T) async throws -> R
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_scan__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__R_U28R_U20TU2920asyncU20throwsU202D_U20R__(receiver.wrapped.__externalRCRef(), initial.__retainedToKotlinPointer(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = try await (operation)(R.__fromKotlinPointer(_p0), T.__fromKotlinPointer(_p1)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    @available(*, unavailable, message: "Flow has less verbose 'scan' shortcut. Replacement: scan(initial, operation)")
    public static func scanFold<T: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        initial: R,
        operation: @escaping (R, T) async throws -> R
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        fatalError()
    }
    @available(*, unavailable, message: "'scanReduce' was renamed to 'runningReduce' to be consistent with Kotlin standard library. Replacement: runningReduce(operation)")
    public static func scanReduce<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        operation: @escaping (T, T) async throws -> T
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    public static func shareIn<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        scope: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        started: any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted,
        replay: Swift.Int32
    ) -> any KotlinCoroutineSupport.KotlinTypedSharedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedSharedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_shareIn__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_SharingStarted_Swift_Int32__(receiver.wrapped.__externalRCRef(), scope.__externalRCRef(), started.__externalRCRef(), replay), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow, T.Type.self)
    }
    public static func single<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>
    ) async throws -> T {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_single__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T___(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func singleOrNull<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>
    ) async throws -> T? {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_singleOrNull__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T___(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(Swift.Optional<T>.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    @available(*, unavailable, message: "Flow analogue of 'skip' is 'drop'. Replacement: drop(count)")
    public static func skip<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        count: Swift.Int32
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'startWith' is 'onStart'. Use 'onStart { emit(value) }'. Replacement: onStart { emit(value) }")
    public static func startWith<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        value: T
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogue of 'startWith' is 'onStart'. Use 'onStart { emitAll(other) }'. Replacement: onStart { emitAll(other) }")
    public static func startWith<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        other: any KotlinCoroutineSupport.KotlinTypedFlow<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    public static func stateIn<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        scope: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
    ) async throws -> any KotlinCoroutineSupport.KotlinTypedStateFlow<T> {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_stateIn__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope__(receiver.wrapped.__externalRCRef(), scope.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(KotlinCoroutineSupport._KotlinTypedStateFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: _p0!, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow, T.Type.self)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func stateIn<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        scope: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        started: any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted,
        initialValue: T
    ) -> any KotlinCoroutineSupport.KotlinTypedStateFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedStateFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_stateIn__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_SharingStarted_T__(receiver.wrapped.__externalRCRef(), scope.__externalRCRef(), started.__externalRCRef(), initialValue.__retainedToKotlinPointer()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow, T.Type.self)
    }
    @available(*, unavailable, message: "Use 'launchIn' with 'onEach', 'onCompletion' and 'catch' instead")
    public static func subscribe<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>
    ) -> Swift.Void {
        fatalError()
    }
    @available(*, unavailable, message: "Use 'launchIn' with 'onEach', 'onCompletion' and 'catch' instead")
    public static func subscribe<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        onEach: @escaping (T) async throws -> Swift.Void
    ) -> Swift.Void {
        fatalError()
    }
    @available(*, unavailable, message: "Use 'launchIn' with 'onEach', 'onCompletion' and 'catch' instead")
    public static func subscribe<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        onEach: @escaping (T) async throws -> Swift.Void,
        onError: @escaping (ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Void
    ) -> Swift.Void {
        fatalError()
    }
    @available(*, unavailable, message: "Use 'flowOn' instead")
    public static func subscribeOn<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        fatalError()
    }
    @available(*, unavailable, message: "Flow analogues of 'switchMap' are 'transformLatest', 'flatMapLatest' and 'mapLatest'. Replacement: this.flatMapLatest(transform)")
    public static func switchMap<T: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        transform: @escaping (T) async throws -> any KotlinCoroutineSupport.KotlinTypedFlow<R>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        fatalError()
    }
    public static func take<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        count: Swift.Int32
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_take__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__Swift_Int32__(receiver.wrapped.__externalRCRef(), count), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func takeWhile<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        predicate: @escaping (T) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_takeWhile__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28TU2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Bool in return try await (predicate)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Bool) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static func timeout<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        timeout: ExportedKotlinPackages.kotlin.time.Duration
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_timeout__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__ExportedKotlinPackages_kotlin_time_Duration__(receiver.wrapped.__externalRCRef(), timeout.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
    public static func toCollection<T: KotlinRuntimeSupport._KotlinBridgeable, C: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        destination: C
    ) async throws -> C {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_toCollection__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__C__(receiver.wrapped.__externalRCRef(), destination.__retainedToKotlinPointer(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(C.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func toList<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        destination: any ExportedKotlinPackages.kotlin.collections.MutableList<T>
    ) async throws -> [T] {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_toList__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__anyU20ExportedKotlinPackages_kotlin_collections_MutableList_T___(receiver.wrapped.__externalRCRef(), destination.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(Swift.Array<T>.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    @available(*, deprecated, message: "SharedFlow never completes, so this terminal operation never completes.")
    public static func toList<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<T>
    ) async throws -> [T] {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_toList__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_T___(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(Swift.Array<T>.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func toList<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<T>,
        destination: any ExportedKotlinPackages.kotlin.collections.MutableList<T>
    ) async throws -> Swift.Never {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_toList__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_T__anyU20ExportedKotlinPackages_kotlin_collections_MutableList_T___(receiver.wrapped.__externalRCRef(), destination.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Never) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func toSet<T: KotlinRuntimeSupport._KotlinBridgeable & Swift.Hashable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        destination: any ExportedKotlinPackages.kotlin.collections.MutableSet<T>
    ) async throws -> Swift.Set<T> {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_toSet__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__anyU20ExportedKotlinPackages_kotlin_collections_MutableSet_T___(receiver.wrapped.__externalRCRef(), destination.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(Swift.Set<T>.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    @available(*, deprecated, message: "SharedFlow never completes, so this terminal operation never completes.")
    public static func toSet<T: KotlinRuntimeSupport._KotlinBridgeable & Swift.Hashable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<T>
    ) async throws -> Swift.Set<T> {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_toSet__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_T___(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(Swift.Set<T>.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func toSet<T: KotlinRuntimeSupport._KotlinBridgeable & Swift.Hashable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<T>,
        destination: any ExportedKotlinPackages.kotlin.collections.MutableSet<T>
    ) async throws -> Swift.Never {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_toSet__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_T__anyU20ExportedKotlinPackages_kotlin_collections_MutableSet_T___(receiver.wrapped.__externalRCRef(), destination.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Never) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func transform<T: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        transform: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<R>, T) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_transform__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_R__U20TU2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void in return try await (transform)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<R>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<R>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }(), T.__fromKotlinPointer(_p1)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func transformLatest<T: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        transform: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<R>, T) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_transformLatest__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_R__U20TU2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void in return try await (transform)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<R>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<R>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }(), T.__fromKotlinPointer(_p1)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    public static func transformWhile<T: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>,
        transform: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<R>, T) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_transformWhile__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_R__U20TU2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Bool in return try await (transform)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<R>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<R>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }(), T.__fromKotlinPointer(_p1)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Bool) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    public static func update<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedMutableStateFlow<T>,
        function: @escaping (T) -> T
    ) -> Swift.Void {
        return { kotlinx_coroutines_flow_update__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedMutableStateFlow_T__U28TU29202D_U20T__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = (function)(T.__fromKotlinPointer(_p0)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()); return () }()
    }
    public static func updateAndGet<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedMutableStateFlow<T>,
        function: @escaping (T) -> T
    ) -> T {
        return T.__fromKotlinPointer(kotlinx_coroutines_flow_updateAndGet__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedMutableStateFlow_T__U28TU29202D_U20T__(receiver.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = (function)(T.__fromKotlinPointer(_p0)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()))
    }
    public static func withIndex<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<ExportedKotlinPackages.kotlin.collections.IndexedValue<T>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<ExportedKotlinPackages.kotlin.collections.IndexedValue<T>>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_withIndex__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T___(receiver.wrapped.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, ExportedKotlinPackages.kotlin.collections.IndexedValue<T>.Type.self)
    }
    public static func zip<T1: KotlinRuntimeSupport._KotlinBridgeable, T2: KotlinRuntimeSupport._KotlinBridgeable, R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<T1>,
        other: any KotlinCoroutineSupport.KotlinTypedFlow<T2>,
        transform: @escaping (T1, T2) async throws -> R
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<R> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<R>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_zip__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T1__anyU20KotlinCoroutineSupport_KotlinTypedFlow_T2__U28T1_U20T2U2920asyncU20throwsU202D_U20R__(receiver.wrapped.__externalRCRef(), other.wrapped.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>, _p1: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = try await (transform)(T1.__fromKotlinPointer(_p0), T2.__fromKotlinPointer(_p1)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, R.Type.self)
    }
    public static func flowCollector<T: KotlinRuntimeSupport._KotlinBridgeable>(
        function: @escaping (T) async throws -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T> {
        return { let _ptr = kotlinx_coroutines_flow_FlowCollector__TypesOfArguments__U28TU2920asyncU20throwsU202D_U20Swift_Void__(Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void in return try await (function)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void) as AnyObject).toOpaque()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public static func sharingStarted(
        function: @escaping (any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Int32>) -> any KotlinCoroutineSupport.KotlinTypedFlow<ExportedKotlinPackages.kotlinx.coroutines.flow.SharingCommand>
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_SharingStarted__TypesOfArguments__U28anyU20KotlinCoroutineSupport_KotlinTypedStateFlow_Swift_Int32_U29202D_U20anyU20KotlinCoroutineSupport_KotlinTypedFlow_ExportedKotlinPackages_kotlinx_coroutines_flow_SharingCommand___(Unmanaged.passRetained((function as (any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Int32>) -> any KotlinCoroutineSupport.KotlinTypedFlow<ExportedKotlinPackages.kotlinx.coroutines.flow.SharingCommand>) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.`internal` {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi) @_documentation(visibility: internal)
    public protocol _ChannelFlow_ReverseDispatch<T>: KotlinRuntime.KotlinBase {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func collect(
            collector: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>
        ) async throws -> Swift.Void
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func dropChannelOperators() -> (any KotlinCoroutineSupport.KotlinTypedFlow<T>)?
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func fuse(
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
            capacity: Swift.Int32,
            onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow
        ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T>
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func produceImpl(
            scope: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<T>
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol FusibleFlow<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`._FusibleFlow {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func fuse(
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
            capacity: Swift.Int32,
            onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow
        ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T>
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_flow_internal_FusibleFlow)
    public protocol _FusibleFlow: ExportedKotlinPackages.kotlinx.coroutines.flow._Flow {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol __FusibleFlow: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.flow.__Flow {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    open class ChannelFlow<T: KotlinRuntimeSupport._KotlinBridgeable>: ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`._ChannelFlow_Base, KotlinRuntimeSupport._KotlinOpenGenericBridgeable, ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`._ChannelFlow_ReverseDispatch, ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.FusibleFlow, ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.__FusibleFlow {
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
                return ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow.__fromKotlinPointer(kotlinx_coroutines_flow_internal_ChannelFlow_onBufferOverflow_get(self.__externalRCRef()))
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func collect(
            collector: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<T>
        ) async throws -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.ChannelFlow<T>.self {
                return try await withKotlinContinuation { continuation, exception, cancellation in
                let _: Bool = kotlinx_coroutines_flow_internal_ChannelFlow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_T___(self.__externalRCRef(), collector.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
            }
            } else {
                return try await withKotlinContinuation { continuation, exception, cancellation in
                let _: Bool = kotlinx_coroutines_flow_internal_ChannelFlow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_T____direct(self.__externalRCRef(), collector.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
            }
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func dropChannelOperators() -> (any KotlinCoroutineSupport.KotlinTypedFlow<T>)? {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.ChannelFlow<T>.self {
                return { switch kotlinx_coroutines_flow_internal_ChannelFlow_dropChannelOperators(self.__externalRCRef()) { case nil: .none; case let res?: KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self); } }()
            } else {
                return { switch kotlinx_coroutines_flow_internal_ChannelFlow_dropChannelOperators_direct(self.__externalRCRef()) { case nil: .none; case let res?: KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self); } }()
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func fuse(
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
            capacity: Swift.Int32,
            onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow
        ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.ChannelFlow<T>.self {
                return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_internal_ChannelFlow_fuse__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow__(self.__externalRCRef(), context.__externalRCRef(), capacity, onBufferOverflow.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
            } else {
                return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_internal_ChannelFlow_fuse__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow___direct(self.__externalRCRef(), context.__externalRCRef(), capacity, onBufferOverflow.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func produceImpl(
            scope: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<T> {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.ChannelFlow<T>.self {
                return { let _ptr = kotlinx_coroutines_flow_internal_ChannelFlow_produceImpl__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope__(self.__externalRCRef(), scope.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.channels.AnyReceiveChannel<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
            } else {
                return { let _ptr = kotlinx_coroutines_flow_internal_ChannelFlow_produceImpl__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope___direct(self.__externalRCRef(), scope.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.channels.AnyReceiveChannel<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public init(
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
            capacity: Swift.Int32,
            onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow
        ) {
            precondition(Self.self != ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.ChannelFlow<T>.self, "ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.ChannelFlow is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_flow_internal_ChannelFlow_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow__(__kt, context.__externalRCRef(), capacity, onBufferOverflow.__externalRCRef()); return () }()
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi) @_documentation(visibility: internal)
    open class _ChannelFlow_Base: KotlinRuntime.KotlinBase {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public final class AnyFusibleFlow<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.FusibleFlow, ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.__FusibleFlow, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public final class SendingCollector<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinCustomPointerBridgeable, ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, ExportedKotlinPackages.kotlinx.coroutines.flow.__FlowCollector {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public func emit(
            value: T
        ) async throws -> Swift.Void {
            return try await withKotlinContinuation { continuation, exception, cancellation in
                let _: Bool = kotlinx_coroutines_flow_internal_SendingCollector_emit__TypesOfArguments__T__(self.__externalRCRef(), value.__retainedToKotlinPointer(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public init(
            channel: any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel<T>
        ) {
            let __kt = kotlinx_coroutines_flow_internal_SendingCollector_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asExistentialWrapper);
            { kotlinx_coroutines_flow_internal_SendingCollector_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_SendChannel_T___(__kt, channel.__externalRCRef()); return () }()
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.`internal` {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public typealias SynchronizedObject = ExportedKotlinPackages.kotlinx.atomicfu.locks.SynchronizedObject
    @_spi(kotlinx$coroutines$InternalCoroutinesApi) @_documentation(visibility: internal)
    public protocol _AtomicOp_ReverseDispatch<T>: KotlinRuntime.KotlinBase {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        var atomicOp: ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp<Swift.Optional<KotlinRuntimeSupport.KotlinAny>> {
            get
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func complete(
            affected: T,
            failure: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func prepare(
            affected: T
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    }
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
    @_spi(kotlinx$coroutines$InternalCoroutinesApi) @_documentation(visibility: internal)
    public protocol _ThreadSafeHeap_ReverseDispatch<T>: KotlinRuntime.KotlinBase {
        associatedtype T: ExportedKotlinPackages.kotlinx.coroutines.`internal`.ThreadSafeHeapNode
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
    open class AtomicOp<T: KotlinRuntimeSupport._KotlinBridgeable>: ExportedKotlinPackages.kotlinx.coroutines.`internal`._AtomicOp_Base, KotlinRuntimeSupport._KotlinOpenGenericBridgeable, ExportedKotlinPackages.kotlinx.coroutines.`internal`._AtomicOp_ReverseDispatch {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open override var atomicOp: ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp<Swift.Optional<KotlinRuntimeSupport.KotlinAny>> {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                if Self.self == ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp<T>.self {
                    return ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__fromKotlinPointer(kotlinx_coroutines_internal_AtomicOp_atomicOp_get(self.__externalRCRef()))
                } else {
                    return ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__fromKotlinPointer(kotlinx_coroutines_internal_AtomicOp_atomicOp_get_direct(self.__externalRCRef()))
                }
            }
        }
        public var contravariant: KotlinRuntimeSupport.KotlinContravariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._noop)
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func complete(
            affected: T,
            failure: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp<T>.self {
                return { kotlinx_coroutines_internal_AtomicOp_complete__TypesOfArguments__T_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), affected.__retainedToKotlinPointer(), (failure).flatMap { it in it.__retainedToKotlinPointer() }); return () }()
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp.complete': a Swift subclass must override it and must not call super.")
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final override func perform(
            affected: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlinx_coroutines_internal_AtomicOp_perform__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), (affected).flatMap { it in it.__retainedToKotlinPointer() }) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func prepare(
            affected: T
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp<T>.self {
                return { switch kotlinx_coroutines_internal_AtomicOp_prepare__TypesOfArguments__T__(self.__externalRCRef(), affected.__retainedToKotlinPointer()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp.prepare': a Swift subclass must override it and must not call super.")
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public init() {
            precondition(Self.self != ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp<T>.self, "ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_internal_AtomicOp_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinContravariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi) @_documentation(visibility: internal)
    open class _AtomicOp_Base: ExportedKotlinPackages.kotlinx.coroutines.`internal`.OpDescriptor {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
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
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
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
        public override init() {
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
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class OpDescriptor: KotlinRuntime.KotlinBase {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open var atomicOp: ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>? {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                if Self.self == ExportedKotlinPackages.kotlinx.coroutines.`internal`.OpDescriptor.self {
                    return { switch kotlinx_coroutines_internal_OpDescriptor_atomicOp_get(self.__externalRCRef()) { case nil: .none; case let res?: ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>.__fromKotlinPointer(res); } }()
                } else {
                    fatalError("Cannot invoke the inherited implementation of abstract property 'ExportedKotlinPackages.kotlinx.coroutines.`internal`.OpDescriptor.atomicOp': a Swift subclass must override it and must not call super.")
                }
            }
        }
        open func perform(
            affected: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            if Self.self == ExportedKotlinPackages.kotlinx.coroutines.`internal`.OpDescriptor.self {
                return { switch kotlinx_coroutines_internal_OpDescriptor_perform__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), (affected).flatMap { it in it.__retainedToKotlinPointer() }) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
            } else {
                fatalError("Cannot invoke the inherited implementation of abstract member 'ExportedKotlinPackages.kotlinx.coroutines.`internal`.OpDescriptor.perform': a Swift subclass must override it and must not call super.")
            }
        }
        public override init() {
            precondition(Self.self != ExportedKotlinPackages.kotlinx.coroutines.`internal`.OpDescriptor.self, "ExportedKotlinPackages.kotlinx.coroutines.`internal`.OpDescriptor is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_internal_OpDescriptor_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    open class ThreadSafeHeap<T: ExportedKotlinPackages.kotlinx.coroutines.`internal`.ThreadSafeHeapNode>: ExportedKotlinPackages.kotlinx.coroutines.`internal`._ThreadSafeHeap_Base, KotlinRuntimeSupport._KotlinOpenGenericBridgeable, ExportedKotlinPackages.kotlinx.coroutines.`internal`._ThreadSafeHeap_ReverseDispatch {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final var isEmpty: Swift.Bool {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return kotlinx_coroutines_internal_ThreadSafeHeap_isEmpty_get(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final var size: Swift.Int32 {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return kotlinx_coroutines_internal_ThreadSafeHeap_size_get(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func addLast(
            node: T
        ) -> Swift.Void {
            return { kotlinx_coroutines_internal_ThreadSafeHeap_addLast__TypesOfArguments__T_ExportedKotlinPackages_kotlinx_coroutines_U60internalU60_ThreadSafeHeapNode__(self.__externalRCRef(), node.__retainedToKotlinPointer()); return () }()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func addLastIf(
            node: T,
            cond: @escaping (T?) -> Swift.Bool
        ) -> Swift.Bool {
            return kotlinx_coroutines_internal_ThreadSafeHeap_addLastIf__TypesOfArguments__T_ExportedKotlinPackages_kotlinx_coroutines_U60internalU60_ThreadSafeHeapNode_U28Swift_Optional_T_U29202D_U20Swift_Bool__(self.__externalRCRef(), node.__retainedToKotlinPointer(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Bool in return (cond)(Swift.Optional<T>.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Bool) as AnyObject).toOpaque())
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func find(
            predicate: @escaping (T) -> Swift.Bool
        ) -> T? {
            return Swift.Optional<T>.__fromKotlinPointer(kotlinx_coroutines_internal_ThreadSafeHeap_find__TypesOfArguments__U28TU29202D_U20Swift_Bool__(self.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Bool in return (predicate)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Bool) as AnyObject).toOpaque()))
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func peek() -> T? {
            return Swift.Optional<T>.__fromKotlinPointer(kotlinx_coroutines_internal_ThreadSafeHeap_peek(self.__externalRCRef()))
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func remove(
            node: T
        ) -> Swift.Bool {
            return kotlinx_coroutines_internal_ThreadSafeHeap_remove__TypesOfArguments__T_ExportedKotlinPackages_kotlinx_coroutines_U60internalU60_ThreadSafeHeapNode__(self.__externalRCRef(), node.__retainedToKotlinPointer())
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func removeFirstIf(
            predicate: @escaping (T) -> Swift.Bool
        ) -> T? {
            return Swift.Optional<T>.__fromKotlinPointer(kotlinx_coroutines_internal_ThreadSafeHeap_removeFirstIf__TypesOfArguments__U28TU29202D_U20Swift_Bool__(self.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Bool in return (predicate)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Bool) as AnyObject).toOpaque()))
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func removeFirstOrNull() -> T? {
            return Swift.Optional<T>.__fromKotlinPointer(kotlinx_coroutines_internal_ThreadSafeHeap_removeFirstOrNull(self.__externalRCRef()))
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public init() {
             let __kt: Swift.UnsafeMutableRawPointer!
             if Self.self == ExportedKotlinPackages.kotlinx.coroutines.`internal`.ThreadSafeHeap<T>.self {
                 __kt = kotlinx_coroutines_internal_ThreadSafeHeap_init_allocate()
             } else {
                 __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
             }
            super.init(__externalRCRefUnsafe: __kt, options: Self.self == ExportedKotlinPackages.kotlinx.coroutines.`internal`.ThreadSafeHeap<T>.self ? .asExistentialWrapper : .asBoundBridge);
            { kotlinx_coroutines_internal_ThreadSafeHeap_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi) @_documentation(visibility: internal)
    open class _ThreadSafeHeap_Base: ExportedKotlinPackages.kotlinx.atomicfu.locks.SynchronizedObject {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public static func synchronized<T: KotlinRuntimeSupport._KotlinBridgeable>(
        lock: ExportedKotlinPackages.kotlinx.coroutines.`internal`.SynchronizedObject,
        block: @escaping () -> T
    ) -> T {
        return T.__fromKotlinPointer(kotlinx_coroutines_internal_synchronized__TypesOfArguments__ExportedKotlinPackages_kotlinx_atomicfu_locks_SynchronizedObject_U2829202D_U20T__(lock.__externalRCRef(), Unmanaged.passRetained(({ let _res: T = (block)(); return _res.__retainedToKotlinPointer() } as () -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()))
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public static func synchronizedImpl<T: KotlinRuntimeSupport._KotlinBridgeable>(
        lock: ExportedKotlinPackages.kotlinx.coroutines.`internal`.SynchronizedObject,
        block: @escaping () -> T
    ) -> T {
        return T.__fromKotlinPointer(kotlinx_coroutines_internal_synchronizedImpl__TypesOfArguments__ExportedKotlinPackages_kotlinx_atomicfu_locks_SynchronizedObject_U2829202D_U20T__(lock.__externalRCRef(), Unmanaged.passRetained(({ let _res: T = (block)(); return _res.__retainedToKotlinPointer() } as () -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()))
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public static func resumeCancellableWith<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlin.coroutines.Continuation<T>,
        result: ExportedKotlinPackages.kotlin.Result<T>,
        onCancellation: ((ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void)?
    ) -> Swift.Void {
        return { kotlinx_coroutines_internal_resumeCancellableWith__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation_T__ExportedKotlinPackages_kotlin_Result_T__Swift_Optional_U28ExportedKotlinPackages_kotlin_ThrowableU29202D_U20Swift_Void___(receiver.__externalRCRef(), result.__externalRCRef(), onCancellation.map { it in Unmanaged.passRetained((it as (ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void) as AnyObject).toOpaque() } ?? nil); return () }()
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.intrinsics {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public static func startCoroutineCancellable<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: @escaping () async throws -> T,
        completion: any ExportedKotlinPackages.kotlin.coroutines.Continuation<T>
    ) -> Swift.Void {
        return { kotlinx_coroutines_intrinsics_startCoroutineCancellable__TypesOfArgumentsE__U282920asyncU20throwsU202D_U20T_anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation_T___(Unmanaged.passRetained(({ () async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: T = try await (receiver)(); return _res.__retainedToKotlinPointer() } as () async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque(), completion.__externalRCRef()); return () }()
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects {
    @frozen
    public enum SelectBuilder_SealedType<R: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntimeSupport.SealedType {
        case unknown(ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder_SealedType<R>.Unknown)
        public struct Unknown: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectBuilder<R>
            init(
                _ value: KotlinRuntime.KotlinBase
            ) {
                self.value = (value as? ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectBuilder<R>) ?? ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectBuilder<R>(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(value.__externalRCRef()), options: .asExistentialWrapper)
            }
        }
        public var value: ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectBuilder<R> {
            get {
                switch self {
                case let .unknown(type): type.value
                }
            }
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public enum SelectClause_SealedType: KotlinRuntimeSupport.SealedType {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        case selectClause0(ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0_SealedType)
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        case selectClause1(ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1_SealedType<KotlinRuntimeSupport.KotlinAny>)
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        case selectClause2(ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2_SealedType<KotlinRuntimeSupport.KotlinAny,KotlinRuntimeSupport.KotlinAny>)
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
    @_spi(kotlinx$coroutines$InternalCoroutinesApi) @frozen
    public enum SelectClause1_SealedType<Q: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntimeSupport.SealedType {
        case unknown(ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1_SealedType<Q>.Unknown)
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public struct Unknown: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectClause1<Q>
            init(
                _ value: KotlinRuntime.KotlinBase
            ) {
                self.value = (value as? ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectClause1<Q>) ?? ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectClause1<Q>(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(value.__externalRCRef()), options: .asExistentialWrapper)
            }
        }
        public var value: ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectClause1<Q> {
            get {
                switch self {
                case let .unknown(type): type.value
                }
            }
        }
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<Q> {
            get {
                return (_owner: self.value, _variance: KotlinRuntimeSupport._absurd)
            }
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi) @frozen
    public enum SelectClause2_SealedType<P: KotlinRuntimeSupport._KotlinBridgeable, Q: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntimeSupport.SealedType {
        case unknown(ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2_SealedType<P,Q>.Unknown)
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public struct Unknown: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectClause2<P,Q>
            init(
                _ value: KotlinRuntime.KotlinBase
            ) {
                self.value = (value as? ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectClause2<P,Q>) ?? ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectClause2<P,Q>(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(value.__externalRCRef()), options: .asExistentialWrapper)
            }
        }
        public var value: ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectClause2<P,Q> {
            get {
                switch self {
                case let .unknown(type): type.value
                }
            }
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi) @frozen
    public enum SelectInstance_SealedType<R: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntimeSupport.SealedType {
        case unknown(ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance_SealedType<R>.Unknown)
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public struct Unknown: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectInstance<R>
            init(
                _ value: KotlinRuntime.KotlinBase
            ) {
                self.value = (value as? ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectInstance<R>) ?? ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectInstance<R>(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(value.__externalRCRef()), options: .asExistentialWrapper)
            }
        }
        public var value: ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectInstance<R> {
            get {
                switch self {
                case let .unknown(type): type.value
                }
            }
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public typealias OnCancellationConstructor = (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) -> (ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public typealias ProcessResultFunction = (any KotlinRuntimeSupport._KotlinBridgeable, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public typealias RegistrationFunction = (any KotlinRuntimeSupport._KotlinBridgeable, any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance, (any KotlinRuntimeSupport._KotlinBridgeable)?) -> Swift.Void
    public protocol SelectBuilder<R>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.selects._SelectBuilder {
        associatedtype R: KotlinRuntimeSupport._KotlinBridgeable
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func invoke(
            _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0,
            block: @escaping () async throws -> R
        ) -> Swift.Void
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func invoke<Q: KotlinRuntimeSupport._KotlinBridgeable>(
            _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1<Q>,
            block: @escaping (Q) async throws -> R
        ) -> Swift.Void
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func invoke<P: KotlinRuntimeSupport._KotlinBridgeable, Q: KotlinRuntimeSupport._KotlinBridgeable>(
            _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2<P,Q>,
            param: P,
            block: @escaping (Q) async throws -> R
        ) -> Swift.Void
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func invoke<P: KotlinRuntimeSupport._KotlinBridgeable, Q: KotlinRuntimeSupport._KotlinBridgeable>(
            _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2<Swift.Optional<P>,Q>,
            block: @escaping (Q) async throws -> R
        ) -> Swift.Void
        func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder_SealedType<R>
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
        var onCancellationConstructor: ExportedKotlinPackages.kotlinx.coroutines.selects.OnCancellationConstructor? {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        var processResFunc: ExportedKotlinPackages.kotlinx.coroutines.selects.ProcessResultFunction {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        var regFunc: ExportedKotlinPackages.kotlinx.coroutines.selects.RegistrationFunction {
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
    public protocol SelectClause1<Q>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause, ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause1 {
        associatedtype Q: KotlinRuntimeSupport._KotlinBridgeable
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1_SealedType<Q>
        @_spi(kotlinx$coroutines$InternalCoroutinesApi) @_disfavoredOverload
        func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause_SealedType
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause1)
    public protocol _SelectClause1: ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol __SelectClause1: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectClause {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol SelectClause2<P, Q>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause, ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause2 {
        associatedtype P: KotlinRuntimeSupport._KotlinBridgeable
        associatedtype Q: KotlinRuntimeSupport._KotlinBridgeable
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2_SealedType<P,Q>
        @_spi(kotlinx$coroutines$InternalCoroutinesApi) @_disfavoredOverload
        func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause_SealedType
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause2)
    public protocol _SelectClause2: ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol __SelectClause2: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectClause {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol SelectInstance<R>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.selects._SelectInstance {
        associatedtype R: KotlinRuntimeSupport._KotlinBridgeable
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
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance_SealedType<R>
    }
    @objc(_ExportedKotlinPackages_kotlinx_coroutines_selects_SelectInstance)
    public protocol _SelectInstance {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol __SelectInstance: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public final class AnySelectBuilder<R: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder, ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectBuilder, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var contravariant: KotlinRuntimeSupport.KotlinContravariant<R> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._noop)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinContravariant<R>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public final class AnySelectClause1<Q: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1, ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectClause1, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<Q> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<Q>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public final class AnySelectClause2<P: KotlinRuntimeSupport._KotlinBridgeable, Q: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2, ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectClause2, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public final class AnySelectInstance<R: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance, ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectInstance, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        public var contravariant: KotlinRuntimeSupport.KotlinContravariant<R> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._noop)
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinContravariant<R>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
    }
    public static func select<R: KotlinRuntimeSupport._KotlinBridgeable>(
        builder: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder<R>) -> Swift.Void
    ) async throws -> R {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_selects_select__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectBuilder_R_U29202D_U20Swift_Void__(Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (builder)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder<R>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectBuilder<R>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(R.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func selectUnbiased<R: KotlinRuntimeSupport._KotlinBridgeable>(
        builder: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder<R>) -> Swift.Void
    ) async throws -> R {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_selects_selectUnbiased__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectBuilder_R_U29202D_U20Swift_Void__(Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (builder)({ let _ptr = _p0!; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder<R>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectBuilder<R>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(R.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func whileSelect(
        builder: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder<Swift.Bool>) -> Swift.Void
    ) async throws -> Swift.Void {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_selects_whileSelect__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectBuilder_Swift_Bool_U29202D_U20Swift_Void__(Unmanaged.passRetained((builder as (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder<Swift.Bool>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func onTimeout<R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder<R>,
        timeMillis: Swift.Int64,
        block: @escaping () async throws -> R
    ) -> Swift.Void {
        return { kotlinx_coroutines_selects_onTimeout__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectBuilder_R__Swift_Int64_U282920asyncU20throwsU202D_U20R__(receiver.__externalRCRef(), timeMillis, Unmanaged.passRetained(({ () async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = try await (block)(); return _res.__retainedToKotlinPointer() } as () async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()); return () }()
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func onTimeout<R: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder<R>,
        timeout: ExportedKotlinPackages.kotlin.time.Duration,
        block: @escaping () async throws -> R
    ) -> Swift.Void {
        return { kotlinx_coroutines_selects_onTimeout__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectBuilder_R__ExportedKotlinPackages_kotlin_time_Duration_U282920asyncU20throwsU202D_U20R__(receiver.__externalRCRef(), timeout.__externalRCRef(), Unmanaged.passRetained(({ () async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = try await (block)(); return _res.__retainedToKotlinPointer() } as () async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()); return () }()
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.sync {
    public protocol Mutex: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.sync._Mutex {
        var isLocked: Swift.Bool {
            get
        }
        @available(*, deprecated, message: "Mutex.onLock deprecated without replacement. For additional details please refer to #2794") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        var onLock: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2<Swift.Optional<KotlinRuntimeSupport.KotlinAny>,KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines.sync._Mutex>> {
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
    public static func withLock<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex,
        owner: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        action: @escaping () -> T
    ) async throws -> T {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_sync_withLock__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_sync_Mutex_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U2829202D_U20T__(receiver.__externalRCRef(), (owner).flatMap { it in it.__retainedToKotlinPointer() }, Unmanaged.passRetained(({ let _res: T = (action)(); return _res.__retainedToKotlinPointer() } as () -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public static func withPermit<T: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.sync.Semaphore,
        action: @escaping () -> T
    ) async throws -> T {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_sync_withPermit__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_sync_Semaphore_U2829202D_U20T__(receiver.__externalRCRef(), Unmanaged.passRetained(({ let _res: T = (action)(); return _res.__retainedToKotlinPointer() } as () -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
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
extension ExportedKotlinPackages.kotlin.collections.Collection where E == KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines._Job> {
    public func joinAll() async throws -> Swift.Void {
        let receiver: any ExportedKotlinPackages.kotlin.collections.Collection<KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines._Job>> = self
        return try await ExportedKotlinPackages.kotlinx.coroutines.joinAll(receiver)
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope {
    public func async<T: KotlinRuntimeSupport._KotlinBridgeable>(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        start: ExportedKotlinPackages.kotlinx.coroutines.CoroutineStart,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> T
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.Deferred<T> {
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
        result: ExportedKotlinPackages.kotlin.Result<T>
    ) -> Swift.Bool {
        let receiver: any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred<T> = self
        return ExportedKotlinPackages.kotlinx.coroutines.completeWith(receiver, result: result)
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func disposeOnCancellation(
        handle: any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
    ) -> Swift.Void {
        let receiver: any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation = self
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
    public func invoke<T: KotlinRuntimeSupport._KotlinBridgeable>(
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> T
    ) async throws -> T {
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
        return { kotlinx_coroutines_CancellableContinuation_completeResume__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(self.__externalRCRef(), (token).__retainedToKotlinPointer()!); return () }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func initCancellability() -> Swift.Void {
        return { kotlinx_coroutines_CancellableContinuation_initCancellability(self.__externalRCRef()); return () }()
    }
    public func invokeOnCancellation(
        handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
    ) -> Swift.Void {
        return { kotlinx_coroutines_CancellableContinuation_invokeOnCancellation__TypesOfArguments__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), Unmanaged.passRetained((handler as (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void) as AnyObject).toOpaque()); return () }()
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func resume(
        value: T,
        onCancellation: ((ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void)?
    ) -> Swift.Void {
        return { kotlinx_coroutines_CancellableContinuation_resume__TypesOfArguments__T_Swift_Optional_U28ExportedKotlinPackages_kotlin_ThrowableU29202D_U20Swift_Void___(self.__externalRCRef(), value.__retainedToKotlinPointer(), onCancellation.map { it in Unmanaged.passRetained((it as (ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void) as AnyObject).toOpaque() } ?? nil); return () }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func tryResume(
        value: T,
        idempotent: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_CancellableContinuation_tryResume__TypesOfArguments__T_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), value.__retainedToKotlinPointer(), (idempotent).flatMap { it in it.__retainedToKotlinPointer() }) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func tryResume(
        value: T,
        idempotent: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        onCancellation: ((ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_CancellableContinuation_tryResume__TypesOfArguments__T_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_U28ExportedKotlinPackages_kotlin_ThrowableU29202D_U20Swift_Void___(self.__externalRCRef(), value.__retainedToKotlinPointer(), (idempotent).flatMap { it in it.__retainedToKotlinPointer() }, onCancellation.map { it in Unmanaged.passRetained((it as (ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void) as AnyObject).toOpaque() } ?? nil) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
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
        value: T
    ) -> Swift.Void {
        return { kotlinx_coroutines_CancellableContinuation_resumeUndispatched__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_CoroutineDispatcher_T__(self.__externalRCRef(), receiver.__externalRCRef(), value.__retainedToKotlinPointer()); return () }()
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
extension ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation {
    @inlinable
    public var contravariant: KotlinRuntimeSupport.KotlinContravariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._noop)
        }
    }
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
        value: T,
        onCancellation: ((ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void)?
    ) -> Swift.Void {
        fatalError("'resume' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func tryResume(
        value: T,
        idempotent: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        fatalError("'tryResume' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func tryResume(
        value: T,
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
        value: T
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
        value: T
    ) -> Swift.Bool {
        return kotlinx_coroutines_CompletableDeferred_complete__TypesOfArguments__T__(self.__externalRCRef(), value.__retainedToKotlinPointer())
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
extension ExportedKotlinPackages.kotlinx.coroutines.CopyableThrowable where Self : ExportedKotlinPackages.kotlinx.coroutines.__CopyableThrowable {
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func createCopy() -> T? {
        return Swift.Optional<T>.__fromKotlinPointer(kotlinx_coroutines_CopyableThrowable_createCopy(self.__externalRCRef()))
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines._CopyableThrowable {
}
extension ExportedKotlinPackages.kotlinx.coroutines.CopyableThrowable {
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
    public var onAwait: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1<T> {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return { let _ptr = kotlinx_coroutines_Deferred_onAwait_get(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectClause1<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
        }
    }
    public func `await`() async throws -> T {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_Deferred_await(self.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(T.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func getCompleted() -> T {
        return T.__fromKotlinPointer(kotlinx_coroutines_Deferred_getCompleted(self.__externalRCRef()))
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func getCompletionExceptionOrNull() -> ExportedKotlinPackages.kotlin.Throwable? {
        return { switch kotlinx_coroutines_Deferred_getCompletionExceptionOrNull(self.__externalRCRef()) { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines._Deferred {
}
extension ExportedKotlinPackages.kotlinx.coroutines.Deferred {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var onAwait: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1<T> {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            fatalError("'onAwait' is an @_spi requirement that must be implemented by Swift conformers")
        }
    }
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func getCompleted() -> T {
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
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func scheduleResumeAfterDelay(
        timeMillis: Swift.Int64,
        continuation: any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation<KotlinRuntimeSupport.KotlinUnit>
    ) -> Swift.Void {
        return { kotlinx_coroutines_Delay_scheduleResumeAfterDelay__TypesOfArguments__Swift_Int64_anyU20ExportedKotlinPackages_kotlinx_coroutines_CancellableContinuation_KotlinRuntimeSupport_KotlinUnit___(self.__externalRCRef(), timeMillis, continuation.__externalRCRef()); return () }()
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
    public var children: any ExportedKotlinPackages.kotlin.sequences.Sequence<KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines._Job>> {
        get {
            return { let _ptr = kotlinx_coroutines_Job_children_get(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.sequences.Sequence<KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines._Job>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.sequences.AnySequence<KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines._Job>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
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
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_Job_invokeOnCompletion__TypesOfArguments__Swift_Bool_Swift_Bool_U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), onCancelling, invokeImmediately, Unmanaged.passRetained((handler as (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
    }
    public func invokeOnCompletion(
        handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_Job_invokeOnCompletion__TypesOfArguments__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), Unmanaged.passRetained((handler as (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void) as AnyObject).toOpaque()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
    }
    public func join() async throws -> Swift.Void {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_Job_join(self.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
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
    public func openSubscription() -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<E> {
        return { let _ptr = kotlinx_coroutines_channels_BroadcastChannel_openSubscription(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.channels.AnyReceiveChannel<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.channels._BroadcastChannel {
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.channels.Channel where Self : ExportedKotlinPackages.kotlinx.coroutines.channels.__Channel {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.channels._Channel {
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.Channel {
    public typealias Factory = KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_channels_Channel_Factory
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator where Self : ExportedKotlinPackages.kotlinx.coroutines.channels.__ChannelIterator {
    public func hasNext() async throws -> Swift.Bool {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_channels_ChannelIterator_hasNext(self.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Bool) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public func next() -> E {
        return E.__fromKotlinPointer(kotlinx_coroutines_channels_ChannelIterator_next(self.__externalRCRef()))
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.channels._ChannelIterator {
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<E> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope where Self : ExportedKotlinPackages.kotlinx.coroutines.channels.__ProducerScope {
    public var channel: any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel<E> {
        get {
            return { let _ptr = kotlinx_coroutines_channels_ProducerScope_channel_get(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.channels.AnySendChannel<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.channels._ProducerScope {
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope {
    @inlinable
    public var contravariant: KotlinRuntimeSupport.KotlinContravariant<E> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._noop)
        }
    }
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
    public var onReceive: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1<E> {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return { let _ptr = kotlinx_coroutines_channels_ReceiveChannel_onReceive_get(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectClause1<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var onReceiveCatching: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1<ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<E>> {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return { let _ptr = kotlinx_coroutines_channels_ReceiveChannel_onReceiveCatching_get(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1<ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<E>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectClause1<ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<E>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
        }
    }
    public func cancel(
        cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
    ) -> Swift.Void {
        return { kotlinx_coroutines_channels_ReceiveChannel_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException___(self.__externalRCRef(), cause.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    public func iterator() -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator<E> {
        return { let _ptr = kotlinx_coroutines_channels_ReceiveChannel_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.channels.AnyChannelIterator<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public func receive() async throws -> E {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_channels_ReceiveChannel_receive(self.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(E.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public func receiveCatching() async throws -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<E> {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_channels_ReceiveChannel_receiveCatching(self.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void in return (continuation)(ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<E>.__fromKotlinPointer(_p0)) } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public func tryReceive() -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<E> {
        return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<E>.__fromKotlinPointer(kotlinx_coroutines_channels_ReceiveChannel_tryReceive(self.__externalRCRef()))
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.channels._ReceiveChannel {
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
    public var onReceive: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1<E> {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            fatalError("'onReceive' is an @_spi requirement that must be implemented by Swift conformers")
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var onReceiveCatching: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1<ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<E>> {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            fatalError("'onReceiveCatching' is an @_spi requirement that must be implemented by Swift conformers")
        }
    }
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<E> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
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
    public var onSend: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2<E,ExportedKotlinPackages.kotlinx.coroutines.channels.AnySendChannel<E>> {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return { let _ptr = kotlinx_coroutines_channels_SendChannel_onSend_get(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2<E,ExportedKotlinPackages.kotlinx.coroutines.channels.AnySendChannel<E>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectClause2<E,ExportedKotlinPackages.kotlinx.coroutines.channels.AnySendChannel<E>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
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
        return { kotlinx_coroutines_channels_SendChannel_invokeOnClose__TypesOfArguments__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), Unmanaged.passRetained((handler as (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void) as AnyObject).toOpaque()); return () }()
    }
    public func send(
        element: E
    ) async throws -> Swift.Void {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_channels_SendChannel_send__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public func trySend(
        element: E
    ) -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<KotlinRuntimeSupport.KotlinUnit> {
        return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<KotlinRuntimeSupport.KotlinUnit>.__fromKotlinPointer(kotlinx_coroutines_channels_SendChannel_trySend__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer()))
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.channels._SendChannel {
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
    public var onSend: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2<E,ExportedKotlinPackages.kotlinx.coroutines.channels.AnySendChannel<E>> {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            fatalError("'onSend' is an @_spi requirement that must be implemented by Swift conformers")
        }
    }
    @inlinable
    public var contravariant: KotlinRuntimeSupport.KotlinContravariant<E> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._noop)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.flow.Flow where Self : ExportedKotlinPackages.kotlinx.coroutines.flow.__Flow {
    public func collect(
        collector: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>
    ) async throws -> Swift.Void {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_Flow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_Swift_Optional_KotlinRuntimeSupport_KotlinAny____(self.__externalRCRef(), collector.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
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
        value: T
    ) async throws -> Swift.Void {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_FlowCollector_emit__TypesOfArguments__T__(self.__externalRCRef(), value.__retainedToKotlinPointer(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.flow._FlowCollector {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector {
    @inlinable
    public var contravariant: KotlinRuntimeSupport.KotlinContravariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._noop)
        }
    }
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
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_MutableSharedFlow_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), (value).flatMap { it in it.__retainedToKotlinPointer() }, Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func resetReplayCache() -> Swift.Void {
        return { kotlinx_coroutines_flow_MutableSharedFlow_resetReplayCache(self.__externalRCRef()); return () }()
    }
    public func tryEmit(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlinx_coroutines_flow_MutableSharedFlow_tryEmit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), (value).flatMap { it in it.__retainedToKotlinPointer() })
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
            return { kotlinx_coroutines_flow_MutableStateFlow_value_set__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), (newValue).flatMap { it in it.__retainedToKotlinPointer() }); return () }()
        }
    }
    public func compareAndSet(
        expect: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        update: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlinx_coroutines_flow_MutableStateFlow_compareAndSet__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), (expect).flatMap { it in it.__retainedToKotlinPointer() }, (update).flatMap { it in it.__retainedToKotlinPointer() })
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
        collector: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>
    ) async throws -> Swift.Never {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_flow_SharedFlow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_Swift_Optional_KotlinRuntimeSupport_KotlinAny____(self.__externalRCRef(), collector.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Never) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
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
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<T> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<T>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_internal_FusibleFlow_fuse__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow__(self.__externalRCRef(), context.__externalRCRef(), capacity, onBufferOverflow.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, T.Type.self)
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`._FusibleFlow {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.FusibleFlow {
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
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder where Self : ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectBuilder {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invoke(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0,
        block: @escaping () async throws -> R
    ) -> Swift.Void {
        return { kotlinx_coroutines_selects_SelectBuilder_invoke__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause0_U282920asyncU20throwsU202D_U20R__(self.__externalRCRef(), receiver.__externalRCRef(), Unmanaged.passRetained(({ () async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = try await (block)(); return _res.__retainedToKotlinPointer() } as () async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()); return () }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invoke<Q: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1<Q>,
        block: @escaping (Q) async throws -> R
    ) -> Swift.Void {
        return { kotlinx_coroutines_selects_SelectBuilder_invoke__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause1_Q__U28QU2920asyncU20throwsU202D_U20R__(self.__externalRCRef(), receiver.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = try await (block)(Q.__fromKotlinPointer(_p0)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()); return () }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invoke<P: KotlinRuntimeSupport._KotlinBridgeable, Q: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2<P,Q>,
        param: P,
        block: @escaping (Q) async throws -> R
    ) -> Swift.Void {
        return { kotlinx_coroutines_selects_SelectBuilder_invoke__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause2_P_Q__P_U28QU2920asyncU20throwsU202D_U20R__(self.__externalRCRef(), receiver.__externalRCRef(), param.__retainedToKotlinPointer(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = try await (block)(Q.__fromKotlinPointer(_p0)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()); return () }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invoke<P: KotlinRuntimeSupport._KotlinBridgeable, Q: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2<Swift.Optional<P>,Q>,
        block: @escaping (Q) async throws -> R
    ) -> Swift.Void {
        return { kotlinx_coroutines_selects_SelectBuilder_invoke__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause2_Swift_Optional_P__Q__U28QU2920asyncU20throwsU202D_U20R__(self.__externalRCRef(), receiver.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = try await (block)(Q.__fromKotlinPointer(_p0)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()); return () }()
    }
    public func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder_SealedType<R> {
        switch 0 {
        default: .unknown(.init(self))
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.selects._SelectBuilder {
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder {
    @inlinable
    public var contravariant: KotlinRuntimeSupport.KotlinContravariant<R> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._noop)
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invoke(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0,
        block: @escaping () async throws -> R
    ) -> Swift.Void {
        fatalError("'invoke' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invoke<Q: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1<Q>,
        block: @escaping (Q) async throws -> R
    ) -> Swift.Void {
        fatalError("'invoke' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invoke<P: KotlinRuntimeSupport._KotlinBridgeable, Q: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2<P,Q>,
        param: P,
        block: @escaping (Q) async throws -> R
    ) -> Swift.Void {
        fatalError("'invoke' is an @_spi requirement that must be implemented by Swift conformers")
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invoke<P: KotlinRuntimeSupport._KotlinBridgeable, Q: KotlinRuntimeSupport._KotlinBridgeable>(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2<Swift.Optional<P>,Q>,
        block: @escaping (Q) async throws -> R
    ) -> Swift.Void {
        return { kotlinx_coroutines_selects_SelectBuilder_invoke__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause2_Swift_Optional_P__Q__U28QU2920asyncU20throwsU202D_U20R___direct(self.__externalRCRef(), receiver.__externalRCRef(), Unmanaged.passRetained(({ (_p0: Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer> in let _res: R = try await (block)(Q.__fromKotlinPointer(_p0)); return _res.__retainedToKotlinPointer() } as (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>) as AnyObject).toOpaque()); return () }()
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
    public var onCancellationConstructor: ExportedKotlinPackages.kotlinx.coroutines.selects.OnCancellationConstructor? {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return kotlinx_coroutines_selects_SelectClause_onCancellationConstructor_get(self.__externalRCRef()).map { it in {
                let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: it, options: .asBestFittingWrapper)
                return { _1, _2, _3 in return {
                let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: KotlinxCoroutinesCore_internal_functional_type_caller_U28ExportedKotlinPackagesU2EkotlinU2EThrowableU29202D3E20SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectInstance_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef(), _1.__externalRCRef(), (_2).flatMap { it in it.__retainedToKotlinPointer() }, (_3).flatMap { it in it.__retainedToKotlinPointer() }), options: .asBestFittingWrapper)
                return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlin_Throwable__(pointerToBlock.__externalRCRef(), _1.__externalRCRef()); return () }() }
            }() }
            }() }
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var processResFunc: ExportedKotlinPackages.kotlinx.coroutines.selects.ProcessResultFunction {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return {
                let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: kotlinx_coroutines_selects_SelectClause_processResFunc_get(self.__externalRCRef()), options: .asBestFittingWrapper)
                return { _1, _2, _3 in return { switch KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EOptionalU3CanyU20KotlinRuntimeSupportU2E_KotlinBridgeableU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinRuntimeSupport__KotlinBridgeable_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef(), (_1).__retainedToKotlinPointer()!, (_2).flatMap { it in it.__retainedToKotlinPointer() }, (_3).flatMap { it in it.__retainedToKotlinPointer() }) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }() }
            }()
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var regFunc: ExportedKotlinPackages.kotlinx.coroutines.selects.RegistrationFunction {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return {
                let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: kotlinx_coroutines_selects_SelectClause_regFunc_get(self.__externalRCRef()), options: .asBestFittingWrapper)
                return { _1, _2, _3 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinRuntimeSupport__KotlinBridgeable_anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectInstance_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef(), (_1).__retainedToKotlinPointer()!, _2.__externalRCRef(), (_3).flatMap { it in it.__retainedToKotlinPointer() }); return () }() }
            }()
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause_SealedType {
        switch kotlinx_coroutines_selects_SelectClause_sealedType_tag(self.__externalRCRef()) {
        case 0: .selectClause0(KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause0>(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(self.__externalRCRef()), options: .asExistentialWrapper).sealedType())
        case 1: .selectClause1(ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectClause1<KotlinRuntimeSupport.KotlinAny>.__fromKotlinPointer(KotlinRuntimeSupport._cloneBridgeableRef(self.__externalRCRef())).sealedType())
        case 2: .selectClause2(ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectClause2<KotlinRuntimeSupport.KotlinAny,KotlinRuntimeSupport.KotlinAny>.__fromKotlinPointer(KotlinRuntimeSupport._cloneBridgeableRef(self.__externalRCRef())).sealedType())
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
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1_SealedType<Q> {
        switch 0 {
        default: .unknown(.init(self))
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause1 {
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1 {
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<Q> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi) @_disfavoredOverload
    public func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause_SealedType {
        .selectClause1(ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectClause1<KotlinRuntimeSupport.KotlinAny>.__fromKotlinPointer(KotlinRuntimeSupport._cloneBridgeableRef(self.__externalRCRef())).sealedType())
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2 where Self : ExportedKotlinPackages.kotlinx.coroutines.selects.__SelectClause2 {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2_SealedType<P,Q> {
        switch 0 {
        default: .unknown(.init(self))
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause2 {
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2 {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi) @_disfavoredOverload
    public func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause_SealedType {
        .selectClause2(ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectClause2<KotlinRuntimeSupport.KotlinAny,KotlinRuntimeSupport.KotlinAny>.__fromKotlinPointer(KotlinRuntimeSupport._cloneBridgeableRef(self.__externalRCRef())).sealedType())
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
        return { kotlinx_coroutines_selects_SelectInstance_selectInRegistrationPhase__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), (internalResult).flatMap { it in it.__retainedToKotlinPointer() }); return () }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func trySelect(
        clauseObject: any KotlinRuntimeSupport._KotlinBridgeable,
        result: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlinx_coroutines_selects_SelectInstance_trySelect__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), (clauseObject).__retainedToKotlinPointer()!, (result).flatMap { it in it.__retainedToKotlinPointer() })
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func sealedType() -> ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance_SealedType<R> {
        switch 0 {
        default: .unknown(.init(self))
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.selects._SelectInstance {
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance {
    public var contravariant: KotlinRuntimeSupport.KotlinContravariant<R> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._noop)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex where Self : ExportedKotlinPackages.kotlinx.coroutines.sync.__Mutex {
    public var isLocked: Swift.Bool {
        get {
            return kotlinx_coroutines_sync_Mutex_isLocked_get(self.__externalRCRef())
        }
    }
    @available(*, deprecated, message: "Mutex.onLock deprecated without replacement. For additional details please refer to #2794") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var onLock: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2<Swift.Optional<KotlinRuntimeSupport.KotlinAny>,KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines.sync._Mutex>> {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return { let _ptr = kotlinx_coroutines_sync_Mutex_onLock_get(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2<Swift.Optional<KotlinRuntimeSupport.KotlinAny>,KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines.sync._Mutex>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectClause2<Swift.Optional<KotlinRuntimeSupport.KotlinAny>,KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines.sync._Mutex>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
        }
    }
    public func holdsLock(
        owner: any KotlinRuntimeSupport._KotlinBridgeable
    ) -> Swift.Bool {
        return kotlinx_coroutines_sync_Mutex_holdsLock__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(self.__externalRCRef(), (owner).__retainedToKotlinPointer()!)
    }
    public func lock(
        owner: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> Swift.Void {
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_sync_Mutex_lock__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), (owner).flatMap { it in it.__retainedToKotlinPointer() }, Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
        }
    }
    public func tryLock(
        owner: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlinx_coroutines_sync_Mutex_tryLock__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), (owner).flatMap { it in it.__retainedToKotlinPointer() })
    }
    public func unlock(
        owner: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return { kotlinx_coroutines_sync_Mutex_unlock__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), (owner).flatMap { it in it.__retainedToKotlinPointer() }); return () }()
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
    public var onLock: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2<Swift.Optional<KotlinRuntimeSupport.KotlinAny>,KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines.sync._Mutex>> {
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
        return try await withKotlinContinuation { continuation, exception, cancellation in
            let _: Bool = kotlinx_coroutines_sync_Semaphore_acquire(self.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Void) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
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
@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_ExportedKotlinPackagesU2EkotlinU2EtimeU2EDuration__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_ExportedKotlinPackagesU2EkotlinU2EtimeU2EDuration__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer {
    let _result: ExportedKotlinPackages.kotlin.time.Duration = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> ExportedKotlinPackages.kotlin.time.Duration)(_1)
    return _result.__externalRCRef()
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EBool__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EBool__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?, _ _2: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _result: Swift.Bool = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Bool)(_1, _2)
    return _result
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EBool__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EBool__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _result: Swift.Bool = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Bool)(_1)
    return _result
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EInt64__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EInt64__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?) -> Swift.Int64 {
    let _result: Swift.Int64 = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Int64)(_1)
    return _result
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _result: Swift.Optional<Swift.UnsafeMutableRawPointer> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Optional<Swift.UnsafeMutableRawPointer>)({ switch _1 { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }())
    return _result
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _result: Swift.Optional<Swift.UnsafeMutableRawPointer> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Optional<Swift.UnsafeMutableRawPointer>)(_1)
    return _result
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
package func KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__(_ pointerToClosure: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _result: Swift.Optional<Swift.UnsafeMutableRawPointer> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! () -> Swift.Optional<Swift.UnsafeMutableRawPointer>)()
    return _result
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EOptionalU3CanyU20KotlinRuntimeSupportU2E_KotlinBridgeableU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinRuntimeSupport__KotlinBridgeable_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EOptionalU3CanyU20KotlinRuntimeSupportU2E_KotlinBridgeableU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinRuntimeSupport__KotlinBridgeable_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer, _ _2: Swift.UnsafeMutableRawPointer?, _ _3: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (any KotlinRuntimeSupport._KotlinBridgeable, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>)(KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: _1), { switch _2 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }(), { switch _3 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return (_result).flatMap { it in it.__retainedToKotlinPointer() }
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlin_Throwable__")
package func KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlin_Throwable__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void)(ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: _1))
    return { _result; return true }()
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__")
package func KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.Bool) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Bool) -> Swift.Void)(_1)
    return { _result; return true }()
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__")
package func KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.Int32) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Int32) -> Swift.Void)(_1)
    return { _result; return true }()
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) -> Swift.Void)({ switch _1 { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }())
    return { _result; return true }()
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.Error>) -> Swift.Void)({ switch _1 { case nil: .none; case let res?: KotlinRuntimeSupport.swiftError(fromKotlinThrowable: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res)); } }())
    return { _result; return true }()
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void)(_1)
    return { _result; return true }()
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__")
package func KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.Bool) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Void) -> Swift.Void)({ _1; return () }())
    return { _result; return true }()
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
package func KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer__(_ pointerToClosure: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! () -> Swift.Void)()
    return { _result; return true }()
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ExportedKotlinPackages_kotlin_Throwable__")
package func KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ExportedKotlinPackages_kotlin_Throwable__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer, _ _2: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext, ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void)(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: _1, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext, ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: _2))
    return { _result; return true }()
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__")
package func KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext) -> Swift.Void)(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: _1, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext)
    return { _result; return true }()
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectBuilder_Swift_Bool___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectBuilder_Swift_Bool___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder<Swift.Bool>) -> Swift.Void)({ let _ptr = _1; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder<Swift.Bool>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectBuilder<Swift.Bool>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
    return { _result; return true }()
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinRuntimeSupport__KotlinBridgeable_anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectInstance_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinRuntimeSupport__KotlinBridgeable_anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectInstance_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer, _ _2: Swift.UnsafeMutableRawPointer, _ _3: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _result: Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (any KotlinRuntimeSupport._KotlinBridgeable, any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void)(KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: _1), { let _ptr = _2; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectInstance<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }(), { switch _3 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return { _result; return true }()
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_U28ExportedKotlinPackagesU2EkotlinU2EThrowableU29202D3E20SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectInstance_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_U28ExportedKotlinPackagesU2EkotlinU2EThrowableU29202D3E20SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectInstance_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer, _ _2: Swift.UnsafeMutableRawPointer?, _ _3: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer {
    let _result: (ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> (ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void)({ let _ptr = _1; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectInstance<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }(), { switch _2 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }(), { switch _3 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return Unmanaged.passRetained((_result as (ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void) as AnyObject).toOpaque()
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_anyU20KotlinCoroutineSupportU2EKotlinTypedFlowU3CExportedKotlinPackagesU2EkotlinxU2EcoroutinesU2EflowU2ESharingCommandU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinCoroutineSupport_KotlinTypedStateFlow_Swift_Int32___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_anyU20KotlinCoroutineSupportU2EKotlinTypedFlowU3CExportedKotlinPackagesU2EkotlinxU2EcoroutinesU2EflowU2ESharingCommandU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinCoroutineSupport_KotlinTypedStateFlow_Swift_Int32___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _result: any KotlinCoroutineSupport.KotlinTypedFlow<ExportedKotlinPackages.kotlinx.coroutines.flow.SharingCommand> = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Int32>) -> any KotlinCoroutineSupport.KotlinTypedFlow<ExportedKotlinPackages.kotlinx.coroutines.flow.SharingCommand>)(KotlinCoroutineSupport._KotlinTypedStateFlowImpl<Swift.Int32>.create(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: _1, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow, Swift.Int32.Type.self))
    return _result.wrapped.__externalRCRef()
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EBool__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlin_Throwable__")
package func KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EBool__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlin_Throwable__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Bool) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef(), _1); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Bool)(ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: _1))
    }
    return true
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EBool__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__ExportedKotlinPackages_kotlin_Throwable_Swift_Int64__")
package func KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EBool__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__ExportedKotlinPackages_kotlin_Throwable_Swift_Int64__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?, _ _2: Swift.UnsafeMutableRawPointer, _ _3: Swift.Int64, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Bool) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef(), _1); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>, ExportedKotlinPackages.kotlin.Throwable, Swift.Int64) async throws -> Swift.Bool)(_1, ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: _2), _3)
    }
    return true
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EBool__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EBool__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?, _ _2: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Bool) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef(), _1); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Bool)(_1, _2)
    }
    return true
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EBool__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EBool__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Bool) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef(), _1); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Bool)(_1)
    }
    return true
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?, _ _2: Swift.UnsafeMutableRawPointer?, _ _3: Swift.UnsafeMutableRawPointer?, _ _4: Swift.UnsafeMutableRawPointer?, _ _5: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(pointerToBlock.__externalRCRef(), _1); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>)(_1, _2, _3, _4, _5)
    }
    return true
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?, _ _2: Swift.UnsafeMutableRawPointer?, _ _3: Swift.UnsafeMutableRawPointer?, _ _4: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(pointerToBlock.__externalRCRef(), _1); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>)(_1, _2, _3, _4)
    }
    return true
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?, _ _2: Swift.UnsafeMutableRawPointer?, _ _3: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(pointerToBlock.__externalRCRef(), _1); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>)(_1, _2, _3)
    }
    return true
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?, _ _2: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(pointerToBlock.__externalRCRef(), _1); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>)(_1, _2)
    }
    return true
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(pointerToBlock.__externalRCRef(), _1); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>)(_1)
    }
    return true
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
package func KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(pointerToBlock.__externalRCRef(), _1); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! () async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>)()
    }
    return true
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope__")
package func KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(pointerToBlock.__externalRCRef(), _1); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Optional<Swift.UnsafeMutableRawPointer>)(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: _1, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope)
    }
    return true
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.Int32, _ _2: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef(), { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Int32, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void)(_1, _2)
    }
    return true
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__ExportedKotlinPackages_kotlin_Throwable__")
package func KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__ExportedKotlinPackages_kotlin_Throwable__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?, _ _2: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef(), { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>, ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Void)(_1, ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: _2))
    }
    return true
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?, _ _2: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef(), { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<ExportedKotlinPackages.kotlin.Throwable>) async throws -> Swift.Void)(_1, { switch _2 { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }())
    }
    return true
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?, _ _2: Swift.UnsafeMutableRawPointer?, _ _3: Swift.UnsafeMutableRawPointer?, _ _4: Swift.UnsafeMutableRawPointer?, _ _5: Swift.UnsafeMutableRawPointer?, _ _6: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef(), { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void)(_1, _2, _3, _4, _5, _6)
    }
    return true
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?, _ _2: Swift.UnsafeMutableRawPointer?, _ _3: Swift.UnsafeMutableRawPointer?, _ _4: Swift.UnsafeMutableRawPointer?, _ _5: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef(), { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void)(_1, _2, _3, _4, _5)
    }
    return true
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?, _ _2: Swift.UnsafeMutableRawPointer?, _ _3: Swift.UnsafeMutableRawPointer?, _ _4: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef(), { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void)(_1, _2, _3, _4)
    }
    return true
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?, _ _2: Swift.UnsafeMutableRawPointer?, _ _3: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef(), { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void)(_1, _2, _3)
    }
    return true
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer__Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?, _ _2: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef(), { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>, Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void)(_1, _2)
    }
    return true
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___")
package func KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef(), { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Optional<Swift.UnsafeMutableRawPointer>) async throws -> Swift.Void)(_1)
    }
    return true
}

@_cdecl("KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope__")
package func KotlinxCoroutinesCore_internal_functional_type_callee_async_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef(), { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Void)(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: _1, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope)
    }
    return true
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_____reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ cause: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.cancel(cause: { switch cause { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_completeResume__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable____reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_completeResume__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ token: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.completeResume(token: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: token))
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_initCancellability__reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_initCancellability__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.initCancellability()
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_invokeOnCancellation__TypesOfArguments__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void____reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_invokeOnCancellation__TypesOfArguments__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ handler: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.invokeOnCancellation(handler: {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: handler, options: .asBestFittingWrapper)
        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(pointerToBlock.__externalRCRef(), _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
    }())
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_isActive_get__reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_isActive_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.isActive
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_isCancelled_get__reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_isCancelled_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.isCancelled
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_isCompleted_get__reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_isCompleted_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.isCompleted
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_resumeUndispatchedWithException__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_CoroutineDispatcher_ExportedKotlinPackages_kotlin_Throwable____reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_resumeUndispatchedWithException__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_CoroutineDispatcher_ExportedKotlinPackages_kotlin_Throwable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ receiver: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.resumeUndispatchedWithException(ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: receiver), exception: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: exception))
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_resumeUndispatched__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_CoroutineDispatcher_T____reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_resumeUndispatched__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_CoroutineDispatcher_T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ receiver: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.resumeUndispatched(ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: receiver), value: __Self.T.__fromKotlinPointer(value))
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_resume__TypesOfArguments__T_Swift_Optional_U28ExportedKotlinPackages_kotlin_ThrowableU29202D_U20Swift_Void_____reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_resume__TypesOfArguments__T_Swift_Optional_U28ExportedKotlinPackages_kotlin_ThrowableU29202D_U20Swift_Void_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?, _ onCancellation: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.resume(value: __Self.T.__fromKotlinPointer(value), onCancellation: onCancellation.map { it in {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: it, options: .asBestFittingWrapper)
        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlin_Throwable__(pointerToBlock.__externalRCRef(), _1.__externalRCRef()); return () }() }
    }() })
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_tryResumeWithException__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable____reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_tryResumeWithException__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.tryResumeWithException(exception: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: exception))
        return (_result).flatMap { it in it.__retainedToKotlinPointer() }
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_tryResume__TypesOfArguments__T_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_U28ExportedKotlinPackages_kotlin_ThrowableU29202D_U20Swift_Void_____reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_tryResume__TypesOfArguments__T_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_U28ExportedKotlinPackages_kotlin_ThrowableU29202D_U20Swift_Void_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?, _ idempotent: Swift.UnsafeMutableRawPointer?, _ onCancellation: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.tryResume(value: __Self.T.__fromKotlinPointer(value), idempotent: { switch idempotent { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }(), onCancellation: onCancellation.map { it in {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: it, options: .asBestFittingWrapper)
        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlin_Throwable__(pointerToBlock.__externalRCRef(), _1.__externalRCRef()); return () }() }
    }() })
        return (_result).flatMap { it in it.__retainedToKotlinPointer() }
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_CancellableContinuation_tryResume__TypesOfArguments__T_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_CancellableContinuation_tryResume__TypesOfArguments__T_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?, _ idempotent: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.tryResume(value: __Self.T.__fromKotlinPointer(value), idempotent: { switch idempotent { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
        return (_result).flatMap { it in it.__retainedToKotlinPointer() }
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_CloseableCoroutineDispatcher_close__reverse_swift")
package func kotlinx_coroutines_CloseableCoroutineDispatcher_close__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.CloseableCoroutineDispatcher.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = _self.close()
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_CompletableDeferred_completeExceptionally__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable____reverse_swift")
package func kotlinx_coroutines_CompletableDeferred_completeExceptionally__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.completeExceptionally(exception: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: exception))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_CompletableDeferred_complete__TypesOfArguments__T____reverse_swift")
package func kotlinx_coroutines_CompletableDeferred_complete__TypesOfArguments__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.complete(value: __Self.T.__fromKotlinPointer(value))
        return _result
    }
    return _open(_rawSelf)
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

@_cdecl("kotlinx_coroutines_CopyableThrowable_createCopy__reverse_swift")
package func kotlinx_coroutines_CopyableThrowable_createCopy__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.CopyableThrowable)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.CopyableThrowable>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: Swift.Optional<__Self.T> = _self.createCopy()
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_CoroutineDispatcher_dispatchYield__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_anyU20ExportedKotlinPackages_kotlinx_coroutines_Runnable____reverse_swift")
package func kotlinx_coroutines_CoroutineDispatcher_dispatchYield__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_anyU20ExportedKotlinPackages_kotlinx_coroutines_Runnable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ context: Swift.UnsafeMutableRawPointer, _ block: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = _self.dispatchYield(context: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: context, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext, block: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: block, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Runnable.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Runnable)
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_CoroutineDispatcher_dispatch__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_anyU20ExportedKotlinPackages_kotlinx_coroutines_Runnable____reverse_swift")
package func kotlinx_coroutines_CoroutineDispatcher_dispatch__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_anyU20ExportedKotlinPackages_kotlinx_coroutines_Runnable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ context: Swift.UnsafeMutableRawPointer, _ block: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Void = _self.dispatch(context: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: context, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext, block: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: block, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Runnable.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Runnable)
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_CoroutineDispatcher_isDispatchNeeded__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext____reverse_swift")
package func kotlinx_coroutines_CoroutineDispatcher_isDispatchNeeded__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ context: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Bool = _self.isDispatchNeeded(context: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: context, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext)
    return _result
}

@_cdecl("kotlinx_coroutines_CoroutineDispatcher_limitedParallelism__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlinx_coroutines_CoroutineDispatcher_limitedParallelism__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ parallelism: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: `self`)
    let _result: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher = _self.limitedParallelism(parallelism: parallelism)
    return _result.__externalRCRef()
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
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.Deferred)
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.Deferred>(_ _self: __Self) -> Swift.Bool {
        let __continuation: (__Self.T) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid_generic_Function1_kotlin_Any___Unit___TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(pointerToBlock.__externalRCRef(), _1.__retainedToKotlinPointer()); return () }() }
}()
        withKotlinTask(__continuation, __exception, __cancellation) {
            try await _self.`await`()
        }
        return true
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_Deferred_getCompleted__reverse_swift")
package func kotlinx_coroutines_Deferred_getCompleted__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.Deferred)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.Deferred>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.getCompleted()
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_Deferred_getCompletionExceptionOrNull__reverse_swift")
package func kotlinx_coroutines_Deferred_getCompletionExceptionOrNull__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.Deferred)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.Deferred>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: Swift.Optional<ExportedKotlinPackages.kotlin.Throwable> = _self.getCompletionExceptionOrNull()
        return _result.map { it in it.__externalRCRef() } ?? nil
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_Deferred_onAwait_get__reverse_swift")
package func kotlinx_coroutines_Deferred_onAwait_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.Deferred)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.Deferred>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1<__Self.T> = _self.onAwait
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_Delay_invokeOnTimeout__TypesOfArguments__Swift_Int64_anyU20ExportedKotlinPackages_kotlinx_coroutines_Runnable_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext____reverse_swift")
package func kotlinx_coroutines_Delay_invokeOnTimeout__TypesOfArguments__Swift_Int64_anyU20ExportedKotlinPackages_kotlinx_coroutines_Runnable_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ timeMillis: Swift.Int64, _ block: Swift.UnsafeMutableRawPointer, _ context: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Delay.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Delay
    let _result: any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle = _self.invokeOnTimeout(timeMillis: timeMillis, block: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: block, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Runnable.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Runnable, context: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: context, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext)
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_Delay_scheduleResumeAfterDelay__TypesOfArguments__Swift_Int64_anyU20ExportedKotlinPackages_kotlinx_coroutines_CancellableContinuation_KotlinRuntimeSupport_KotlinUnit_____reverse_swift")
package func kotlinx_coroutines_Delay_scheduleResumeAfterDelay__TypesOfArguments__Swift_Int64_anyU20ExportedKotlinPackages_kotlinx_coroutines_CancellableContinuation_KotlinRuntimeSupport_KotlinUnit_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ timeMillis: Swift.Int64, _ continuation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Delay.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Delay
    let _result: Swift.Void = _self.scheduleResumeAfterDelay(timeMillis: timeMillis, continuation: { let _ptr = continuation; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation<KotlinRuntimeSupport.KotlinUnit>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.AnyCancellableContinuation<KotlinRuntimeSupport.KotlinUnit>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
    return { _result; return true }()
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
    let _result: any ExportedKotlinPackages.kotlin.sequences.Sequence<KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines._Job>> = _self.children
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_Job_getCancellationException__reverse_swift")
package func kotlinx_coroutines_Job_getCancellationException__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job
    let _result: ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException = _self.getCancellationException()
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_Job_invokeOnCompletion__TypesOfArguments__Swift_Bool_Swift_Bool_U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void____reverse_swift")
package func kotlinx_coroutines_Job_invokeOnCompletion__TypesOfArguments__Swift_Bool_Swift_Bool_U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ onCancelling: Swift.Bool, _ invokeImmediately: Swift.Bool, _ handler: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job
    let _result: any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle = _self.invokeOnCompletion(onCancelling: onCancelling, invokeImmediately: invokeImmediately, handler: {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: handler, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(pointerToBlock.__externalRCRef(), _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
}())
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_Job_invokeOnCompletion__TypesOfArguments__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void____reverse_swift")
package func kotlinx_coroutines_Job_invokeOnCompletion__TypesOfArguments__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ handler: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.Job.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.Job
    let _result: any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle = _self.invokeOnCompletion(handler: {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: handler, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(pointerToBlock.__externalRCRef(), _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
}())
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
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef(), { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
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
    let _self = ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher.__createClassWrapper(externalRCRef: `self`)
    let _result: ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher = _self.immediate
    return _result.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_MainCoroutineDispatcher_limitedParallelism__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlinx_coroutines_MainCoroutineDispatcher_limitedParallelism__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ parallelism: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher.__createClassWrapper(externalRCRef: `self`)
    let _result: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher = _self.limitedParallelism(parallelism: parallelism)
    return _result.__externalRCRef()
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
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.cancel(cause: { switch cause { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException.__createClassWrapper(externalRCRef: res); } }())
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@available(*, deprecated, message: "BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported")
@_cdecl("kotlinx_coroutines_channels_BroadcastChannel_openSubscription__reverse_swift")
package func kotlinx_coroutines_channels_BroadcastChannel_openSubscription__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<__Self.E> = _self.openSubscription()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_channels_ChannelIterator_hasNext__reverse_swift")
package func kotlinx_coroutines_channels_ChannelIterator_hasNext__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator)
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator>(_ _self: __Self) -> Swift.Bool {
        let __continuation: (Swift.Bool) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef(), _1); return () }() }
}()
        withKotlinTask(__continuation, __exception, __cancellation) {
            try await _self.hasNext()
        }
        return true
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_channels_ChannelIterator_next__reverse_swift")
package func kotlinx_coroutines_channels_ChannelIterator_next__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.E = _self.next()
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_channels_ProducerScope_channel_get__reverse_swift")
package func kotlinx_coroutines_channels_ProducerScope_channel_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel<__Self.E> = _self.channel
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_channels_ReceiveChannel_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException_____reverse_swift")
package func kotlinx_coroutines_channels_ReceiveChannel_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ cause: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.cancel(cause: { switch cause { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException.__createClassWrapper(externalRCRef: res); } }())
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_channels_ReceiveChannel_isClosedForReceive_get__reverse_swift")
package func kotlinx_coroutines_channels_ReceiveChannel_isClosedForReceive_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.isClosedForReceive
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_channels_ReceiveChannel_isEmpty_get__reverse_swift")
package func kotlinx_coroutines_channels_ReceiveChannel_isEmpty_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.isEmpty
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_channels_ReceiveChannel_iterator__reverse_swift")
package func kotlinx_coroutines_channels_ReceiveChannel_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator<__Self.E> = _self.iterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_channels_ReceiveChannel_onReceiveCatching_get__reverse_swift")
package func kotlinx_coroutines_channels_ReceiveChannel_onReceiveCatching_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1<ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<__Self.E>> = _self.onReceiveCatching
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_channels_ReceiveChannel_onReceive_get__reverse_swift")
package func kotlinx_coroutines_channels_ReceiveChannel_onReceive_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1<__Self.E> = _self.onReceive
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_channels_ReceiveChannel_receiveCatching__reverse_swift")
package func kotlinx_coroutines_channels_ReceiveChannel_receiveCatching__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel)
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel>(_ _self: __Self) -> Swift.Bool {
        let __continuation: (ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<__Self.E>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid_generic_Function1_kotlinx_coroutines_channels_ChannelResult_kotlin_Any____Unit___TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(pointerToBlock.__externalRCRef(), _1.__retainedToKotlinPointer()); return () }() }
}()
        withKotlinTask(__continuation, __exception, __cancellation) {
            try await _self.receiveCatching()
        }
        return true
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_channels_ReceiveChannel_receive__reverse_swift")
package func kotlinx_coroutines_channels_ReceiveChannel_receive__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel)
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel>(_ _self: __Self) -> Swift.Bool {
        let __continuation: (__Self.E) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid_generic_Function1_kotlin_Any___Unit___TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(pointerToBlock.__externalRCRef(), _1.__retainedToKotlinPointer()); return () }() }
}()
        withKotlinTask(__continuation, __exception, __cancellation) {
            try await _self.receive()
        }
        return true
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_channels_ReceiveChannel_tryReceive__reverse_swift")
package func kotlinx_coroutines_channels_ReceiveChannel_tryReceive__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<__Self.E> = _self.tryReceive()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_channels_SendChannel_close__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_____reverse_swift")
package func kotlinx_coroutines_channels_SendChannel_close__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ cause: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.close(cause: { switch cause { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_channels_SendChannel_invokeOnClose__TypesOfArguments__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void____reverse_swift")
package func kotlinx_coroutines_channels_SendChannel_invokeOnClose__TypesOfArguments__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ handler: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.invokeOnClose(handler: {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: handler, options: .asBestFittingWrapper)
        return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(pointerToBlock.__externalRCRef(), _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
    }())
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_channels_SendChannel_isClosedForSend_get__reverse_swift")
package func kotlinx_coroutines_channels_SendChannel_isClosedForSend_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.isClosedForSend
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_channels_SendChannel_onSend_get__reverse_swift")
package func kotlinx_coroutines_channels_SendChannel_onSend_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2<__Self.E,ExportedKotlinPackages.kotlinx.coroutines.channels.AnySendChannel<__Self.E>> = _self.onSend
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_channels_SendChannel_send__TypesOfArguments__E____reverse_swift")
package func kotlinx_coroutines_channels_SendChannel_send__TypesOfArguments__E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel)
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel>(_ _self: __Self) -> Swift.Bool {
        let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef(), { _1; return true }()); return () }() }
}()
        withKotlinTask(__continuation, __exception, __cancellation) {
            try await _self.send(element: __Self.E.__fromKotlinPointer(element))
        }
        return true
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_channels_SendChannel_trySend__TypesOfArguments__E____reverse_swift")
package func kotlinx_coroutines_channels_SendChannel_trySend__TypesOfArguments__E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult<KotlinRuntimeSupport.KotlinUnit> = _self.trySend(element: __Self.E.__fromKotlinPointer(element))
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_flow_AbstractFlow_collectSafely__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_T_____reverse_swift")
package func kotlinx_coroutines_flow_AbstractFlow_collectSafely__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_T_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ collector: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.flow._AbstractFlow_ReverseDispatch)
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.flow._AbstractFlow_ReverseDispatch>(_ _self: __Self) -> Swift.Bool {
        let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef(), { _1; return true }()); return () }() }
}()
        withKotlinTask(__continuation, __exception, __cancellation) {
            try await _self.collectSafely(collector: { let _ptr = collector; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<__Self.T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<__Self.T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        }
        return true
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_flow_FlowCollector_emit__TypesOfArguments__T____reverse_swift")
package func kotlinx_coroutines_flow_FlowCollector_emit__TypesOfArguments__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector)
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector>(_ _self: __Self) -> Swift.Bool {
        let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef(), { _1; return true }()); return () }() }
}()
        withKotlinTask(__continuation, __exception, __cancellation) {
            try await _self.emit(value: __Self.T.__fromKotlinPointer(value))
        }
        return true
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_flow_Flow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_Swift_Optional_KotlinRuntimeSupport_KotlinAny______reverse_swift")
package func kotlinx_coroutines_flow_Flow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_Swift_Optional_KotlinRuntimeSupport_KotlinAny______reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ collector: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef(), { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.collect(collector: { let _ptr = collector; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
    }
    return true
}

@_cdecl("kotlinx_coroutines_flow_MutableSharedFlow_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_flow_MutableSharedFlow_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef(), { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
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
    return (_result).flatMap { it in it.__retainedToKotlinPointer() }
}

@_cdecl("kotlinx_coroutines_flow_MutableStateFlow_value_set__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_flow_MutableStateFlow_value_set__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ newValue: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow
    let _result: Swift.Void = { _self.value = { switch newValue { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }() }()
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_flow_SharedFlow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_Swift_Optional_KotlinRuntimeSupport_KotlinAny______reverse_swift")
package func kotlinx_coroutines_flow_SharedFlow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_Swift_Optional_KotlinRuntimeSupport_KotlinAny______reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ collector: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow
    let __continuation: (Swift.Never) -> Swift.Void = {
    let _ = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _ in fatalError() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.collect(collector: { let _ptr = collector; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
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
    return (_result).flatMap { it in it.__retainedToKotlinPointer() }
}

@_cdecl("kotlinx_coroutines_flow_internal_ChannelFlow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_T_____reverse_swift")
package func kotlinx_coroutines_flow_internal_ChannelFlow_collect__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector_T_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ collector: Swift.UnsafeMutableRawPointer, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`._ChannelFlow_ReverseDispatch)
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`._ChannelFlow_ReverseDispatch>(_ _self: __Self) -> Swift.Bool {
        let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef(), { _1; return true }()); return () }() }
}()
        withKotlinTask(__continuation, __exception, __cancellation) {
            try await _self.collect(collector: { let _ptr = collector; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector<__Self.T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.flow.AnyFlowCollector<__Self.T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        }
        return true
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_flow_internal_ChannelFlow_dropChannelOperators__reverse_swift")
package func kotlinx_coroutines_flow_internal_ChannelFlow_dropChannelOperators__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`._ChannelFlow_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`._ChannelFlow_ReverseDispatch>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: Swift.Optional<any KotlinCoroutineSupport.KotlinTypedFlow<__Self.T>> = _self.dropChannelOperators()
        return _result.map { it in it.wrapped.__externalRCRef() } ?? nil
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_flow_internal_ChannelFlow_fuse__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow____reverse_swift")
package func kotlinx_coroutines_flow_internal_ChannelFlow_fuse__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ context: Swift.UnsafeMutableRawPointer, _ capacity: Swift.Int32, _ onBufferOverflow: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`._ChannelFlow_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`._ChannelFlow_ReverseDispatch>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any KotlinCoroutineSupport.KotlinTypedFlow<__Self.T> = _self.fuse(context: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: context, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext, capacity: capacity, onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow.__fromKotlinPointer(onBufferOverflow))
        return _result.wrapped.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_flow_internal_ChannelFlow_produceImpl__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope____reverse_swift")
package func kotlinx_coroutines_flow_internal_ChannelFlow_produceImpl__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ scope: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`._ChannelFlow_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`._ChannelFlow_ReverseDispatch>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel<__Self.T> = _self.produceImpl(scope: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: scope, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope)
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_flow_internal_FusibleFlow_fuse__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow____reverse_swift")
package func kotlinx_coroutines_flow_internal_FusibleFlow_fuse__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ context: Swift.UnsafeMutableRawPointer, _ capacity: Swift.Int32, _ onBufferOverflow: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.FusibleFlow)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.FusibleFlow>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any KotlinCoroutineSupport.KotlinTypedFlow<__Self.T> = _self.fuse(context: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: context, conformsTo: ExportedKotlinPackages.kotlin.coroutines.CoroutineContext.Type.self) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext, capacity: capacity, onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow.__fromKotlinPointer(onBufferOverflow))
        return _result.wrapped.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_internal_AtomicOp_atomicOp_get__reverse_swift")
package func kotlinx_coroutines_internal_AtomicOp_atomicOp_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.`internal`._AtomicOp_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.`internal`._AtomicOp_ReverseDispatch>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp<Swift.Optional<KotlinRuntimeSupport.KotlinAny>> = _self.atomicOp
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_internal_AtomicOp_complete__TypesOfArguments__T_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_internal_AtomicOp_complete__TypesOfArguments__T_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ affected: Swift.UnsafeMutableRawPointer?, _ failure: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.`internal`._AtomicOp_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.`internal`._AtomicOp_ReverseDispatch>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.complete(affected: __Self.T.__fromKotlinPointer(affected), failure: { switch failure { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_internal_AtomicOp_prepare__TypesOfArguments__T____reverse_swift")
package func kotlinx_coroutines_internal_AtomicOp_prepare__TypesOfArguments__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ affected: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.`internal`._AtomicOp_ReverseDispatch)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.`internal`._AtomicOp_ReverseDispatch>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.prepare(affected: __Self.T.__fromKotlinPointer(affected))
        return (_result).flatMap { it in it.__retainedToKotlinPointer() }
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_internal_LockFreeLinkedListHead_isRemoved_get__reverse_swift")
package func kotlinx_coroutines_internal_LockFreeLinkedListHead_isRemoved_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.`internal`.LockFreeLinkedListHead.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Bool = _self.isRemoved
    return _result
}

@_cdecl("kotlinx_coroutines_internal_LockFreeLinkedListNode_isRemoved_get__reverse_swift")
package func kotlinx_coroutines_internal_LockFreeLinkedListNode_isRemoved_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.`internal`.LockFreeLinkedListNode.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Bool = _self.isRemoved
    return _result
}

@_cdecl("kotlinx_coroutines_internal_LockFreeLinkedListNode_remove__reverse_swift")
package func kotlinx_coroutines_internal_LockFreeLinkedListNode_remove__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.`internal`.LockFreeLinkedListNode.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Bool = _self.remove()
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
    let _self = ExportedKotlinPackages.kotlinx.coroutines.`internal`.OpDescriptor.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Optional<ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>> = _self.atomicOp
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlinx_coroutines_internal_OpDescriptor_perform__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_internal_OpDescriptor_perform__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ affected: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _self = ExportedKotlinPackages.kotlinx.coroutines.`internal`.OpDescriptor.__createClassWrapper(externalRCRef: `self`)
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.perform(affected: { switch affected { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return (_result).flatMap { it in it.__retainedToKotlinPointer() }
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

@_cdecl("kotlinx_coroutines_selects_SelectBuilder_invoke__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause0_U282920asyncU20throwsU202D_U20R____reverse_swift")
package func kotlinx_coroutines_selects_SelectBuilder_invoke__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause0_U282920asyncU20throwsU202D_U20R____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ receiver: Swift.UnsafeMutableRawPointer, _ block: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.invoke(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: receiver, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0, block: {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: block, options: .asBestFittingWrapper)
        return { return __Self.R.__fromKotlinPointer(try await withKotlinContinuation { continuation, exception, cancellation in
        let _: Bool = KotlinxCoroutinesCore_internal_functional_type_caller_async_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E_generic_kotlin_coroutines_SuspendFunction0_kotlin_Any____TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToBlock.__externalRCRef(), Unmanaged.passRetained((continuation as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
    }) }
    }())
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_selects_SelectBuilder_invoke__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause1_Q__U28QU2920asyncU20throwsU202D_U20R____reverse_swift")
package func kotlinx_coroutines_selects_SelectBuilder_invoke__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause1_Q__U28QU2920asyncU20throwsU202D_U20R____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ receiver: Swift.UnsafeMutableRawPointer, _ block: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.invoke({ let _ptr = receiver; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectClause1<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }(), block: {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: block, options: .asBestFittingWrapper)
        return { _1 in return __Self.R.__fromKotlinPointer(try await withKotlinContinuation { continuation, exception, cancellation in
        let _: Bool = KotlinxCoroutinesCore_internal_functional_type_caller_async_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E_generic_kotlin_coroutines_SuspendFunction1_kotlin_Any___kotlin_Any____TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(pointerToBlock.__externalRCRef(), _1.__retainedToKotlinPointer(), Unmanaged.passRetained((continuation as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
    }) }
    }())
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_selects_SelectBuilder_invoke__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause2_P_Q__P_U28QU2920asyncU20throwsU202D_U20R____reverse_swift")
package func kotlinx_coroutines_selects_SelectBuilder_invoke__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause2_P_Q__P_U28QU2920asyncU20throwsU202D_U20R____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ receiver: Swift.UnsafeMutableRawPointer, _ param: Swift.UnsafeMutableRawPointer?, _ block: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.invoke({ let _ptr = receiver; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2<Swift.Optional<KotlinRuntimeSupport.KotlinAny>,Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectClause2<Swift.Optional<KotlinRuntimeSupport.KotlinAny>,Swift.Optional<KotlinRuntimeSupport.KotlinAny>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }(), param: Swift.Optional<KotlinRuntimeSupport.KotlinAny>.__fromKotlinPointer(param), block: {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: block, options: .asBestFittingWrapper)
        return { _1 in return __Self.R.__fromKotlinPointer(try await withKotlinContinuation { continuation, exception, cancellation in
        let _: Bool = KotlinxCoroutinesCore_internal_functional_type_caller_async_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E_generic_kotlin_coroutines_SuspendFunction1_kotlin_Any___kotlin_Any____TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(pointerToBlock.__externalRCRef(), _1.__retainedToKotlinPointer(), Unmanaged.passRetained((continuation as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
    }) }
    }())
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_selects_SelectBuilder_invoke__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause2_Swift_Optional_P__Q__U28QU2920asyncU20throwsU202D_U20R____reverse_swift")
package func kotlinx_coroutines_selects_SelectBuilder_invoke__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause2_Swift_Optional_P__Q__U28QU2920asyncU20throwsU202D_U20R____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ receiver: Swift.UnsafeMutableRawPointer, _ block: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.invoke({ let _ptr = receiver; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2<Swift.Optional<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>,Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlinx.coroutines.selects.AnySelectClause2<Swift.Optional<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>,Swift.Optional<KotlinRuntimeSupport.KotlinAny>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }(), block: {
        let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: block, options: .asBestFittingWrapper)
        return { _1 in return __Self.R.__fromKotlinPointer(try await withKotlinContinuation { continuation, exception, cancellation in
        let _: Bool = KotlinxCoroutinesCore_internal_functional_type_caller_async_SwiftU2EOptionalU3CSwiftU2EUnsafeMutableRawPointerU3E_generic_kotlin_coroutines_SuspendFunction1_kotlin_Any___kotlin_Any____TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_UnsafeMutableRawPointer___(pointerToBlock.__externalRCRef(), _1.__retainedToKotlinPointer(), Unmanaged.passRetained((continuation as (Swift.Optional<Swift.UnsafeMutableRawPointer>) -> Swift.Void) as AnyObject).toOpaque(), Unmanaged.passRetained((exception as (Swift.Optional<Swift.Error>) -> Swift.Void) as AnyObject).toOpaque(), cancellation.__externalRCRef())
    }) }
    }())
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_selects_SelectClause_clauseObject_get__reverse_swift")
package func kotlinx_coroutines_selects_SelectClause_clauseObject_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause
    let _result: any KotlinRuntimeSupport._KotlinBridgeable = _self.clauseObject
    return (_result).__retainedToKotlinPointer()!
}

@_cdecl("kotlinx_coroutines_selects_SelectClause_onCancellationConstructor_get__reverse_swift")
package func kotlinx_coroutines_selects_SelectClause_onCancellationConstructor_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause
    let _result: Swift.Optional<(any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> (ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void> = _self.onCancellationConstructor
    return _result.map { it in Unmanaged.passRetained((it as (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> (ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void) as AnyObject).toOpaque() } ?? nil
}

@_cdecl("kotlinx_coroutines_selects_SelectClause_processResFunc_get__reverse_swift")
package func kotlinx_coroutines_selects_SelectClause_processResFunc_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause
    let _result: (any KotlinRuntimeSupport._KotlinBridgeable, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.processResFunc
    return Unmanaged.passRetained((_result as (any KotlinRuntimeSupport._KotlinBridgeable, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) as AnyObject).toOpaque()
}

@_cdecl("kotlinx_coroutines_selects_SelectClause_regFunc_get__reverse_swift")
package func kotlinx_coroutines_selects_SelectClause_regFunc_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause
    let _result: (any KotlinRuntimeSupport._KotlinBridgeable, any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = _self.regFunc
    return Unmanaged.passRetained((_result as (any KotlinRuntimeSupport._KotlinBridgeable, any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance, Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void) as AnyObject).toOpaque()
}

@_cdecl("kotlinx_coroutines_selects_SelectInstance_context_get__reverse_swift")
package func kotlinx_coroutines_selects_SelectInstance_context_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext = _self.context
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_selects_SelectInstance_disposeOnCompletion__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_DisposableHandle____reverse_swift")
package func kotlinx_coroutines_selects_SelectInstance_disposeOnCompletion__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_DisposableHandle____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ disposableHandle: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.disposeOnCompletion(disposableHandle: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: disposableHandle, conformsTo: ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle.Type.self) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle)
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_selects_SelectInstance_selectInRegistrationPhase__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_selects_SelectInstance_selectInRegistrationPhase__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ internalResult: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.selectInRegistrationPhase(internalResult: { switch internalResult { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlinx_coroutines_selects_SelectInstance_trySelect__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_selects_SelectInstance_trySelect__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ clauseObject: Swift.UnsafeMutableRawPointer, _ result: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance)
    func _open<__Self: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.trySelect(clauseObject: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: clauseObject), result: { switch result { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
        return _result
    }
    return _open(_rawSelf)
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
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef(), { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
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
    let _result: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2<Swift.Optional<KotlinRuntimeSupport.KotlinAny>,KotlinRuntimeSupport._KotlinExistential<any ExportedKotlinPackages.kotlinx.coroutines.sync._Mutex>> = _self.onLock
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
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef(), { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef(), _1.map { it in KotlinRuntimeSupport.kotlinThrowableRCRef(for: it) } ?? nil); return () }() }
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

@usableFromInline
@discardableResult
internal func _cbridge_kotlinx_coroutines_CompletableDeferred__TypesOfArguments__T__(_ _p0: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer {
    return kotlinx_coroutines_CompletableDeferred__TypesOfArguments__T__(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlinx_coroutines_CoroutineDispatcher_interceptContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation_T___(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    return kotlinx_coroutines_CoroutineDispatcher_interceptContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation_T___(_p0, _p1)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlinx_coroutines_channels_ChannelResult_exceptionOrNull(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    return kotlinx_coroutines_channels_ChannelResult_exceptionOrNull(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlinx_coroutines_channels_ChannelResult_getOrNull(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    return kotlinx_coroutines_channels_ChannelResult_getOrNull(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlinx_coroutines_channels_ChannelResult_getOrThrow(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    return kotlinx_coroutines_channels_ChannelResult_getOrThrow(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlinx_coroutines_channels_ChannelResult_isClosed_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    return kotlinx_coroutines_channels_ChannelResult_isClosed_get(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlinx_coroutines_channels_ChannelResult_isFailure_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    return kotlinx_coroutines_channels_ChannelResult_isFailure_get(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlinx_coroutines_channels_ChannelResult_isSuccess_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    return kotlinx_coroutines_channels_ChannelResult_isSuccess_get(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlinx_coroutines_channels_trySendBlocking__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_SendChannel_E__E__(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer {
    return kotlinx_coroutines_channels_trySendBlocking__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_SendChannel_E__E__(_p0, _p1)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlinx_coroutines_completeWith__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CompletableDeferred_T__ExportedKotlinPackages_kotlin_Result_T___(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    return kotlinx_coroutines_completeWith__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CompletableDeferred_T__ExportedKotlinPackages_kotlin_Result_T___(_p0, _p1)
}

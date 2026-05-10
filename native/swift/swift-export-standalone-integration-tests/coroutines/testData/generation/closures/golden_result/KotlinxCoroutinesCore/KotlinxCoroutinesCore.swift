import Atomicfu
@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinxCoroutinesCore
@_exported import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport
@_spi(kotlin$ExperimentalStdlibApi) import KotlinStdlib

public final class _ExportedKotlinPackages_kotlinx_coroutines_CoroutineExceptionHandler_Key: KotlinRuntime.KotlinBase {
    public static var shared: KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_CoroutineExceptionHandler_Key {
        get {
            return KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_CoroutineExceptionHandler_Key.__createClassWrapper(externalRCRef: kotlinx_coroutines_CoroutineExceptionHandler_Key_get())
        }
    }
    private init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class _ExportedKotlinPackages_kotlinx_coroutines_Job_Key: KotlinRuntime.KotlinBase {
    public static var shared: KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_Job_Key {
        get {
            return KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_Job_Key.__createClassWrapper(externalRCRef: kotlinx_coroutines_Job_Key_get())
        }
    }
    private init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
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
    private init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class _ExportedKotlinPackages_kotlinx_coroutines_flow_SharingStarted_Companion: KotlinRuntime.KotlinBase {
    public var Eagerly: any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_SharingStarted_Companion_Eagerly_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted
        }
    }
    public var Lazily: any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_SharingStarted_Companion_Lazily_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted
        }
    }
    public static var shared: KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_flow_SharingStarted_Companion {
        get {
            return KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_flow_SharingStarted_Companion.__createClassWrapper(externalRCRef: kotlinx_coroutines_flow_SharingStarted_Companion_get())
        }
    }
    private init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public func WhileSubscribed(
        stopTimeoutMillis: Swift.Int64,
        replayExpirationMillis: Swift.Int64
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_SharingStarted_Companion_WhileSubscribed__TypesOfArguments__Swift_Int64_Swift_Int64__(self.__externalRCRef(), stopTimeoutMillis, replayExpirationMillis)) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel where Self : KotlinRuntimeSupport._KotlinBridgeable {
    @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public func cancel(
        cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
    ) -> Swift.Void {
        return { kotlinx_coroutines_channels_BroadcastChannel_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException___(self.__externalRCRef(), cause.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public func openSubscription() -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_BroadcastChannel_openSubscription(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel {
}
extension ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation where Self : KotlinRuntimeSupport._KotlinBridgeable {
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
            let originalBlock = handler
            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()); return true }() }
        }()); return () }()
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func resume(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        onCancellation: ((ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void)?
    ) -> Swift.Void {
        return { kotlinx_coroutines_CancellableContinuation_resume__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_U28ExportedKotlinPackages_kotlin_ThrowableU29202D_U20Swift_Void___(self.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil, onCancellation.map { it in {
            let originalBlock = it
            return { (arg0: Swift.UnsafeMutableRawPointer) in return { originalBlock(ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: arg0)); return true }() }
        }() } ?? nil); return () }()
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
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func tryResume(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        idempotent: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_CancellableContinuation_tryResume__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil, idempotent.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func tryResume(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        idempotent: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        onCancellation: ((ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_CancellableContinuation_tryResume__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_U28ExportedKotlinPackages_kotlin_ThrowableU29202D_U20Swift_Void___(self.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil, idempotent.map { it in it.__externalRCRef() } ?? nil, onCancellation.map { it in {
            let originalBlock = it
            return { (arg0: Swift.UnsafeMutableRawPointer) in return { originalBlock(ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: arg0)); return true }() }
        }() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func tryResumeWithException(
        exception: ExportedKotlinPackages.kotlin.Throwable
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_CancellableContinuation_tryResumeWithException__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable__(self.__externalRCRef(), exception.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation {
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.Channel where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.Channel {
    typealias Factory = KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_channels_Channel_Factory
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func hasNext() async throws -> Swift.Bool {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Bool) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_channels_ChannelIterator_hasNext(self.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.Bool) in return { originalBlock(arg0); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_channels_ChannelIterator_next(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator {
}
@available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ChildHandle")
extension ExportedKotlinPackages.kotlinx.coroutines.ChildHandle where Self : KotlinRuntimeSupport._KotlinBridgeable {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)? {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return { switch kotlinx_coroutines_ChildHandle_parent_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res) as! any ExportedKotlinPackages.kotlinx.coroutines.Job; } }()
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func childCancelled(
        cause: ExportedKotlinPackages.kotlin.Throwable
    ) -> Swift.Bool {
        return kotlinx_coroutines_ChildHandle_childCancelled__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable__(self.__externalRCRef(), cause.__externalRCRef())
    }
}
@available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ChildHandle")
extension ExportedKotlinPackages.kotlinx.coroutines.ChildHandle {
}
@available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ChildJob")
extension ExportedKotlinPackages.kotlinx.coroutines.ChildJob where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
@available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ChildJob")
extension ExportedKotlinPackages.kotlinx.coroutines.ChildJob {
}
extension ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred where Self : KotlinRuntimeSupport._KotlinBridgeable {
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
extension ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred {
}
extension ExportedKotlinPackages.kotlinx.coroutines.CompletableJob where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func complete() -> Swift.Bool {
        return kotlinx_coroutines_CompletableJob_complete(self.__externalRCRef())
    }
    public func completeExceptionally(
        exception: ExportedKotlinPackages.kotlin.Throwable
    ) -> Swift.Bool {
        return kotlinx_coroutines_CompletableJob_completeExceptionally__TypesOfArguments__ExportedKotlinPackages_kotlin_Throwable__(self.__externalRCRef(), exception.__externalRCRef())
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.CompletableJob {
}
extension ExportedKotlinPackages.kotlinx.coroutines.CoroutineExceptionHandler where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func handleException(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        exception: ExportedKotlinPackages.kotlin.Throwable
    ) -> Swift.Void {
        return { kotlinx_coroutines_CoroutineExceptionHandler_handleException__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ExportedKotlinPackages_kotlin_Throwable__(self.__externalRCRef(), context.__externalRCRef(), exception.__externalRCRef()); return () }()
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.CoroutineExceptionHandler {
    typealias Key = KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_CoroutineExceptionHandler_Key
}
extension ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var coroutineContext: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_CoroutineScope_coroutineContext_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
        }
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope {
}
extension ExportedKotlinPackages.kotlinx.coroutines.Deferred where Self : KotlinRuntimeSupport._KotlinBridgeable {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var onAwait: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1 {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_Deferred_onAwait_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1
        }
    }
    public func `await`() async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_Deferred_await(self.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func getCompleted() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_Deferred_getCompleted(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func getCompletionExceptionOrNull() -> ExportedKotlinPackages.kotlin.Throwable? {
        return { switch kotlinx_coroutines_Deferred_getCompletionExceptionOrNull(self.__externalRCRef()) { case nil: .none; case let res: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.Deferred {
}
extension ExportedKotlinPackages.kotlinx.coroutines.Delay where Self : KotlinRuntimeSupport._KotlinBridgeable {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invokeOnTimeout(
        timeMillis: Swift.Int64,
        block: any ExportedKotlinPackages.kotlinx.coroutines.Runnable,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_Delay_invokeOnTimeout__TypesOfArguments__Swift_Int64_anyU20ExportedKotlinPackages_kotlinx_coroutines_Runnable_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(self.__externalRCRef(), timeMillis, block.__externalRCRef(), context.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.Delay {
}
extension ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func dispose() -> Swift.Void {
        return { kotlinx_coroutines_DisposableHandle_dispose(self.__externalRCRef()); return () }()
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.Flow where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.Flow {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func emit(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> Swift.Void {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_FlowCollector_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil, {
                            let originalBlock = continuation
                            return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.FusibleFlow where Self : KotlinRuntimeSupport._KotlinBridgeable {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func fuse(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        capacity: Swift.Int32,
        onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_internal_FusibleFlow_fuse__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow__(self.__externalRCRef(), context.__externalRCRef(), capacity, onBufferOverflow.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.FusibleFlow {
}
extension ExportedKotlinPackages.kotlinx.coroutines.Job where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var children: any ExportedKotlinPackages.kotlin.sequences.Sequence {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_Job_children_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.sequences.Sequence
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
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_Job_onJoin_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0
        }
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public var parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)? {
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        get {
            return { switch kotlinx_coroutines_Job_parent_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res) as! any ExportedKotlinPackages.kotlinx.coroutines.Job; } }()
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
    public func invokeOnCompletion(
        handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_Job_invokeOnCompletion__TypesOfArguments__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), {
            let originalBlock = handler
            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()); return true }() }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invokeOnCompletion(
        onCancelling: Swift.Bool,
        invokeImmediately: Swift.Bool,
        handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_Job_invokeOnCompletion__TypesOfArguments__Swift_Bool_Swift_Bool_U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), onCancelling, invokeImmediately, {
            let originalBlock = handler
            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()); return true }() }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
    }
    public func join() async throws -> Swift.Void {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_Job_join(self.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public func start() -> Swift.Bool {
        return kotlinx_coroutines_Job_start(self.__externalRCRef())
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.Job {
    typealias Key = KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_Job_Key
}
extension ExportedKotlinPackages.kotlinx.coroutines.`internal`.MainDispatcherFactory where Self : KotlinRuntimeSupport._KotlinBridgeable {
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
extension ExportedKotlinPackages.kotlinx.coroutines.`internal`.MainDispatcherFactory {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var subscriptionCount: any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Int32> {
        get {
            return KotlinCoroutineSupport._KotlinTypedStateFlowImpl<Swift.Int32>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_MutableSharedFlow_subscriptionCount_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow)
        }
    }
    public func emit(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> Swift.Void {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_MutableSharedFlow_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil, {
                            let originalBlock = continuation
                            return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
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
extension ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var value: (any KotlinRuntimeSupport._KotlinBridgeable)? {
        get {
            return { switch kotlinx_coroutines_flow_MutableStateFlow_value_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
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
extension ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow {
}
extension ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var isLocked: Swift.Bool {
        get {
            return kotlinx_coroutines_sync_Mutex_isLocked_get(self.__externalRCRef())
        }
    }
    @available(*, deprecated, message: "Mutex.onLock deprecated without replacement. For additional details please refer to #2794") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var onLock: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2 {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_sync_Mutex_onLock_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2
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
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_sync_Mutex_lock__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), owner.map { it in it.__externalRCRef() } ?? nil, {
                            let originalBlock = continuation
                            return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
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
extension ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex {
}
@available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ParentJob")
extension ExportedKotlinPackages.kotlinx.coroutines.ParentJob where Self : KotlinRuntimeSupport._KotlinBridgeable {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func getChildJobCancellationCause() -> ExportedKotlinPackages.kotlinx.coroutines.CancellationException {
        return ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException.__createClassWrapper(externalRCRef: kotlinx_coroutines_ParentJob_getChildJobCancellationCause(self.__externalRCRef()))
    }
}
@available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ParentJob")
extension ExportedKotlinPackages.kotlinx.coroutines.ParentJob {
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var channel: any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel {
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_ProducerScope_channel_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel
        }
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope {
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel where Self : KotlinRuntimeSupport._KotlinBridgeable {
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
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_ReceiveChannel_onReceive_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var onReceiveCatching: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1 {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_ReceiveChannel_onReceiveCatching_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1
        }
    }
    public func cancel(
        cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
    ) -> Swift.Void {
        return { kotlinx_coroutines_channels_ReceiveChannel_cancel__TypesOfArguments__Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException___(self.__externalRCRef(), cause.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    public func iterator() -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_ReceiveChannel_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator
    }
    public func receive() async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_channels_ReceiveChannel_receive(self.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public func receiveCatching() async throws -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_channels_ReceiveChannel_receiveCatching(self.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer) in return { originalBlock(ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult.__createClassWrapper(externalRCRef: arg0)); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public func tryReceive() -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult {
        return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult.__createClassWrapper(externalRCRef: kotlinx_coroutines_channels_ReceiveChannel_tryReceive(self.__externalRCRef()))
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel {
}
extension ExportedKotlinPackages.kotlinx.coroutines.Runnable where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func run() -> Swift.Void {
        return { kotlinx_coroutines_Runnable_run(self.__externalRCRef()); return () }()
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.Runnable {
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder where Self : KotlinRuntimeSupport._KotlinBridgeable {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public func invoke(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0,
        block: @escaping () async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return { kotlinx_coroutines_selects_SelectBuilder_invoke__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectClause0_U282920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), receiver.__externalRCRef(), {
            let originalBlock = block
            return { (__continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)

                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock()
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }()); return () }()
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder {
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause where Self : KotlinRuntimeSupport._KotlinBridgeable {
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
                let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: it, options: .asBestFittingWrapper)!
                return { _1, _2, _3 in return {
                let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: KotlinxCoroutinesCore_internal_functional_type_caller_U28ExportedKotlinPackagesU2EkotlinU2EThrowableU29202D3E20SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectInstance_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.__externalRCRef(), _2.map { it in it.__externalRCRef() } ?? nil, _3.map { it in it.__externalRCRef() } ?? nil), options: .asBestFittingWrapper)!
                return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlin_Throwable__(pointerToBlock.__externalRCRef()!, _1.__externalRCRef()); return () }() }
            }() }
            }() }
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var processResFunc: ExportedKotlinPackages.kotlinx.coroutines.selects.ProcessResultFunction {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return {
                let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: kotlinx_coroutines_selects_SelectClause_processResFunc_get(self.__externalRCRef()), options: .asBestFittingWrapper)!
                return { _1, _2, _3 in return { switch KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EOptionalU3CanyU20KotlinRuntimeSupportU2E_KotlinBridgeableU3E__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinRuntimeSupport__KotlinBridgeable_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.__externalRCRef(), _2.map { it in it.__externalRCRef() } ?? nil, _3.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }() }
            }()
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var regFunc: ExportedKotlinPackages.kotlinx.coroutines.selects.RegistrationFunction {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return {
                let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: kotlinx_coroutines_selects_SelectClause_regFunc_get(self.__externalRCRef()), options: .asBestFittingWrapper)!
                return { _1, _2, _3 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinRuntimeSupport__KotlinBridgeable_anyU20ExportedKotlinPackages_kotlinx_coroutines_selects_SelectInstance_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.__externalRCRef(), _2.__externalRCRef(), _3.map { it in it.__externalRCRef() } ?? nil); return () }() }
            }()
        }
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause {
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0 where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0 {
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1 where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1 {
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2 where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2 {
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance where Self : KotlinRuntimeSupport._KotlinBridgeable {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public var context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        get {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_selects_SelectInstance_context_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
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
extension ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance {
}
extension ExportedKotlinPackages.kotlinx.coroutines.sync.Semaphore where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var availablePermits: Swift.Int32 {
        get {
            return kotlinx_coroutines_sync_Semaphore_availablePermits_get(self.__externalRCRef())
        }
    }
    public func acquire() async throws -> Swift.Void {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_sync_Semaphore_acquire(self.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public func tryAcquire() -> Swift.Bool {
        return kotlinx_coroutines_sync_Semaphore_tryAcquire(self.__externalRCRef())
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.sync.Semaphore {
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel where Self : KotlinRuntimeSupport._KotlinBridgeable {
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
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_SendChannel_onSend_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2
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
            let originalBlock = handler
            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()); return true }() }
        }()); return () }()
    }
    public func send(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> Swift.Void {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_channels_SendChannel_send__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil, {
                            let originalBlock = continuation
                            return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public func trySend(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult {
        return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult.__createClassWrapper(externalRCRef: kotlinx_coroutines_channels_SendChannel_trySend__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil))
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var replayCache: [(any KotlinRuntimeSupport._KotlinBridgeable)?] {
        get {
            return kotlinx_coroutines_flow_SharedFlow_replayCache_get(self.__externalRCRef()) as! Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
        }
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func command(
        subscriptionCount: any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Int32>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<ExportedKotlinPackages.kotlinx.coroutines.flow.SharingCommand> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<ExportedKotlinPackages.kotlinx.coroutines.flow.SharingCommand>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_SharingStarted_command__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedStateFlow_Swift_Int32___(self.__externalRCRef(), subscriptionCount.wrapped.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted {
    typealias Companion = KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_flow_SharingStarted_Companion
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var value: (any KotlinRuntimeSupport._KotlinBridgeable)? {
        get {
            return { switch kotlinx_coroutines_flow_StateFlow_value_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow {
}
extension ExportedKotlinPackages.kotlinx.coroutines.`internal`.ThreadSafeHeapNode where Self : KotlinRuntimeSupport._KotlinBridgeable {
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
extension ExportedKotlinPackages.kotlinx.coroutines.`internal`.ThreadSafeHeapNode {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.CancellableContinuation where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._CancellableContinuation {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi) @available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ChildHandle")
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.ChildHandle where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._ChildHandle {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi) @available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ChildJob")
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.ChildJob where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._ChildJob {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._CompletableDeferred {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.CompletableJob where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._CompletableJob {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.CoroutineExceptionHandler where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._CoroutineExceptionHandler {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._CoroutineScope {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.Deferred where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._Deferred {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.Delay where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._Delay {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._DisposableHandle {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.Job where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._Job {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi) @available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.ParentJob")
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.ParentJob where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._ParentJob {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.Runnable where Wrapped : ExportedKotlinPackages.kotlinx.coroutines._Runnable {
}
@_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.channels._BroadcastChannel {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.channels.Channel where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.channels._Channel {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelIterator where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.channels._ChannelIterator {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.channels._ProducerScope {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.channels._ReceiveChannel {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.channels._SendChannel {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinCoroutineSupport.KotlinFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._Flow {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._FlowCollector {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow, KotlinCoroutineSupport.KotlinMutableSharedFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._MutableSharedFlow {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow, KotlinCoroutineSupport.KotlinMutableStateFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._MutableStateFlow {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow, KotlinCoroutineSupport.KotlinSharedFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._SharedFlow {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._SharingStarted {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow, KotlinCoroutineSupport.KotlinStateFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._StateFlow {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.FusibleFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`._FusibleFlow {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectBuilder where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.selects._SelectBuilder {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0 where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause0 {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause1 where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause1 {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2 where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause2 {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.selects._SelectInstance {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.`internal`.MainDispatcherFactory where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.`internal`._MainDispatcherFactory {
}
@_spi(kotlinx$coroutines$InternalCoroutinesApi)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.`internal`.ThreadSafeHeapNode where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.`internal`._ThreadSafeHeapNode {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.sync._Mutex {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.sync.Semaphore where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.sync._Semaphore {
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
    }
    @available(*, deprecated, message: "BroadcastChannel is deprecated in the favour of SharedFlow and is no longer supported") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public protocol BroadcastChannel: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel {
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        func cancel(
            cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
        ) -> Swift.Void
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        func openSubscription() -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
    }
    public protocol Channel: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel, ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel {
    }
    public protocol ChannelIterator: KotlinRuntime.KotlinBase {
        func hasNext() async throws -> Swift.Bool
        func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    }
    public protocol ProducerScope: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope, ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel {
        var channel: any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel {
            get
        }
    }
    public protocol ReceiveChannel: KotlinRuntime.KotlinBase {
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
    public protocol SendChannel: KotlinRuntime.KotlinBase {
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
    @objc(_BroadcastChannel)
    package protocol _BroadcastChannel: ExportedKotlinPackages.kotlinx.coroutines.channels._SendChannel {
    }
    @objc(_Channel)
    package protocol _Channel: ExportedKotlinPackages.kotlinx.coroutines.channels._SendChannel, ExportedKotlinPackages.kotlinx.coroutines.channels._ReceiveChannel {
    }
    @objc(_ChannelIterator)
    package protocol _ChannelIterator {
    }
    @objc(_ProducerScope)
    package protocol _ProducerScope: ExportedKotlinPackages.kotlinx.coroutines._CoroutineScope, ExportedKotlinPackages.kotlinx.coroutines.channels._SendChannel {
    }
    @objc(_ReceiveChannel)
    package protocol _ReceiveChannel {
    }
    @objc(_SendChannel)
    package protocol _SendChannel {
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
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
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
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return kotlinx_coroutines_channels_ChannelResult_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public func exceptionOrNull() -> ExportedKotlinPackages.kotlin.Throwable? {
            return { switch kotlinx_coroutines_channels_ChannelResult_exceptionOrNull(self.__externalRCRef()) { case nil: .none; case let res: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
        }
        public func getOrNull() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlinx_coroutines_channels_ChannelResult_getOrNull(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
        public func getOrThrow() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlinx_coroutines_channels_ChannelResult_getOrThrow(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
        public func hashCode() -> Swift.Int32 {
            return kotlinx_coroutines_channels_ChannelResult_hashCode(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlinx_coroutines_channels_ChannelResult_toString(self.__externalRCRef())
        }
    }
    public final class ClosedReceiveChannelException: ExportedKotlinPackages.kotlin.NoSuchElementException {
        public override init(
            message: Swift.String?
        ) {
            if Self.self != ExportedKotlinPackages.kotlinx.coroutines.channels.ClosedReceiveChannelException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlinx.coroutines.channels.ClosedReceiveChannelException ") }
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
            if Self.self != ExportedKotlinPackages.kotlinx.coroutines.channels.ClosedSendChannelException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlinx.coroutines.channels.ClosedSendChannelException ") }
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
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_ConflatedBroadcastChannel_onSend_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause2
            }
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public var value: (any KotlinRuntimeSupport._KotlinBridgeable)? {
            @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
            get {
                return { switch kotlinx_coroutines_channels_ConflatedBroadcastChannel_value_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
            }
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public var valueOrNull: (any KotlinRuntimeSupport._KotlinBridgeable)? {
            @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
            get {
                return { switch kotlinx_coroutines_channels_ConflatedBroadcastChannel_valueOrNull_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
            }
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public init() {
            if Self.self != ExportedKotlinPackages.kotlinx.coroutines.channels.ConflatedBroadcastChannel.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlinx.coroutines.channels.ConflatedBroadcastChannel ") }
            let __kt = kotlinx_coroutines_channels_ConflatedBroadcastChannel_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_channels_ConflatedBroadcastChannel_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public init(
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) {
            if Self.self != ExportedKotlinPackages.kotlinx.coroutines.channels.ConflatedBroadcastChannel.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlinx.coroutines.channels.ConflatedBroadcastChannel ") }
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
                let originalBlock = handler
                return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()); return true }() }
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
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_ConflatedBroadcastChannel_openSubscription(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public func send(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) async throws -> Swift.Void {
            try await {
                try Task.checkCancellation()
                var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
                return try await withTaskCancellationHandler {
                    try await withUnsafeThrowingContinuation { nativeContinuation in
                        withUnsafeCurrentTask { currentTask in
                            let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                            let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                                nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                            }
                            cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                            let _: Bool = kotlinx_coroutines_channels_ConflatedBroadcastChannel_send__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil, {
                                let originalBlock = continuation
                                return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                            }(), {
                                let originalBlock = exception
                                return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                            }(), cancellation.__externalRCRef())
                        }
                    }
                } onCancel: {
                    cancellation?.cancelExternally()
                }
            }()
        }
        @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
        public func trySend(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult {
            return ExportedKotlinPackages.kotlinx.coroutines.channels.ChannelResult.__createClassWrapper(externalRCRef: kotlinx_coroutines_channels_ConflatedBroadcastChannel_trySend__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil))
        }
    }
    public static func awaitClose(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.channels.ProducerScope,
        block: @escaping () -> Swift.Void
    ) async throws -> Swift.Void {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_channels_awaitClose__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_channels_ProducerScope_U2829202D_U20Swift_Void__(receiver.__externalRCRef(), {
                            let originalBlock = block
                            return { return { originalBlock(); return true }() }
                        }(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    @available(*, deprecated, message: "BroadcastChannel is deprecated in the favour of SharedFlow and StateFlow, and is no longer supported") @_spi(kotlinx$coroutines$ObsoleteCoroutinesApi)
    public static func broadcastChannel(
        capacity: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_BroadcastChannel__TypesOfArguments__Swift_Int32__(capacity)) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.BroadcastChannel
    }
    public static func channel(
        capacity: Swift.Int32,
        onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow,
        onUndeliveredElement: (((any KotlinRuntimeSupport._KotlinBridgeable)?) -> Swift.Void)?
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.channels.Channel {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_channels_Channel__TypesOfArguments__Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow_Swift_Optional_U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U29202D_U20Swift_Void___(capacity, onBufferOverflow.__externalRCRef(), onUndeliveredElement.map { it in {
            let originalBlock = it
            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
        }() } ?? nil)) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.Channel
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines {
    public enum CoroutineStart: KotlinRuntimeSupport._KotlinBridgeable, Swift.CaseIterable, Swift.LosslessStringConvertible, Swift.RawRepresentable {
        case DEFAULT
        case LAZY
        case ATOMIC
        case UNDISPATCHED
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
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public var isLazy: Swift.Bool {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return kotlinx_coroutines_CoroutineStart_isLazy_get(self.__externalRCRef())
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
    }
    public typealias CancellationException = ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException
    public typealias CompletionHandler = (ExportedKotlinPackages.kotlin.Throwable?) -> Swift.Void
    public protocol CancellableContinuation: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.coroutines.Continuation {
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
    @available(*, unavailable, message: "This is internal API and may be removed in the future releases") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol ChildJob: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.Job {
    }
    public protocol CompletableDeferred: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.Deferred {
        func complete(
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func completeExceptionally(
            exception: ExportedKotlinPackages.kotlin.Throwable
        ) -> Swift.Bool
    }
    public protocol CompletableJob: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.Job {
        func complete() -> Swift.Bool
        func completeExceptionally(
            exception: ExportedKotlinPackages.kotlin.Throwable
        ) -> Swift.Bool
    }
    public protocol CoroutineExceptionHandler: KotlinRuntime.KotlinBase, KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
        func handleException(
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
            exception: ExportedKotlinPackages.kotlin.Throwable
        ) -> Swift.Void
    }
    public protocol CoroutineScope: KotlinRuntime.KotlinBase {
        var coroutineContext: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
            get
        }
    }
    public protocol Deferred: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.Job {
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
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol Delay: KotlinRuntime.KotlinBase {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func invokeOnTimeout(
            timeMillis: Swift.Int64,
            block: any ExportedKotlinPackages.kotlinx.coroutines.Runnable,
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
    }
    public protocol DisposableHandle: KotlinRuntime.KotlinBase {
        func dispose() -> Swift.Void
    }
    public protocol Job: KotlinRuntime.KotlinBase, KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
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
        func invokeOnCompletion(
            handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func invokeOnCompletion(
            onCancelling: Swift.Bool,
            invokeImmediately: Swift.Bool,
            handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
        func join() async throws -> Swift.Void
        func start() -> Swift.Bool
    }
    @available(*, unavailable, message: "This is internal API and may be removed in the future releases") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol ParentJob: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.Job {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func getChildJobCancellationCause() -> ExportedKotlinPackages.kotlinx.coroutines.CancellationException
    }
    public protocol Runnable: KotlinRuntime.KotlinBase {
        func run() -> Swift.Void
    }
    @objc(_CancellableContinuation)
    package protocol _CancellableContinuation: ExportedKotlinPackages.kotlin.coroutines._Continuation {
    }
    @objc(_ChildHandle)
    package protocol _ChildHandle: ExportedKotlinPackages.kotlinx.coroutines._DisposableHandle {
    }
    @objc(_ChildJob)
    package protocol _ChildJob: ExportedKotlinPackages.kotlinx.coroutines._Job {
    }
    @objc(_CompletableDeferred)
    package protocol _CompletableDeferred: ExportedKotlinPackages.kotlinx.coroutines._Deferred {
    }
    @objc(_CompletableJob)
    package protocol _CompletableJob: ExportedKotlinPackages.kotlinx.coroutines._Job {
    }
    @objc(_CoroutineExceptionHandler)
    package protocol _CoroutineExceptionHandler: KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
    }
    @objc(_CoroutineScope)
    package protocol _CoroutineScope {
    }
    @objc(_Deferred)
    package protocol _Deferred: ExportedKotlinPackages.kotlinx.coroutines._Job {
    }
    @objc(_Delay)
    package protocol _Delay {
    }
    @objc(_DisposableHandle)
    package protocol _DisposableHandle {
    }
    @objc(_Job)
    package protocol _Job: KotlinStdlib.__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element {
    }
    @objc(_ParentJob)
    package protocol _ParentJob: ExportedKotlinPackages.kotlinx.coroutines._Job {
    }
    @objc(_Runnable)
    package protocol _Runnable {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi) @available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.kotlinx.coroutines.JobSupport")
    open class AbstractCoroutine: ExportedKotlinPackages.kotlinx.coroutines.JobSupport, ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope, ExportedKotlinPackages.kotlinx.coroutines._CoroutineScope {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final var context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_AbstractCoroutine_context_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open var coroutineContext: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_AbstractCoroutine_coroutineContext_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open override var isActive: Swift.Bool {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return kotlinx_coroutines_AbstractCoroutine_isActive_get(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        package init(
            parentContext: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
            initParentJob: Swift.Bool,
            active: Swift.Bool
        ) {
            fatalError()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func start(
            start: ExportedKotlinPackages.kotlinx.coroutines.CoroutineStart,
            receiver: (any KotlinRuntimeSupport._KotlinBridgeable)?,
            block: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void {
            return { kotlinx_coroutines_AbstractCoroutine_start__TypesOfArguments__ExportedKotlinPackages_kotlinx_coroutines_CoroutineStart_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), start.__externalRCRef(), receiver.map { it in it.__externalRCRef() } ?? nil, {
                let originalBlock = block
                return { (arg0: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                    let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
                return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
            }()
                    let __exception: (Swift.Error) -> Swift.Void = {
                let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
                return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
            }()
                    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                            let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                    let task = Task {
                        await withTaskCancellationHandler {
                            do {
                                let result = try await originalBlock(__wrapped_arg0)
                                __continuation(result)
                            } catch {
                                __exception(error)
                            }
                        } onCancel: {
                            __cancellation.cancelExternally()
                        }
                    }
                    __cancellation.setCallback { shouldCancel in
                        defer { if shouldCancel { task.cancel() } }
                        return task.isCancelled
                    }
                }
            }()); return () }()
        }
    }
    open class CloseableCoroutineDispatcher: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher {
        package override init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        open func close() -> Swift.Void {
            return { kotlinx_coroutines_CloseableCoroutineDispatcher_close(self.__externalRCRef()); return () }()
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public final class CompletionHandlerException: ExportedKotlinPackages.kotlin.RuntimeException {
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public init(
            message: Swift.String,
            cause: ExportedKotlinPackages.kotlin.Throwable
        ) {
            if Self.self != ExportedKotlinPackages.kotlinx.coroutines.CompletionHandlerException.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlinx.coroutines.CompletionHandlerException ") }
            let __kt = kotlinx_coroutines_CompletionHandlerException_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_CompletionHandlerException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_ExportedKotlinPackages_kotlin_Throwable__(__kt, message, cause.__externalRCRef()); return () }()
        }
    }
    open class CoroutineDispatcher: ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextElement, ExportedKotlinPackages.kotlin.coroutines.ContinuationInterceptor, ExportedKotlinPackages.kotlin.coroutines._ContinuationInterceptor {
        @_spi(kotlin$ExperimentalStdlibApi)
        public final class Key: ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextKey {
            @_spi(kotlin$ExperimentalStdlibApi)
            public static var shared: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.Key {
                @_spi(kotlin$ExperimentalStdlibApi)
                get {
                    return ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.Key.__createClassWrapper(externalRCRef: kotlinx_coroutines_CoroutineDispatcher_Key_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
        }
        package init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @available(*, unavailable, message: "Operator '+' on two CoroutineDispatcher objects is meaningless. CoroutineDispatcher is a coroutine context element and `+` is a set-sum operator for coroutine contexts. The dispatcher to the right of `+` just replaces the dispatcher to the left.")
        public static func +(
            this: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher,
            other: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher
        ) -> ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher {
            fatalError()
        }
        @available(*, unavailable, message: "Operator '+' on two CoroutineDispatcher objects is meaningless. CoroutineDispatcher is a coroutine context element and `+` is a set-sum operator for coroutine contexts. The dispatcher to the right of `+` just replaces the dispatcher to the left.")
        public final func _plus(
            other: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher
        ) -> ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher {
            fatalError()
        }
        open func dispatch(
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
            block: any ExportedKotlinPackages.kotlinx.coroutines.Runnable
        ) -> Swift.Void {
            return { kotlinx_coroutines_CoroutineDispatcher_dispatch__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_anyU20ExportedKotlinPackages_kotlinx_coroutines_Runnable__(self.__externalRCRef(), context.__externalRCRef(), block.__externalRCRef()); return () }()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func dispatchYield(
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
            block: any ExportedKotlinPackages.kotlinx.coroutines.Runnable
        ) -> Swift.Void {
            return { kotlinx_coroutines_CoroutineDispatcher_dispatchYield__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_anyU20ExportedKotlinPackages_kotlinx_coroutines_Runnable__(self.__externalRCRef(), context.__externalRCRef(), block.__externalRCRef()); return () }()
        }
        open func isDispatchNeeded(
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
        ) -> Swift.Bool {
            return kotlinx_coroutines_CoroutineDispatcher_isDispatchNeeded__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(self.__externalRCRef(), context.__externalRCRef())
        }
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        open func limitedParallelism(
            parallelism: Swift.Int32
        ) -> ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher {
            return ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: kotlinx_coroutines_CoroutineDispatcher_limitedParallelism__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), parallelism))
        }
        public final func releaseInterceptedContinuation(
            continuation: any ExportedKotlinPackages.kotlin.coroutines.Continuation
        ) -> Swift.Void {
            return { kotlinx_coroutines_CoroutineDispatcher_releaseInterceptedContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation__(self.__externalRCRef(), continuation.__externalRCRef()); return () }()
        }
        open func toString() -> Swift.String {
            return kotlinx_coroutines_CoroutineDispatcher_toString(self.__externalRCRef())
        }
    }
    public final class CoroutineName: ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextElement {
        public final class Key: KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.kotlinx.coroutines.CoroutineName.Key {
                get {
                    return ExportedKotlinPackages.kotlinx.coroutines.CoroutineName.Key.__createClassWrapper(externalRCRef: kotlinx_coroutines_CoroutineName_Key_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
        }
        public var name: Swift.String {
            get {
                return kotlinx_coroutines_CoroutineName_name_get(self.__externalRCRef())
            }
        }
        public init(
            name: Swift.String
        ) {
            if Self.self != ExportedKotlinPackages.kotlinx.coroutines.CoroutineName.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlinx.coroutines.CoroutineName ") }
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
        public static func ==(
            this: ExportedKotlinPackages.kotlinx.coroutines.CoroutineName,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
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
        public func hashCode() -> Swift.Int32 {
            return kotlinx_coroutines_CoroutineName_hashCode(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlinx_coroutines_CoroutineName_toString(self.__externalRCRef())
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
        private init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
    public final class GlobalScope: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope, ExportedKotlinPackages.kotlinx.coroutines._CoroutineScope {
        @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
        public var coroutineContext: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
            @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_GlobalScope_coroutineContext_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
            }
        }
        @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
        public static var shared: ExportedKotlinPackages.kotlinx.coroutines.GlobalScope {
            @_spi(kotlinx$coroutines$DelicateCoroutinesApi)
            get {
                return ExportedKotlinPackages.kotlinx.coroutines.GlobalScope.__createClassWrapper(externalRCRef: kotlinx_coroutines_GlobalScope_get())
            }
        }
        private init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @available(*, unavailable, message: "This is internal API and may be removed in the future releases") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    open class JobSupport: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.Job, ExportedKotlinPackages.kotlinx.coroutines._Job, ExportedKotlinPackages.kotlinx.coroutines.ChildJob, ExportedKotlinPackages.kotlinx.coroutines._ChildJob, ExportedKotlinPackages.kotlinx.coroutines.ParentJob, ExportedKotlinPackages.kotlinx.coroutines._ParentJob {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final var children: any ExportedKotlinPackages.kotlin.sequences.Sequence {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_JobSupport_children_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.sequences.Sequence
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
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_JobSupport_key_get(self.__externalRCRef())) as! any KotlinStdlib._ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final var onJoin: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0 {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_JobSupport_onJoin_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0
            }
        }
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi) @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open var parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)? {
            @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi) @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return { switch kotlinx_coroutines_JobSupport_parent_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res) as! any ExportedKotlinPackages.kotlinx.coroutines.Job; } }()
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public init(
            active: Swift.Bool
        ) {
            if Self.self != ExportedKotlinPackages.kotlinx.coroutines.JobSupport.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlinx.coroutines.JobSupport ") }
            let __kt = kotlinx_coroutines_JobSupport_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_JobSupport_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(__kt, active); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
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
            return { switch kotlinx_coroutines_JobSupport_getCompletionExceptionOrNull(self.__externalRCRef()) { case nil: .none; case let res: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func invokeOnCompletion(
            handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_JobSupport_invokeOnCompletion__TypesOfArguments__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), {
                let originalBlock = handler
                return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()); return true }() }
            }())) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func invokeOnCompletion(
            onCancelling: Swift.Bool,
            invokeImmediately: Swift.Bool,
            handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_JobSupport_invokeOnCompletion__TypesOfArguments__Swift_Bool_Swift_Bool_U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), onCancelling, invokeImmediately, {
                let originalBlock = handler
                return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()); return true }() }
            }())) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func join() async throws -> Swift.Void {
            try await {
                try Task.checkCancellation()
                var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
                return try await withTaskCancellationHandler {
                    try await withUnsafeThrowingContinuation { nativeContinuation in
                        withUnsafeCurrentTask { currentTask in
                            let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                            let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                                nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                            }
                            cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                            let _: Bool = kotlinx_coroutines_JobSupport_join(self.__externalRCRef(), {
                                let originalBlock = continuation
                                return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                            }(), {
                                let originalBlock = exception
                                return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                            }(), cancellation.__externalRCRef())
                        }
                    }
                } onCancel: {
                    cancellation?.cancelExternally()
                }
            }()
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
    }
    open class MainCoroutineDispatcher: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher {
        open var immediate: ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher {
            get {
                return ExportedKotlinPackages.kotlinx.coroutines.MainCoroutineDispatcher.__createClassWrapper(externalRCRef: kotlinx_coroutines_MainCoroutineDispatcher_immediate_get(self.__externalRCRef()))
            }
        }
        package override init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        open override func limitedParallelism(
            parallelism: Swift.Int32
        ) -> ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher {
            return ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher.__createClassWrapper(externalRCRef: kotlinx_coroutines_MainCoroutineDispatcher_limitedParallelism__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), parallelism))
        }
        open override func toString() -> Swift.String {
            return kotlinx_coroutines_MainCoroutineDispatcher_toString(self.__externalRCRef())
        }
    }
    public final class NonCancellable: ExportedKotlinPackages.kotlin.coroutines.AbstractCoroutineContextElement, ExportedKotlinPackages.kotlinx.coroutines.Job, ExportedKotlinPackages.kotlinx.coroutines._Job {
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")
        public var children: any ExportedKotlinPackages.kotlin.sequences.Sequence {
            get {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_NonCancellable_children_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.sequences.Sequence
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
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_NonCancellable_onJoin_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0
            }
        }
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited") @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        public var parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)? {
            @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
            get {
                return { switch kotlinx_coroutines_NonCancellable_parent_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res) as! any ExportedKotlinPackages.kotlinx.coroutines.Job; } }()
            }
        }
        public static var shared: ExportedKotlinPackages.kotlinx.coroutines.NonCancellable {
            get {
                return ExportedKotlinPackages.kotlinx.coroutines.NonCancellable.__createClassWrapper(externalRCRef: kotlinx_coroutines_NonCancellable_get())
            }
        }
        private init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
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
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")
        public func invokeOnCompletion(
            handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_NonCancellable_invokeOnCompletion__TypesOfArguments__U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), {
                let originalBlock = handler
                return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()); return true }() }
            }())) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
        }
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited") @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public func invokeOnCompletion(
            onCancelling: Swift.Bool,
            invokeImmediately: Swift.Bool,
            handler: @escaping ExportedKotlinPackages.kotlinx.coroutines.CompletionHandler
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_NonCancellable_invokeOnCompletion__TypesOfArguments__Swift_Bool_Swift_Bool_U28Swift_Optional_ExportedKotlinPackages_kotlin_Throwable_U29202D_U20Swift_Void__(self.__externalRCRef(), onCancelling, invokeImmediately, {
                let originalBlock = handler
                return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: res); } }()); return true }() }
            }())) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
        }
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")
        public func join() async throws -> Swift.Void {
            try await {
                try Task.checkCancellation()
                var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
                return try await withTaskCancellationHandler {
                    try await withUnsafeThrowingContinuation { nativeContinuation in
                        withUnsafeCurrentTask { currentTask in
                            let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                            let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                                nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                            }
                            cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                            let _: Bool = kotlinx_coroutines_NonCancellable_join(self.__externalRCRef(), {
                                let originalBlock = continuation
                                return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                            }(), {
                                let originalBlock = exception
                                return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                            }(), cancellation.__externalRCRef())
                        }
                    }
                } onCancel: {
                    cancellation?.cancelExternally()
                }
            }()
        }
        @available(*, deprecated, message: "NonCancellable can be used only as an argument for 'withContext', direct usages of its API are prohibited")
        public func start() -> Swift.Bool {
            return kotlinx_coroutines_NonCancellable_start(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return kotlinx_coroutines_NonCancellable_toString(self.__externalRCRef())
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public final class NonDisposableHandle: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle, ExportedKotlinPackages.kotlinx.coroutines._DisposableHandle, ExportedKotlinPackages.kotlinx.coroutines._ChildHandle {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public var parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)? {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return { switch kotlinx_coroutines_NonDisposableHandle_parent_get(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res) as! any ExportedKotlinPackages.kotlinx.coroutines.Job; } }()
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public static var shared: ExportedKotlinPackages.kotlinx.coroutines.NonDisposableHandle {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return ExportedKotlinPackages.kotlinx.coroutines.NonDisposableHandle.__createClassWrapper(externalRCRef: kotlinx_coroutines_NonDisposableHandle_get())
            }
        }
        private init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
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
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public final class TimeoutCancellationException: ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException {
    }
    public static func MainScope() -> any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_MainScope()) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
    }
    public static func SupervisorJob(
        parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)?
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.CompletableJob {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_SupervisorJob__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_kotlinx_coroutines_Job___(parent.map { it in it.__externalRCRef() } ?? nil)) as! any ExportedKotlinPackages.kotlinx.coroutines.CompletableJob
    }
    public static func async(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        start: ExportedKotlinPackages.kotlinx.coroutines.CoroutineStart,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.Deferred {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_async__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ExportedKotlinPackages_kotlinx_coroutines_CoroutineStart_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.__externalRCRef(), context.__externalRCRef(), start.__externalRCRef(), {
            let originalBlock = block
            return { (arg0: Swift.UnsafeMutableRawPointer, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.Deferred
    }
    public static func awaitCancellation() async throws -> Swift.Never {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Never) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_awaitCancellation({
                            let originalBlock = continuation
                            return { (arg0: Swift.Bool) in return { originalBlock({ arg0; fatalError() }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func cancel(
        _ receiver: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
    ) -> Swift.Void {
        return { kotlinx_coroutines_cancel__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException___(receiver.__externalRCRef(), cause.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    public static func cancel(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        cause: ExportedKotlinPackages.kotlinx.coroutines.CancellationException?
    ) -> Swift.Void {
        return { kotlinx_coroutines_cancel__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_Swift_Optional_ExportedKotlinPackages_kotlin_coroutines_cancellation_CancellationException___(receiver.__externalRCRef(), cause.map { it in it.__externalRCRef() } ?? nil); return () }()
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
    public static func cancelAndJoin(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.Job
    ) async throws -> Swift.Void {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_cancelAndJoin__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_Job__(receiver.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
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
    public static func cancellationException(
        message: Swift.String?,
        cause: ExportedKotlinPackages.kotlin.Throwable?
    ) -> ExportedKotlinPackages.kotlinx.coroutines.CancellationException {
        return ExportedKotlinPackages.kotlin.coroutines.cancellation.CancellationException.__createClassWrapper(externalRCRef: kotlinx_coroutines_CancellationException__TypesOfArguments__Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(message ?? nil, cause.map { it in it.__externalRCRef() } ?? nil))
    }
    public static func completableDeferred(
        parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)?
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_CompletableDeferred__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_kotlinx_coroutines_Job___(parent.map { it in it.__externalRCRef() } ?? nil)) as! any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred
    }
    public static func completableDeferred(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_CompletableDeferred__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(value.map { it in it.__externalRCRef() } ?? nil)) as! any ExportedKotlinPackages.kotlinx.coroutines.CompletableDeferred
    }
    public static func coroutineExceptionHandler(
        handler: @escaping (any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext, ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.CoroutineExceptionHandler {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_CoroutineExceptionHandler__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_U20ExportedKotlinPackages_kotlin_ThrowableU29202D_U20Swift_Void__({
            let originalBlock = handler
            return { (arg0: Swift.UnsafeMutableRawPointer, arg1: Swift.UnsafeMutableRawPointer) in return { originalBlock(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext, ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: arg1)); return true }() }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineExceptionHandler
    }
    public static func coroutineScope(
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_coroutineScope__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___({
                            let originalBlock = block
                            return { (arg0: Swift.UnsafeMutableRawPointer, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                        }()
                                let __exception: (Swift.Error) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                        }()
                                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                                        let __wrapped_arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                                let task = Task {
                                    await withTaskCancellationHandler {
                                        do {
                                            let result = try await originalBlock(__wrapped_arg0)
                                            __continuation(result)
                                        } catch {
                                            __exception(error)
                                        }
                                    } onCancel: {
                                        __cancellation.cancelExternally()
                                    }
                                }
                                __cancellation.setCallback { shouldCancel in
                                    defer { if shouldCancel { task.cancel() } }
                                    return task.isCancelled
                                }
                            }
                        }(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func coroutineScope(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_CoroutineScope__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(context.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
    }
    public static func currentCoroutineContext() async throws -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_currentCoroutineContext({
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer) in return { originalBlock(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func delay(
        duration: ExportedKotlinPackages.kotlin.time.Duration
    ) async throws -> Swift.Void {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_delay__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__(duration.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func delay(
        timeMillis: Swift.Int64
    ) async throws -> Swift.Void {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_delay__TypesOfArguments__Swift_Int64__(timeMillis, {
                            let originalBlock = continuation
                            return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func disposableHandle(
        function: @escaping () -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_DisposableHandle__TypesOfArguments__U2829202D_U20Swift_Void__({
            let originalBlock = function
            return { return { originalBlock(); return true }() }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.DisposableHandle
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
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_job_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(receiver.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.Job
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public static func handleCoroutineException(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        exception: ExportedKotlinPackages.kotlin.Throwable
    ) -> Swift.Void {
        return { kotlinx_coroutines_handleCoroutineException__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ExportedKotlinPackages_kotlin_Throwable__(context.__externalRCRef(), exception.__externalRCRef()); return () }()
    }
    public static func invoke(
        _ receiver: ExportedKotlinPackages.kotlinx.coroutines.CoroutineDispatcher,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_invoke__TypesOfArgumentsE__ExportedKotlinPackages_kotlinx_coroutines_CoroutineDispatcher_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.__externalRCRef(), {
                            let originalBlock = block
                            return { (arg0: Swift.UnsafeMutableRawPointer, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                        }()
                                let __exception: (Swift.Error) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                        }()
                                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                                        let __wrapped_arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                                let task = Task {
                                    await withTaskCancellationHandler {
                                        do {
                                            let result = try await originalBlock(__wrapped_arg0)
                                            __continuation(result)
                                        } catch {
                                            __exception(error)
                                        }
                                    } onCancel: {
                                        __cancellation.cancelExternally()
                                    }
                                }
                                __cancellation.setCallback { shouldCancel in
                                    defer { if shouldCancel { task.cancel() } }
                                    return task.isCancelled
                                }
                            }
                        }(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func job(
        parent: (any ExportedKotlinPackages.kotlinx.coroutines.Job)?
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.CompletableJob {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_Job__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_kotlinx_coroutines_Job___(parent.map { it in it.__externalRCRef() } ?? nil)) as! any ExportedKotlinPackages.kotlinx.coroutines.CompletableJob
    }
    public static func joinAll(
        jobs: any ExportedKotlinPackages.kotlinx.coroutines.Job...
    ) async throws -> Swift.Void {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_joinAll__TypesOfArguments__Swift_Array_anyU20ExportedKotlinPackages_kotlinx_coroutines_Job__Vararg___(jobs, {
                            let originalBlock = continuation
                            return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func launch(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        start: ExportedKotlinPackages.kotlinx.coroutines.CoroutineStart,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.Job {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_launch__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ExportedKotlinPackages_kotlinx_coroutines_CoroutineStart_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Void__(receiver.__externalRCRef(), context.__externalRCRef(), start.__externalRCRef(), {
            let originalBlock = block
            return { (arg0: Swift.UnsafeMutableRawPointer, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Void) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.Job
    }
    public static func newCoroutineContext(
        _ receiver: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        addedContext: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_newCoroutineContext__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(receiver.__externalRCRef(), addedContext.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
    public static func newCoroutineContext(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_newCoroutineContext__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(receiver.__externalRCRef(), context.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
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
    public static func plus(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_plus__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(receiver.__externalRCRef(), context.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
    }
    public static func runBlocking(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_runBlocking__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(context.__externalRCRef(), {
            let originalBlock = block
            return { (arg0: Swift.UnsafeMutableRawPointer, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public static func runnable(
        block: @escaping () -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.Runnable {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_Runnable__TypesOfArguments__U2829202D_U20Swift_Void__({
            let originalBlock = block
            return { return { originalBlock(); return true }() }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.Runnable
    }
    public static func supervisorScope(
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_supervisorScope__TypesOfArguments__U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___({
                            let originalBlock = block
                            return { (arg0: Swift.UnsafeMutableRawPointer, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                        }()
                                let __exception: (Swift.Error) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                        }()
                                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                                        let __wrapped_arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                                let task = Task {
                                    await withTaskCancellationHandler {
                                        do {
                                            let result = try await originalBlock(__wrapped_arg0)
                                            __continuation(result)
                                        } catch {
                                            __exception(error)
                                        }
                                    } onCancel: {
                                        __cancellation.cancelExternally()
                                    }
                                }
                                __cancellation.setCallback { shouldCancel in
                                    defer { if shouldCancel { task.cancel() } }
                                    return task.isCancelled
                                }
                            }
                        }(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func withContext(
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_withContext__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(context.__externalRCRef(), {
                            let originalBlock = block
                            return { (arg0: Swift.UnsafeMutableRawPointer, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                        }()
                                let __exception: (Swift.Error) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                        }()
                                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                                        let __wrapped_arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                                let task = Task {
                                    await withTaskCancellationHandler {
                                        do {
                                            let result = try await originalBlock(__wrapped_arg0)
                                            __continuation(result)
                                        } catch {
                                            __exception(error)
                                        }
                                    } onCancel: {
                                        __cancellation.cancelExternally()
                                    }
                                }
                                __cancellation.setCallback { shouldCancel in
                                    defer { if shouldCancel { task.cancel() } }
                                    return task.isCancelled
                                }
                            }
                        }(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func withTimeout(
        timeMillis: Swift.Int64,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_withTimeout__TypesOfArguments__Swift_Int64_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(timeMillis, {
                            let originalBlock = block
                            return { (arg0: Swift.UnsafeMutableRawPointer, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                        }()
                                let __exception: (Swift.Error) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                        }()
                                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                                        let __wrapped_arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                                let task = Task {
                                    await withTaskCancellationHandler {
                                        do {
                                            let result = try await originalBlock(__wrapped_arg0)
                                            __continuation(result)
                                        } catch {
                                            __exception(error)
                                        }
                                    } onCancel: {
                                        __cancellation.cancelExternally()
                                    }
                                }
                                __cancellation.setCallback { shouldCancel in
                                    defer { if shouldCancel { task.cancel() } }
                                    return task.isCancelled
                                }
                            }
                        }(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func withTimeout(
        timeout: ExportedKotlinPackages.kotlin.time.Duration,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_withTimeout__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(timeout.__externalRCRef(), {
                            let originalBlock = block
                            return { (arg0: Swift.UnsafeMutableRawPointer, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                        }()
                                let __exception: (Swift.Error) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                        }()
                                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                                        let __wrapped_arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                                let task = Task {
                                    await withTaskCancellationHandler {
                                        do {
                                            let result = try await originalBlock(__wrapped_arg0)
                                            __continuation(result)
                                        } catch {
                                            __exception(error)
                                        }
                                    } onCancel: {
                                        __cancellation.cancelExternally()
                                    }
                                }
                                __cancellation.setCallback { shouldCancel in
                                    defer { if shouldCancel { task.cancel() } }
                                    return task.isCancelled
                                }
                            }
                        }(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func withTimeoutOrNull(
        timeMillis: Swift.Int64,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_withTimeoutOrNull__TypesOfArguments__Swift_Int64_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(timeMillis, {
                            let originalBlock = block
                            return { (arg0: Swift.UnsafeMutableRawPointer, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                        }()
                                let __exception: (Swift.Error) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                        }()
                                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                                        let __wrapped_arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                                let task = Task {
                                    await withTaskCancellationHandler {
                                        do {
                                            let result = try await originalBlock(__wrapped_arg0)
                                            __continuation(result)
                                        } catch {
                                            __exception(error)
                                        }
                                    } onCancel: {
                                        __cancellation.cancelExternally()
                                    }
                                }
                                __cancellation.setCallback { shouldCancel in
                                    defer { if shouldCancel { task.cancel() } }
                                    return task.isCancelled
                                }
                            }
                        }(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func withTimeoutOrNull(
        timeout: ExportedKotlinPackages.kotlin.time.Duration,
        block: @escaping (any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_withTimeoutOrNull__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration_U28anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScopeU2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(timeout.__externalRCRef(), {
                            let originalBlock = block
                            return { (arg0: Swift.UnsafeMutableRawPointer, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                        }()
                                let __exception: (Swift.Error) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                        }()
                                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                                        let __wrapped_arg0: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
                                let task = Task {
                                    await withTaskCancellationHandler {
                                        do {
                                            let result = try await originalBlock(__wrapped_arg0)
                                            __continuation(result)
                                        } catch {
                                            __exception(error)
                                        }
                                    } onCancel: {
                                        __cancellation.cancelExternally()
                                    }
                                }
                                __cancellation.setCallback { shouldCancel in
                                    defer { if shouldCancel { task.cancel() } }
                                    return task.isCancelled
                                }
                            }
                        }(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func yield() async throws -> Swift.Void {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_yield({
                            let originalBlock = continuation
                            return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
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
    }
    public protocol Flow: KotlinRuntime.KotlinBase, KotlinCoroutineSupport.KotlinFlow {
    }
    public protocol FlowCollector: KotlinRuntime.KotlinBase {
        func emit(
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) async throws -> Swift.Void
    }
    public protocol MutableSharedFlow: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow, ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, KotlinCoroutineSupport.KotlinMutableSharedFlow {
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
    public protocol MutableStateFlow: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow, ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow, KotlinCoroutineSupport.KotlinMutableStateFlow {
        var value: (any KotlinRuntimeSupport._KotlinBridgeable)? {
            get
            set
        }
        func compareAndSet(
            expect: (any KotlinRuntimeSupport._KotlinBridgeable)?,
            update: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
    }
    public protocol SharedFlow: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinCoroutineSupport.KotlinSharedFlow {
        var replayCache: [(any KotlinRuntimeSupport._KotlinBridgeable)?] {
            get
        }
    }
    public protocol SharingStarted: KotlinRuntime.KotlinBase {
        func command(
            subscriptionCount: any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Int32>
        ) -> any KotlinCoroutineSupport.KotlinTypedFlow<ExportedKotlinPackages.kotlinx.coroutines.flow.SharingCommand>
    }
    public protocol StateFlow: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow, KotlinCoroutineSupport.KotlinStateFlow {
        var value: (any KotlinRuntimeSupport._KotlinBridgeable)? {
            get
        }
    }
    @objc(_Flow)
    package protocol _Flow {
    }
    @objc(_FlowCollector)
    package protocol _FlowCollector {
    }
    @objc(_MutableSharedFlow)
    package protocol _MutableSharedFlow: ExportedKotlinPackages.kotlinx.coroutines.flow._SharedFlow, ExportedKotlinPackages.kotlinx.coroutines.flow._FlowCollector {
    }
    @objc(_MutableStateFlow)
    package protocol _MutableStateFlow: ExportedKotlinPackages.kotlinx.coroutines.flow._StateFlow, ExportedKotlinPackages.kotlinx.coroutines.flow._MutableSharedFlow {
    }
    @objc(_SharedFlow)
    package protocol _SharedFlow: ExportedKotlinPackages.kotlinx.coroutines.flow._Flow {
    }
    @objc(_SharingStarted)
    package protocol _SharingStarted {
    }
    @objc(_StateFlow)
    package protocol _StateFlow: ExportedKotlinPackages.kotlinx.coroutines.flow._SharedFlow {
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    open class AbstractFlow: KotlinRuntime.KotlinBase {
        @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
        package init() {
            fatalError()
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
    public static func WhileSubscribed(
        _ receiver: KotlinxCoroutinesCore._ExportedKotlinPackages_kotlinx_coroutines_flow_SharingStarted_Companion,
        stopTimeout: ExportedKotlinPackages.kotlin.time.Duration,
        replayExpiration: ExportedKotlinPackages.kotlin.time.Duration
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_WhileSubscribed__TypesOfArgumentsE__KotlinxCoroutinesCore__ExportedKotlinPackages_kotlinx_coroutines_flow_SharingStarted_Companion_ExportedKotlinPackages_kotlin_time_Duration_ExportedKotlinPackages_kotlin_time_Duration__(receiver.__externalRCRef(), stopTimeout.__externalRCRef(), replayExpiration.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted
    }
    public static func asFlow(
        _ receiver: @escaping () -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__U2829202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___({
            let originalBlock = receiver
            return { return originalBlock().map { it in it.__externalRCRef() } ?? nil }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func asFlow(
        _ receiver: ExportedKotlinPackages.kotlin.IntArray
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Int32> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Int32>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__ExportedKotlinPackages_kotlin_IntArray__(receiver.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func asFlow(
        _ receiver: ExportedKotlinPackages.kotlin.LongArray
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Int64> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Int64>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__ExportedKotlinPackages_kotlin_LongArray__(receiver.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func asFlow(
        _ receiver: Swift.ClosedRange<Swift.Int32>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Int32> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Int32>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__Swift_ClosedRange_Swift_Int32___(kotlin_ranges_intRange_create_int_KotlinxCoroutinesCore(receiver.lowerBound, receiver.upperBound))) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func asFlow(
        _ receiver: Swift.ClosedRange<Swift.Int64>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Int64> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Int64>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__Swift_ClosedRange_Swift_Int64___(kotlin_ranges_longRange_create_long_KotlinxCoroutinesCore(receiver.lowerBound, receiver.upperBound))) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func asFlow(
        _ receiver: @escaping () async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asFlow__TypesOfArgumentsE__U282920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___({
            let originalBlock = receiver
            return { (__continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)

                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock()
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func asSharedFlow(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedMutableSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedSharedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asSharedFlow__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedMutableSharedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow)
    }
    public static func asStateFlow(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedMutableStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedStateFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_asStateFlow__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedMutableStateFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow)
    }
    public static func buffer(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        capacity: Swift.Int32,
        onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_buffer__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow__(receiver.wrapped.__externalRCRef(), capacity, onBufferOverflow.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
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
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_cancellable__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    @available(*, unavailable, message: "Applying 'cancellable' to a SharedFlow has no effect. See the SharedFlow documentation on Operator Fusion.. Replacement: this")
    public static func cancellable(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    public static func collect(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow
    ) async throws -> Swift.Void {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_collect__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_Flow__(receiver.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func collectIndexed(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        action: @escaping (Swift.Int32, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Void
    ) async throws -> Swift.Void {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_collectIndexed__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Int32_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), {
                            let originalBlock = action
                            return { (arg0: Swift.Int32, arg1: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                                let __continuation: (Swift.Void) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                        }()
                                let __exception: (Swift.Error) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                        }()
                                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                                        let __wrapped_arg0: Swift.Int32 = arg0
                                let __wrapped_arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                                let task = Task {
                                    await withTaskCancellationHandler {
                                        do {
                                            let result = try await originalBlock(__wrapped_arg0, __wrapped_arg1)
                                            __continuation(result)
                                        } catch {
                                            __exception(error)
                                        }
                                    } onCancel: {
                                        __cancellation.cancelExternally()
                                    }
                                }
                                __cancellation.setCallback { shouldCancel in
                                    defer { if shouldCancel { task.cancel() } }
                                    return task.isCancelled
                                }
                            }
                        }(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func collectLatest(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        action: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Void
    ) async throws -> Swift.Void {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_collectLatest__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), {
                            let originalBlock = action
                            return { (arg0: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                                let __continuation: (Swift.Void) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
                        }()
                                let __exception: (Swift.Error) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                        }()
                                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                                let task = Task {
                                    await withTaskCancellationHandler {
                                        do {
                                            let result = try await originalBlock(__wrapped_arg0)
                                            __continuation(result)
                                        } catch {
                                            __exception(error)
                                        }
                                    } onCancel: {
                                        __cancellation.cancelExternally()
                                    }
                                }
                                __cancellation.setCallback { shouldCancel in
                                    defer { if shouldCancel { task.cancel() } }
                                    return task.isCancelled
                                }
                            }
                        }(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func combine(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combine__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), flow.wrapped.__externalRCRef(), {
            let originalBlock = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let __wrapped_arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0, __wrapped_arg1)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func combine(
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow2: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combine__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(flow.wrapped.__externalRCRef(), flow2.wrapped.__externalRCRef(), {
            let originalBlock = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let __wrapped_arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0, __wrapped_arg1)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func combine(
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow2: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow3: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combine__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(flow.wrapped.__externalRCRef(), flow2.wrapped.__externalRCRef(), flow3.wrapped.__externalRCRef(), {
            let originalBlock = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, arg2: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let __wrapped_arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let __wrapped_arg2: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg2 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0, __wrapped_arg1, __wrapped_arg2)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func combine(
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow2: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow3: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow4: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combine__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(flow.wrapped.__externalRCRef(), flow2.wrapped.__externalRCRef(), flow3.wrapped.__externalRCRef(), flow4.wrapped.__externalRCRef(), {
            let originalBlock = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, arg2: Swift.UnsafeMutableRawPointer?, arg3: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let __wrapped_arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let __wrapped_arg2: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg2 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let __wrapped_arg3: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg3 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0, __wrapped_arg1, __wrapped_arg2, __wrapped_arg3)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func combine(
        flow: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow2: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow3: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow4: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        flow5: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_combine__TypesOfArguments__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(flow.wrapped.__externalRCRef(), flow2.wrapped.__externalRCRef(), flow3.wrapped.__externalRCRef(), flow4.wrapped.__externalRCRef(), flow5.wrapped.__externalRCRef(), {
            let originalBlock = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, arg2: Swift.UnsafeMutableRawPointer?, arg3: Swift.UnsafeMutableRawPointer?, arg4: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let __wrapped_arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let __wrapped_arg2: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg2 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let __wrapped_arg3: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg3 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let __wrapped_arg4: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg4 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0, __wrapped_arg1, __wrapped_arg2, __wrapped_arg3, __wrapped_arg4)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    @available(*, unavailable, message: "Flow analogue of 'combineLatest' is 'combine'. Replacement: this.combine(other, transform)")
    public static func combineLatest(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        other: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
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
    @available(*, unavailable, message: "Flow analogue of 'concatWith' is 'onCompletion'. Use 'onCompletion { if (it == null) emitAll(other) }'. Replacement: onCompletion { if (it == null) emitAll(other) }")
    public static func concatWith(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        other: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
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
    public static func conflate(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_conflate__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    @available(*, unavailable, message: "Applying 'conflate' to StateFlow has no effect. See the StateFlow documentation on Operator Fusion.. Replacement: this")
    public static func conflate(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    public static func count(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) async throws -> Swift.Int32 {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Int32) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_count__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.Int32) in return { originalBlock(arg0); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    @available(*, deprecated, message: "SharedFlow never completes, so this terminal operation never completes.")
    public static func count(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) async throws -> Swift.Int32 {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Int32) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_count__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.Int32) in return { originalBlock(arg0); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func count(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        predicate: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Bool
    ) async throws -> Swift.Int32 {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Int32) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_count__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), {
                            let originalBlock = predicate
                            return { (arg0: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                                let __continuation: (Swift.Bool) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                        }()
                                let __exception: (Swift.Error) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                        }()
                                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                                let task = Task {
                                    await withTaskCancellationHandler {
                                        do {
                                            let result = try await originalBlock(__wrapped_arg0)
                                            __continuation(result)
                                        } catch {
                                            __exception(error)
                                        }
                                    } onCancel: {
                                        __cancellation.cancelExternally()
                                    }
                                }
                                __cancellation.setCallback { shouldCancel in
                                    defer { if shouldCancel { task.cancel() } }
                                    return task.isCancelled
                                }
                            }
                        }(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.Int32) in return { originalBlock(arg0); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static func debounce(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        timeout: ExportedKotlinPackages.kotlin.time.Duration
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_debounce__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___ExportedKotlinPackages_kotlin_time_Duration__(receiver.wrapped.__externalRCRef(), timeout.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static func debounce(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        timeout: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) -> ExportedKotlinPackages.kotlin.time.Duration
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_debounce__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U29202D_U20ExportedKotlinPackages_kotlin_time_Duration__(receiver.wrapped.__externalRCRef(), {
            let originalBlock = timeout
            return { (arg0: Swift.UnsafeMutableRawPointer?) in return originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()).__externalRCRef() }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static func debounce(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        timeoutMillis: Swift.Int64
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_debounce__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Int64__(receiver.wrapped.__externalRCRef(), timeoutMillis)) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static func debounce(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        timeoutMillis: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) -> Swift.Int64
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_debounce__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U29202D_U20Swift_Int64__(receiver.wrapped.__externalRCRef(), {
            let originalBlock = timeoutMillis
            return { (arg0: Swift.UnsafeMutableRawPointer?) in return originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()) }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
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
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_distinctUntilChanged__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    @available(*, unavailable, message: "Applying 'distinctUntilChanged' to StateFlow has no effect. See the StateFlow documentation on Operator Fusion.. Replacement: this")
    public static func distinctUntilChanged(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    public static func distinctUntilChanged(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        areEquivalent: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_distinctUntilChanged__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U29202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), {
            let originalBlock = areEquivalent
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?) in return originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }(), { switch arg1 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()) }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func distinctUntilChangedBy(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        keySelector: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_distinctUntilChangedBy__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), {
            let originalBlock = keySelector
            return { (arg0: Swift.UnsafeMutableRawPointer?) in return originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()).map { it in it.__externalRCRef() } ?? nil }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func drop(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        count: Swift.Int32
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_drop__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Int32__(receiver.wrapped.__externalRCRef(), count)) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func dropWhile(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        predicate: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_dropWhile__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), {
            let originalBlock = predicate
            return { (arg0: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Bool) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func emptyFlow() -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_emptyFlow()) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func filter(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        predicate: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_filter__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), {
            let originalBlock = predicate
            return { (arg0: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Bool) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func filterNot(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        predicate: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_filterNot__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), {
            let originalBlock = predicate
            return { (arg0: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Bool) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func filterNotNull(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<any KotlinRuntimeSupport._KotlinBridgeable> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<any KotlinRuntimeSupport._KotlinBridgeable>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_filterNotNull__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func first(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_first__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func first(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        predicate: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Bool
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_first__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), {
                            let originalBlock = predicate
                            return { (arg0: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                                let __continuation: (Swift.Bool) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                        }()
                                let __exception: (Swift.Error) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                        }()
                                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                                let task = Task {
                                    await withTaskCancellationHandler {
                                        do {
                                            let result = try await originalBlock(__wrapped_arg0)
                                            __continuation(result)
                                        } catch {
                                            __exception(error)
                                        }
                                    } onCancel: {
                                        __cancellation.cancelExternally()
                                    }
                                }
                                __cancellation.setCallback { shouldCancel in
                                    defer { if shouldCancel { task.cancel() } }
                                    return task.isCancelled
                                }
                            }
                        }(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func firstOrNull(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_firstOrNull__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func firstOrNull(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        predicate: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Bool
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_firstOrNull__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), {
                            let originalBlock = predicate
                            return { (arg0: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                                let __continuation: (Swift.Bool) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                        }()
                                let __exception: (Swift.Error) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                        }()
                                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                                let task = Task {
                                    await withTaskCancellationHandler {
                                        do {
                                            let result = try await originalBlock(__wrapped_arg0)
                                            __continuation(result)
                                        } catch {
                                            __exception(error)
                                        }
                                    } onCancel: {
                                        __cancellation.cancelExternally()
                                    }
                                }
                                __cancellation.setCallback { shouldCancel in
                                    defer { if shouldCancel { task.cancel() } }
                                    return task.isCancelled
                                }
                            }
                        }(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
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
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flatMapConcat__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
            let originalBlock = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(pointerToBlock.__externalRCRef()!, _1.wrapped.__externalRCRef()); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func flatMapLatest(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flatMapLatest__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
            let originalBlock = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(pointerToBlock.__externalRCRef()!, _1.wrapped.__externalRCRef()); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func flatMapMerge(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        concurrency: Swift.Int32,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flatMapMerge__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Int32_U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), concurrency, {
            let originalBlock = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(pointerToBlock.__externalRCRef()!, _1.wrapped.__externalRCRef()); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
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
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flattenConcat__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____(receiver.wrapped.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func flattenMerge(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>>,
        concurrency: Swift.Int32
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flattenMerge__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____Swift_Int32__(receiver.wrapped.__externalRCRef(), concurrency)) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func flowCollector(
        function: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Void
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_FlowCollector__TypesOfArguments__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Void__({
            let originalBlock = function
            return { (arg0: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Void) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
    }
    public static func flowOf(
        elements: (any KotlinRuntimeSupport._KotlinBridgeable)?...
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flowOf__TypesOfArguments__Swift_Array_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Vararg___(elements.map { it in it as! NSObject? ?? NSNull() })) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func flowOf(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flowOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(value.map { it in it.__externalRCRef() } ?? nil)) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func flowOn(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_flowOn__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(receiver.wrapped.__externalRCRef(), context.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
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
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_fold__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), initial.map { it in it.__externalRCRef() } ?? nil, {
                            let originalBlock = operation
                            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                        }()
                                let __exception: (Swift.Error) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                        }()
                                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                                let __wrapped_arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                                let task = Task {
                                    await withTaskCancellationHandler {
                                        do {
                                            let result = try await originalBlock(__wrapped_arg0, __wrapped_arg1)
                                            __continuation(result)
                                        } catch {
                                            __exception(error)
                                        }
                                    } onCancel: {
                                        __cancellation.cancelExternally()
                                    }
                                }
                                __cancellation.setCallback { shouldCancel in
                                    defer { if shouldCancel { task.cancel() } }
                                    return task.isCancelled
                                }
                            }
                        }(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
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
            let originalBlock = function
            return { (arg0: Swift.UnsafeMutableRawPointer?) in return originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()).map { it in it.__externalRCRef() } ?? nil }
        }()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public static func getCoroutineContext(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
    ) -> any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_coroutineContext_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector__(receiver.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    }
    public static func getIsActive(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
    ) -> Swift.Bool {
        return kotlinx_coroutines_flow_isActive_get__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_FlowCollector__(receiver.__externalRCRef())
    }
    public static func last(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_last__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func lastOrNull(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_lastOrNull__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func launchIn(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        scope: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.Job {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_launchIn__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope__(receiver.wrapped.__externalRCRef(), scope.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.Job
    }
    public static func map(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_map__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), {
            let originalBlock = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public static func mapLatest(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_mapLatest__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), {
            let originalBlock = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func mapNotNull(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<any KotlinRuntimeSupport._KotlinBridgeable> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<any KotlinRuntimeSupport._KotlinBridgeable>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_mapNotNull__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), {
            let originalBlock = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    @available(*, unavailable, message: "Flow analogue of 'merge' is 'flattenConcat'. Replacement: flattenConcat()")
    public static func merge(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    public static func merge(
        flows: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>...
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_merge__TypesOfArguments__Swift_Array_anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____Vararg___(flows)) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func mutableSharedFlow(
        replay: Swift.Int32,
        extraBufferCapacity: Swift.Int32,
        onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow
    ) -> any KotlinCoroutineSupport.KotlinTypedMutableSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedMutableSharedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_MutableSharedFlow__TypesOfArguments__Swift_Int32_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow__(replay, extraBufferCapacity, onBufferOverflow.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow)
    }
    public static func mutableStateFlow(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedMutableStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedMutableStateFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_MutableStateFlow__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(value.map { it in it.__externalRCRef() } ?? nil)) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow)
    }
    @available(*, unavailable, message: "Collect flow in the desired context instead")
    public static func observeOn(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    public static func onEach(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        action: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Void
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_onEach__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Void__(receiver.wrapped.__externalRCRef(), {
            let originalBlock = action
            return { (arg0: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Void) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
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
    public static func produceIn(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        scope: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_produceIn__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope__(receiver.wrapped.__externalRCRef(), scope.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
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
    public static func reduce(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        operation: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_reduce__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), {
                            let originalBlock = operation
                            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
                        }()
                                let __exception: (Swift.Error) -> Swift.Void = {
                            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
                            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
                        }()
                                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                                let __wrapped_arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                                let task = Task {
                                    await withTaskCancellationHandler {
                                        do {
                                            let result = try await originalBlock(__wrapped_arg0, __wrapped_arg1)
                                            __continuation(result)
                                        } catch {
                                            __exception(error)
                                        }
                                    } onCancel: {
                                        __cancellation.cancelExternally()
                                    }
                                }
                                __cancellation.setCallback { shouldCancel in
                                    defer { if shouldCancel { task.cancel() } }
                                    return task.isCancelled
                                }
                            }
                        }(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
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
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_retry__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Int64_U28ExportedKotlinPackages_kotlin_ThrowableU2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), retries, {
            let originalBlock = predicate
            return { (arg0: Swift.UnsafeMutableRawPointer, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Bool) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: ExportedKotlinPackages.kotlin.Throwable = ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: arg0)
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    @available(*, deprecated, message: "SharedFlow never completes, so this operator has no effect.. Replacement: this")
    public static func retry(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        retries: Swift.Int64,
        predicate: @escaping (ExportedKotlinPackages.kotlin.Throwable) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_retry__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Int64_U28ExportedKotlinPackages_kotlin_ThrowableU2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), retries, {
            let originalBlock = predicate
            return { (arg0: Swift.UnsafeMutableRawPointer, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Bool) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: ExportedKotlinPackages.kotlin.Throwable = ExportedKotlinPackages.kotlin.Throwable.__createClassWrapper(externalRCRef: arg0)
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func runningFold(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        initial: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        operation: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_runningFold__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), initial.map { it in it.__externalRCRef() } ?? nil, {
            let originalBlock = operation
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let __wrapped_arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0, __wrapped_arg1)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func runningReduce(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        operation: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_runningReduce__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), {
            let originalBlock = operation
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let __wrapped_arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0, __wrapped_arg1)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static func sample(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        period: ExportedKotlinPackages.kotlin.time.Duration
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_sample__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___ExportedKotlinPackages_kotlin_time_Duration__(receiver.wrapped.__externalRCRef(), period.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static func sample(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        periodMillis: Swift.Int64
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_sample__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Int64__(receiver.wrapped.__externalRCRef(), periodMillis)) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func scan(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        initial: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        operation: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_scan__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), initial.map { it in it.__externalRCRef() } ?? nil, {
            let originalBlock = operation
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let __wrapped_arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0, __wrapped_arg1)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
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
        return KotlinCoroutineSupport._KotlinTypedSharedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_shareIn__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_SharingStarted_Swift_Int32__(receiver.wrapped.__externalRCRef(), scope.__externalRCRef(), started.__externalRCRef(), replay)) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow)
    }
    public static func sharingStarted(
        function: @escaping (any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Int32>) -> any KotlinCoroutineSupport.KotlinTypedFlow<ExportedKotlinPackages.kotlinx.coroutines.flow.SharingCommand>
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_SharingStarted__TypesOfArguments__U28anyU20KotlinCoroutineSupport_KotlinTypedStateFlow_Swift_Int32_U29202D_U20anyU20KotlinCoroutineSupport_KotlinTypedFlow_ExportedKotlinPackages_kotlinx_coroutines_flow_SharingCommand___({
            let originalBlock = function
            return { (arg0: Swift.UnsafeMutableRawPointer) in return originalBlock(KotlinCoroutineSupport._KotlinTypedStateFlowImpl<Swift.Int32>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow)).wrapped.__externalRCRef() }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted
    }
    public static func single(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_single__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func singleOrNull(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_singleOrNull__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    @available(*, unavailable, message: "Flow analogue of 'skip' is 'drop'. Replacement: drop(count)")
    public static func skip(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        count: Swift.Int32
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
    @available(*, unavailable, message: "Flow analogue of 'startWith' is 'onStart'. Use 'onStart { emit(value) }'. Replacement: onStart { emit(value) }")
    public static func startWith(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        fatalError()
    }
    public static func stateIn(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        scope: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
    ) async throws -> any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_stateIn__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope__(receiver.wrapped.__externalRCRef(), scope.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer) in return { originalBlock(KotlinCoroutineSupport._KotlinTypedStateFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow)); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func stateIn(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        scope: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope,
        started: any ExportedKotlinPackages.kotlinx.coroutines.flow.SharingStarted,
        initialValue: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedStateFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_stateIn__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope_anyU20ExportedKotlinPackages_kotlinx_coroutines_flow_SharingStarted_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), scope.__externalRCRef(), started.__externalRCRef(), initialValue.map { it in it.__externalRCRef() } ?? nil)) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow)
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
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_take__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Swift_Int32__(receiver.wrapped.__externalRCRef(), count)) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func takeWhile(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        predicate: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> Swift.Bool
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_takeWhile__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Bool__(receiver.wrapped.__externalRCRef(), {
            let originalBlock = predicate
            return { (arg0: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Bool) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Bool__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    @_spi(kotlinx$coroutines$FlowPreview)
    public static func timeout(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        timeout: ExportedKotlinPackages.kotlin.time.Duration
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_timeout__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___ExportedKotlinPackages_kotlin_time_Duration__(receiver.wrapped.__externalRCRef(), timeout.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func toCollection(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        destination: any ExportedKotlinPackages.kotlin.collections.MutableCollection
    ) async throws -> any ExportedKotlinPackages.kotlin.collections.MutableCollection {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (any ExportedKotlinPackages.kotlin.collections.MutableCollection) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_toCollection__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20ExportedKotlinPackages_kotlin_collections_MutableCollection__(receiver.wrapped.__externalRCRef(), destination.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer) in return { originalBlock(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any ExportedKotlinPackages.kotlin.collections.MutableCollection); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    @available(*, deprecated, message: "SharedFlow never completes, so this terminal operation never completes.")
    public static func toList(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) async throws -> [(any KotlinRuntimeSupport._KotlinBridgeable)?] {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_toList__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Any) in return { originalBlock(arg0 as! Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    @available(*, deprecated, message: "SharedFlow never completes, so this terminal operation never completes.")
    public static func toSet(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedSharedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) async throws -> Swift.Set<Swift.Optional<Swift.AnyHashable>> {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Set<Swift.Optional<Swift.AnyHashable>>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_flow_toSet__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedSharedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef(), {
                            let originalBlock = continuation
                            return { (arg0: Any) in return { originalBlock(arg0 as! Swift.Set<Swift.Optional<Swift.AnyHashable>>); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func update(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedMutableStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        function: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return { kotlinx_coroutines_flow_update__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedMutableStateFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), {
            let originalBlock = function
            return { (arg0: Swift.UnsafeMutableRawPointer?) in return originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()).map { it in it.__externalRCRef() } ?? nil }
        }()); return () }()
    }
    public static func updateAndGet(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedMutableStateFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        function: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_flow_updateAndGet__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedMutableStateFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), {
            let originalBlock = function
            return { (arg0: Swift.UnsafeMutableRawPointer?) in return originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()).map { it in it.__externalRCRef() } ?? nil }
        }()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public static func withIndex(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<ExportedKotlinPackages.kotlin.collections.IndexedValue> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<ExportedKotlinPackages.kotlin.collections.IndexedValue>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_withIndex__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(receiver.wrapped.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
    public static func zip(
        _ receiver: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        other: any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>,
        transform: @escaping ((any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_zip__TypesOfArgumentsE__anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___anyU20KotlinCoroutineSupport_KotlinTypedFlow_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_U2920asyncU20throwsU202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.wrapped.__externalRCRef(), other.wrapped.__externalRCRef(), {
            let originalBlock = transform
            return { (arg0: Swift.UnsafeMutableRawPointer?, arg1: Swift.UnsafeMutableRawPointer?, __continuationPtr: Swift.UnsafeMutableRawPointer, __exceptionPtr: Swift.UnsafeMutableRawPointer, __cancellationPtr: Swift.UnsafeMutableRawPointer) in
                let __continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __continuationPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(pointerToBlock.__externalRCRef()!, _1.map { it in it.__externalRCRef() } ?? nil); return () }() }
        }()
                let __exception: (Swift.Error) -> Swift.Void = {
            let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: __exceptionPtr, options: .asBestFittingWrapper)!
            return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Error__(pointerToBlock.__externalRCRef()!, _1); return () }() }
        }()
                let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: __cancellationPtr)
                        let __wrapped_arg0: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let __wrapped_arg1: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = { switch arg1 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
                let task = Task {
                    await withTaskCancellationHandler {
                        do {
                            let result = try await originalBlock(__wrapped_arg0, __wrapped_arg1)
                            __continuation(result)
                        } catch {
                            __exception(error)
                        }
                    } onCancel: {
                        __cancellation.cancelExternally()
                    }
                }
                __cancellation.setCallback { shouldCancel in
                    defer { if shouldCancel { task.cancel() } }
                    return task.isCancelled
                }
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.`internal` {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public typealias SynchronizedObject = ExportedKotlinPackages.kotlinx.atomicfu.locks.SynchronizedObject
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol MainDispatcherFactory: KotlinRuntime.KotlinBase {
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
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol ThreadSafeHeapNode: KotlinRuntime.KotlinBase {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        var index: Swift.Int32 {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            set
        }
    }
    @objc(_MainDispatcherFactory)
    package protocol _MainDispatcherFactory {
    }
    @objc(_ThreadSafeHeapNode)
    package protocol _ThreadSafeHeapNode {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    open class AtomicOp: ExportedKotlinPackages.kotlinx.coroutines.`internal`.OpDescriptor {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open override var atomicOp: ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp.__createClassWrapper(externalRCRef: kotlinx_coroutines_internal_AtomicOp_atomicOp_get(self.__externalRCRef()))
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        package override init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func complete(
            affected: (any KotlinRuntimeSupport._KotlinBridgeable)?,
            failure: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void {
            return { kotlinx_coroutines_internal_AtomicOp_complete__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), affected.map { it in it.__externalRCRef() } ?? nil, failure.map { it in it.__externalRCRef() } ?? nil); return () }()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final override func perform(
            affected: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlinx_coroutines_internal_AtomicOp_perform__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), affected.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func prepare(
            affected: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlinx_coroutines_internal_AtomicOp_prepare__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), affected.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
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
                return kotlinx_coroutines_internal_LockFreeLinkedListHead_isRemoved_get(self.__externalRCRef())
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public override init() {
            if Self.self != ExportedKotlinPackages.kotlinx.coroutines.`internal`.LockFreeLinkedListHead.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlinx.coroutines.`internal`.LockFreeLinkedListHead ") }
            let __kt = kotlinx_coroutines_internal_LockFreeLinkedListHead_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_internal_LockFreeLinkedListHead_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final func remove() -> Swift.Never {
            return { kotlinx_coroutines_internal_LockFreeLinkedListHead_remove(self.__externalRCRef()); fatalError() }()
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    open class LockFreeLinkedListNode: KotlinRuntime.KotlinBase {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open var isRemoved: Swift.Bool {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return kotlinx_coroutines_internal_LockFreeLinkedListNode_isRemoved_get(self.__externalRCRef())
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
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public init() {
            if Self.self != ExportedKotlinPackages.kotlinx.coroutines.`internal`.LockFreeLinkedListNode.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlinx.coroutines.`internal`.LockFreeLinkedListNode ") }
            let __kt = kotlinx_coroutines_internal_LockFreeLinkedListNode_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlinx_coroutines_internal_LockFreeLinkedListNode_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
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
            return kotlinx_coroutines_internal_LockFreeLinkedListNode_remove(self.__externalRCRef())
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func toString() -> Swift.String {
            return kotlinx_coroutines_internal_LockFreeLinkedListNode_toString(self.__externalRCRef())
        }
    }
    open class OpDescriptor: KotlinRuntime.KotlinBase {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open var atomicOp: ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp? {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return { switch kotlinx_coroutines_internal_OpDescriptor_atomicOp_get(self.__externalRCRef()) { case nil: .none; case let res: ExportedKotlinPackages.kotlinx.coroutines.`internal`.AtomicOp.__createClassWrapper(externalRCRef: res); } }()
            }
        }
        package init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        open func perform(
            affected: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
            return { switch kotlinx_coroutines_internal_OpDescriptor_perform__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), affected.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
        open func toString() -> Swift.String {
            return kotlinx_coroutines_internal_OpDescriptor_toString(self.__externalRCRef())
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public static func synchronized(
        lock: ExportedKotlinPackages.kotlinx.coroutines.`internal`.SynchronizedObject,
        block: @escaping () -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_internal_synchronized__TypesOfArguments__ExportedKotlinPackages_kotlinx_atomicfu_locks_SynchronizedObject_U2829202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(lock.__externalRCRef(), {
            let originalBlock = block
            return { return originalBlock().map { it in it.__externalRCRef() } ?? nil }
        }()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public static func synchronizedImpl(
        lock: ExportedKotlinPackages.kotlinx.coroutines.`internal`.SynchronizedObject,
        block: @escaping () -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlinx_coroutines_internal_synchronizedImpl__TypesOfArguments__ExportedKotlinPackages_kotlinx_atomicfu_locks_SynchronizedObject_U2829202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(lock.__externalRCRef(), {
            let originalBlock = block
            return { return originalBlock().map { it in it.__externalRCRef() } ?? nil }
        }()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.`internal` {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol FusibleFlow: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow.Flow {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func fuse(
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
            capacity: Swift.Int32,
            onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow
        ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    }
    @objc(_FusibleFlow)
    package protocol _FusibleFlow: ExportedKotlinPackages.kotlinx.coroutines.flow._Flow {
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
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_internal_ChannelFlow_context_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext
            }
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public final var onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow {
            @_spi(kotlinx$coroutines$InternalCoroutinesApi)
            get {
                return ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow(__externalRCRefUnsafe: kotlinx_coroutines_flow_internal_ChannelFlow_onBufferOverflow_get(self.__externalRCRef()), options: .asBestFittingWrapper)
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        package init(
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
            capacity: Swift.Int32,
            onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow
        ) {
            fatalError()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func dropChannelOperators() -> (any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>)? {
            return { switch kotlinx_coroutines_flow_internal_ChannelFlow_dropChannelOperators(self.__externalRCRef()) { case nil: .none; case let res: KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow); } }()
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func fuse(
            context: any ExportedKotlinPackages.kotlin.coroutines.CoroutineContext,
            capacity: Swift.Int32,
            onBufferOverflow: ExportedKotlinPackages.kotlinx.coroutines.channels.BufferOverflow
        ) -> any KotlinCoroutineSupport.KotlinTypedFlow<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
            return KotlinCoroutineSupport._KotlinTypedFlowImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_internal_ChannelFlow_fuse__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Swift_Int32_ExportedKotlinPackages_kotlinx_coroutines_channels_BufferOverflow__(self.__externalRCRef(), context.__externalRCRef(), capacity, onBufferOverflow.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow)
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func produceImpl(
            scope: any ExportedKotlinPackages.kotlinx.coroutines.CoroutineScope
        ) -> any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_internal_ChannelFlow_produceImpl__TypesOfArguments__anyU20ExportedKotlinPackages_kotlinx_coroutines_CoroutineScope__(self.__externalRCRef(), scope.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel
        }
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        open func toString() -> Swift.String {
            return kotlinx_coroutines_flow_internal_ChannelFlow_toString(self.__externalRCRef())
        }
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public final class SendingCollector: KotlinRuntime.KotlinBase {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public init(
            channel: any ExportedKotlinPackages.kotlinx.coroutines.channels.SendChannel
        ) {
            if Self.self != ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.SendingCollector.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.kotlinx.coroutines.flow.`internal`.SendingCollector ") }
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
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        public func emit(
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) async throws -> Swift.Void {
            try await {
                try Task.checkCancellation()
                var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
                return try await withTaskCancellationHandler {
                    try await withUnsafeThrowingContinuation { nativeContinuation in
                        withUnsafeCurrentTask { currentTask in
                            let continuation: (Swift.Void) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                            let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                                nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                            }
                            cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                            let _: Bool = kotlinx_coroutines_flow_internal_SendingCollector_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), value.map { it in it.__externalRCRef() } ?? nil, {
                                let originalBlock = continuation
                                return { (arg0: Swift.Bool) in return { originalBlock({ arg0; return () }()); return true }() }
                            }(), {
                                let originalBlock = exception
                                return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                            }(), cancellation.__externalRCRef())
                        }
                    }
                } onCancel: {
                    cancellation?.cancelExternally()
                }
            }()
        }
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.selects {
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public typealias OnCancellationConstructor = (any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) -> (ExportedKotlinPackages.kotlin.Throwable) -> Swift.Void
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public typealias ProcessResultFunction = (any KotlinRuntimeSupport._KotlinBridgeable, (any KotlinRuntimeSupport._KotlinBridgeable)?, (any KotlinRuntimeSupport._KotlinBridgeable)?) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public typealias RegistrationFunction = (any KotlinRuntimeSupport._KotlinBridgeable, any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectInstance, (any KotlinRuntimeSupport._KotlinBridgeable)?) -> Swift.Void
    public protocol SelectBuilder: KotlinRuntime.KotlinBase {
        @_spi(kotlinx$coroutines$InternalCoroutinesApi)
        func invoke(
            _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause0,
            block: @escaping () async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol SelectClause: KotlinRuntime.KotlinBase {
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
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol SelectClause0: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol SelectClause1: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol SelectClause2: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.selects.SelectClause {
    }
    @_spi(kotlinx$coroutines$InternalCoroutinesApi)
    public protocol SelectInstance: KotlinRuntime.KotlinBase {
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
    @objc(_SelectBuilder)
    package protocol _SelectBuilder {
    }
    @objc(_SelectClause)
    package protocol _SelectClause {
    }
    @objc(_SelectClause0)
    package protocol _SelectClause0: ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause {
    }
    @objc(_SelectClause1)
    package protocol _SelectClause1: ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause {
    }
    @objc(_SelectClause2)
    package protocol _SelectClause2: ExportedKotlinPackages.kotlinx.coroutines.selects._SelectClause {
    }
    @objc(_SelectInstance)
    package protocol _SelectInstance {
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.sync {
    public protocol Mutex: KotlinRuntime.KotlinBase {
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
    public protocol Semaphore: KotlinRuntime.KotlinBase {
        var availablePermits: Swift.Int32 {
            get
        }
        func acquire() async throws -> Swift.Void
        func tryAcquire() -> Swift.Bool
    }
    @objc(_Mutex)
    package protocol _Mutex {
    }
    @objc(_Semaphore)
    package protocol _Semaphore {
    }
    public static func mutex(
        locked: Swift.Bool
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_sync_Mutex__TypesOfArguments__Swift_Bool__(locked)) as! any ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex
    }
    public static func semaphore(
        permits: Swift.Int32,
        acquiredPermits: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlinx.coroutines.sync.Semaphore {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_sync_Semaphore__TypesOfArguments__Swift_Int32_Swift_Int32__(permits, acquiredPermits)) as! any ExportedKotlinPackages.kotlinx.coroutines.sync.Semaphore
    }
    public static func withLock(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.sync.Mutex,
        owner: (any KotlinRuntimeSupport._KotlinBridgeable)?,
        action: @escaping () -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_sync_withLock__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_sync_Mutex_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U2829202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.__externalRCRef(), owner.map { it in it.__externalRCRef() } ?? nil, {
                            let originalBlock = action
                            return { return originalBlock().map { it in it.__externalRCRef() } ?? nil }
                        }(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
    public static func withPermit(
        _ receiver: any ExportedKotlinPackages.kotlinx.coroutines.sync.Semaphore,
        action: @escaping () -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) async throws -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        try await {
            try Task.checkCancellation()
            var cancellation: KotlinCoroutineSupport.KotlinTask! = nil
            return try await withTaskCancellationHandler {
                try await withUnsafeThrowingContinuation { nativeContinuation in
                    withUnsafeCurrentTask { currentTask in
                        let continuation: (Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>) -> Swift.Void = { nativeContinuation.resume(returning: $0) }
                        let exception: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = { error in
                            nativeContinuation.resume(throwing: error.map { KotlinError(wrapped: $0) } ?? CancellationError())
                        }
                        cancellation = KotlinCoroutineSupport.KotlinTask(currentTask!)

                        let _: Bool = kotlinx_coroutines_sync_withPermit__TypesOfArgumentsE__anyU20ExportedKotlinPackages_kotlinx_coroutines_sync_Semaphore_U2829202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(receiver.__externalRCRef(), {
                            let originalBlock = action
                            return { return originalBlock().map { it in it.__externalRCRef() } ?? nil }
                        }(), {
                            let originalBlock = continuation
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()); return true }() }
                        }(), {
                            let originalBlock = exception
                            return { (arg0: Swift.UnsafeMutableRawPointer?) in return { originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()); return true }() }
                        }(), cancellation.__externalRCRef())
                    }
                }
            } onCancel: {
                cancellation?.cancelExternally()
            }
        }()
    }
}

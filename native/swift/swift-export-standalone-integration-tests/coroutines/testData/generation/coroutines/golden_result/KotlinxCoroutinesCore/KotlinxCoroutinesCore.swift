@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinxCoroutinesCore
@_exported import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport

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
extension ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var replayCache: [(any KotlinRuntimeSupport._KotlinBridgeable)?] {
        get {
            return kotlinx_coroutines_flow_SharedFlow_replayCache_get(self.__externalRCRef()) as! Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
        }
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow {
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
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinCoroutineSupport.KotlinFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._Flow {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow, KotlinCoroutineSupport.KotlinMutableSharedFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._MutableSharedFlow {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow, KotlinCoroutineSupport.KotlinMutableStateFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._MutableStateFlow {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow, KotlinCoroutineSupport.KotlinSharedFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._SharedFlow {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow, KotlinCoroutineSupport.KotlinStateFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._StateFlow {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._FlowCollector {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow {
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
    @objc(_StateFlow)
    package protocol _StateFlow: ExportedKotlinPackages.kotlinx.coroutines.flow._SharedFlow {
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
}

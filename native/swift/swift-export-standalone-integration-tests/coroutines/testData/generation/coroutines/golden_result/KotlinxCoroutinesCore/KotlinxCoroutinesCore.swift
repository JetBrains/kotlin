@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinxCoroutinesCore
@_exported import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.kotlinx.coroutines.flow.Flow where Self : ExportedKotlinPackages.kotlinx.coroutines.flow.__Flow {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.Flow {
}
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
                let originalBlock: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<KotlinRuntime.KotlinBase> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
                    let _result = originalBlock(_arg0)
                    return { _result; return true }()
                }
            }(), cancellation.__externalRCRef())
        }
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow where Self : ExportedKotlinPackages.kotlinx.coroutines.flow.__MutableSharedFlow {
    public var subscriptionCount: any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Int32> {
        get {
            return KotlinCoroutineSupport._KotlinTypedStateFlowImpl<Swift.Int32>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlinx_coroutines_flow_MutableSharedFlow_subscriptionCount_get(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow)
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
                let originalBlock: (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Void = exception
                return { (arg0: Swift.UnsafeMutableRawPointer?) in
                    let _arg0: Swift.Optional<KotlinRuntime.KotlinBase> = { switch arg0 { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
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
extension ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow {
    @_spi(kotlinx$coroutines$ExperimentalCoroutinesApi)
    public func resetReplayCache() -> Swift.Void {
        fatalError("'resetReplayCache' is an @_spi requirement that must be implemented by Swift conformers")
    }
}
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
extension ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow where Self : ExportedKotlinPackages.kotlinx.coroutines.flow.__SharedFlow {
    public var replayCache: [(any KotlinRuntimeSupport._KotlinBridgeable)?] {
        get {
            return kotlinx_coroutines_flow_SharedFlow_replayCache_get(self.__externalRCRef()) as! Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
        }
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow where Self : ExportedKotlinPackages.kotlinx.coroutines.flow.__StateFlow {
    public var value: (any KotlinRuntimeSupport._KotlinBridgeable)? {
        get {
            return { switch kotlinx_coroutines_flow_StateFlow_value_get(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
    }
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, ExportedKotlinPackages.kotlinx.coroutines.flow.__Flow, KotlinCoroutineSupport.KotlinFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._Flow {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow, ExportedKotlinPackages.kotlinx.coroutines.flow.__MutableSharedFlow, KotlinCoroutineSupport.KotlinMutableSharedFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._MutableSharedFlow {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow, ExportedKotlinPackages.kotlinx.coroutines.flow.__MutableStateFlow, KotlinCoroutineSupport.KotlinMutableStateFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._MutableStateFlow {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow, ExportedKotlinPackages.kotlinx.coroutines.flow.__SharedFlow, KotlinCoroutineSupport.KotlinSharedFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._SharedFlow {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow, ExportedKotlinPackages.kotlinx.coroutines.flow.__StateFlow, KotlinCoroutineSupport.KotlinStateFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._StateFlow {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector, ExportedKotlinPackages.kotlinx.coroutines.flow.__FlowCollector where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._FlowCollector {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.flow._Flow {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.flow._MutableSharedFlow {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.flow._MutableStateFlow {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.flow._SharedFlow {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.flow._StateFlow {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlinx.coroutines.flow._FlowCollector {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow {
    public protocol Flow: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow._Flow, KotlinCoroutineSupport.KotlinFlow {
    }
    public protocol FlowCollector: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow._FlowCollector {
        func emit(
            value: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) async throws -> Swift.Void
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
    public protocol SharedFlow: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, ExportedKotlinPackages.kotlinx.coroutines.flow._SharedFlow, KotlinCoroutineSupport.KotlinSharedFlow {
        var replayCache: [(any KotlinRuntimeSupport._KotlinBridgeable)?] {
            get
        }
    }
    public protocol StateFlow: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow, ExportedKotlinPackages.kotlinx.coroutines.flow._StateFlow, KotlinCoroutineSupport.KotlinStateFlow {
        var value: (any KotlinRuntimeSupport._KotlinBridgeable)? {
            get
        }
    }
    @objc(_Flow)
    public protocol _Flow {
    }
    @objc(_FlowCollector)
    public protocol _FlowCollector {
    }
    @objc(_MutableSharedFlow)
    public protocol _MutableSharedFlow: ExportedKotlinPackages.kotlinx.coroutines.flow._SharedFlow, ExportedKotlinPackages.kotlinx.coroutines.flow._FlowCollector {
    }
    @objc(_MutableStateFlow)
    public protocol _MutableStateFlow: ExportedKotlinPackages.kotlinx.coroutines.flow._StateFlow, ExportedKotlinPackages.kotlinx.coroutines.flow._MutableSharedFlow {
    }
    @objc(_SharedFlow)
    public protocol _SharedFlow: ExportedKotlinPackages.kotlinx.coroutines.flow._Flow {
    }
    @objc(_StateFlow)
    public protocol _StateFlow: ExportedKotlinPackages.kotlinx.coroutines.flow._SharedFlow {
    }
    public protocol __Flow: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol __FlowCollector: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol __MutableSharedFlow: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.flow.__SharedFlow, ExportedKotlinPackages.kotlinx.coroutines.flow.__FlowCollector {
    }
    public protocol __MutableStateFlow: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.flow.__StateFlow, ExportedKotlinPackages.kotlinx.coroutines.flow.__MutableSharedFlow {
    }
    public protocol __SharedFlow: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.flow.__Flow {
    }
    public protocol __StateFlow: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlinx.coroutines.flow.__SharedFlow {
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
                    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1 ?? nil); return () }() }
                }()
                let _cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
                let _result = withKotlinTask(_continuation, _exception, _cancellation){
                    try await originalBlock(_arg0)
                }
                return { _result; return true }()
            }
        }())) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
    }
}
@_cdecl("kotlinx_coroutines_flow_FlowCollector_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_flow_FlowCollector_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.FlowCollector
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1 ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.emit(value: { switch value { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    }
    return true
}

@_cdecl("kotlinx_coroutines_flow_MutableSharedFlow_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_flow_MutableSharedFlow_emit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?, _ continuation: Swift.UnsafeMutableRawPointer, _ exception: Swift.UnsafeMutableRawPointer, _ cancellation: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow
    let __continuation: (Swift.Void) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: continuation, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Void__(pointerToBlock.__externalRCRef()!, { _1; return true }()); return () }() }
}()
    let __exception: (Swift.Optional<Swift.Error>) -> Swift.Void = {
    let pointerToBlock = KotlinRuntime.KotlinBase(__externalRCRefUnsafe: exception, options: .asBestFittingWrapper)!
    return { _1 in return { KotlinxCoroutinesCore_internal_functional_type_caller_SwiftU2EVoid__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_Error___(pointerToBlock.__externalRCRef()!, _1 ?? nil); return () }() }
}()
    let __cancellation: KotlinCoroutineSupport.KotlinTask = KotlinCoroutineSupport.KotlinTask.__createClassWrapper(externalRCRef: cancellation)
    withKotlinTask(__continuation, __exception, __cancellation) {
        try await _self.emit(value: { switch value { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    }
    return true
}

@_cdecl("kotlinx_coroutines_flow_MutableSharedFlow_resetReplayCache__reverse_swift")
package func kotlinx_coroutines_flow_MutableSharedFlow_resetReplayCache__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow
    let _result: Swift.Void = _self.resetReplayCache()
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_flow_MutableSharedFlow_subscriptionCount_get__reverse_swift")
package func kotlinx_coroutines_flow_MutableSharedFlow_subscriptionCount_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow
    let _result: any KotlinCoroutineSupport.KotlinTypedStateFlow<Swift.Int32> = _self.subscriptionCount
    return _result.wrapped.__externalRCRef()
}

@_cdecl("kotlinx_coroutines_flow_MutableSharedFlow_tryEmit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_flow_MutableSharedFlow_tryEmit__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableSharedFlow
    let _result: Swift.Bool = _self.tryEmit(value: { switch value { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlinx_coroutines_flow_MutableStateFlow_compareAndSet__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_flow_MutableStateFlow_compareAndSet__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ expect: Swift.UnsafeMutableRawPointer?, _ update: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow
    let _result: Swift.Bool = _self.compareAndSet(expect: { switch expect { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }(), update: { switch update { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlinx_coroutines_flow_MutableStateFlow_value_get__reverse_swift")
package func kotlinx_coroutines_flow_MutableStateFlow_value_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.value
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlinx_coroutines_flow_MutableStateFlow_value_set__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlinx_coroutines_flow_MutableStateFlow_value_set__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ newValue: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.MutableStateFlow
    let _result: Swift.Void = { _self.value = { switch newValue { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }() }()
    return { _result; return true }()
}

@_cdecl("kotlinx_coroutines_flow_SharedFlow_replayCache_get__reverse_swift")
package func kotlinx_coroutines_flow_SharedFlow_replayCache_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Any {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow
    let _result: Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> = _self.replayCache
    return _result.map { it in it as! NSObject? ?? NSNull() }
}

@_cdecl("kotlinx_coroutines_flow_StateFlow_value_get__reverse_swift")
package func kotlinx_coroutines_flow_StateFlow_value_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.value
    return _result.map { it in it.__externalRCRef() } ?? nil
}

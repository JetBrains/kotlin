@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinxCoroutinesCore
import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.kotlinx.coroutines.flow.Flow where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.Flow {
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
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinCoroutineSupport._KotlinFlow, _Concurrency.AsyncSequence where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._Flow {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.StateFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._StateFlow {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._SharedFlow {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow {
    public protocol Flow: KotlinRuntime.KotlinBase, KotlinCoroutineSupport._KotlinFlow, _Concurrency.AsyncSequence {
    }
    public protocol SharedFlow: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow.Flow {
        var replayCache: [(any KotlinRuntimeSupport._KotlinBridgeable)?] {
            get
        }
    }
    public protocol StateFlow: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlinx.coroutines.flow.SharedFlow {
        var value: (any KotlinRuntimeSupport._KotlinBridgeable)? {
            get
        }
    }
    @objc(_Flow)
    package protocol _Flow {
    }
    @objc(_SharedFlow)
    package protocol _SharedFlow: ExportedKotlinPackages.kotlinx.coroutines.flow._Flow {
    }
    @objc(_StateFlow)
    package protocol _StateFlow: ExportedKotlinPackages.kotlinx.coroutines.flow._SharedFlow {
    }
}

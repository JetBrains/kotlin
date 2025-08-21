@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_state
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib

extension ExportedKotlinPackages.oh.my.state.inner {
    public final class InnerState: KotlinRuntime.KotlinBase {
        public var bytes: ExportedKotlinPackages.kotlin.ByteArray? {
            get {
                return { switch oh_my_state_inner_InnerState_bytes_get(self.__externalRCRef()) { case nil: .none; case let res: ExportedKotlinPackages.kotlin.ByteArray.__createClassWrapper(externalRCRef: res); } }()
            }
        }
        public init(
            bytes: ExportedKotlinPackages.kotlin.ByteArray?
        ) {
            if Self.self != ExportedKotlinPackages.oh.my.state.inner.InnerState.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.oh.my.state.inner.InnerState ") }
            let __kt = oh_my_state_inner_InnerState_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            oh_my_state_inner_InnerState_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_ByteArray___(__kt, bytes.map { it in it.__externalRCRef() } ?? nil)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
}
extension ExportedKotlinPackages.oh.my.state {
    public typealias ToExtract = ExportedKotlinPackages.oh.my.state.ExtractedByTypealias
    public final class ExtractedByTypealias: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != ExportedKotlinPackages.oh.my.state.ExtractedByTypealias.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.oh.my.state.ExtractedByTypealias ") }
            let __kt = oh_my_state_ExtractedByTypealias_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            oh_my_state_ExtractedByTypealias_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class State: KotlinRuntime.KotlinBase {
        public var innerState: ExportedKotlinPackages.oh.my.state.inner.InnerState? {
            get {
                return { switch oh_my_state_State_innerState_get(self.__externalRCRef()) { case nil: .none; case let res: ExportedKotlinPackages.oh.my.state.inner.InnerState.__createClassWrapper(externalRCRef: res); } }()
            }
        }
        public init(
            innerState: ExportedKotlinPackages.oh.my.state.inner.InnerState?
        ) {
            if Self.self != ExportedKotlinPackages.oh.my.state.State.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.oh.my.state.State ") }
            let __kt = oh_my_state_State_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            oh_my_state_State_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_oh_my_state_inner_InnerState___(__kt, innerState.map { it in it.__externalRCRef() } ?? nil)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
}

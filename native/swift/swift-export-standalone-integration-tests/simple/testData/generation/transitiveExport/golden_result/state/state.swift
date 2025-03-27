@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_state
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib

public extension ExportedKotlinPackages.oh.my.state {
    public final class State: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public var bytes: ExportedKotlinPackages.kotlin.ByteArray? {
            get {
                return { switch oh_my_state_State_bytes_get(self.__externalRCRef()) { case nil: .none; case let res: ExportedKotlinPackages.kotlin.ByteArray(__externalRCRef: res); } }()
            }
        }
        package override init(
            __externalRCRef: Swift.UnsafeMutableRawPointer?
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public init(
            bytes: ExportedKotlinPackages.kotlin.ByteArray?
        ) {
            let __kt = oh_my_state_State_init_allocate()
            super.init(__externalRCRef: __kt)
            oh_my_state_State_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_ByteArray___(__kt, bytes.map { it in it.__externalRCRef() } ?? nil)
        }
    }
}

@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinxCoroutinesCore
import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel, KotlinCoroutineSupport.KotlinReceiveChannel where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.channels._ReceiveChannel {
}
extension ExportedKotlinPackages.kotlinx.coroutines.channels {
    public protocol ReceiveChannel: KotlinRuntime.KotlinBase, KotlinCoroutineSupport.KotlinReceiveChannel {
    }
    @objc(_ReceiveChannel)
    package protocol _ReceiveChannel {
    }
}

@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinxCoroutinesCore
import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.kotlinx.coroutines.flow.Flow where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow.Flow {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlinx.coroutines.flow.Flow, KotlinCoroutineSupport._KotlinFlow, _Concurrency.AsyncSequence where Wrapped : ExportedKotlinPackages.kotlinx.coroutines.flow._Flow {
}
extension ExportedKotlinPackages.kotlinx.coroutines.flow {
    public protocol Flow: KotlinRuntime.KotlinBase, KotlinCoroutineSupport._KotlinFlow, _Concurrency.AsyncSequence {
    }
    @objc(_Flow)
    package protocol _Flow {
    }
}

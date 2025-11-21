@_implementationOnly import KotlinBridges_main
import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinxCoroutinesCore

public func demo() -> any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow {
    return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___demo()) as! any ExportedKotlinPackages.kotlinx.coroutines.flow.Flow
}

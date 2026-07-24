@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport
import dependency

public func today() -> ExportedKotlinPackages.datetime.LocalDate {
    return ExportedKotlinPackages.datetime.LocalDate.__createClassWrapper(externalRCRef: __root___today())
}

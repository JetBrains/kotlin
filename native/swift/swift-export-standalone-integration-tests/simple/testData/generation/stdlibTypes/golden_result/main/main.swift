@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib

public func buildString(
    sb: ExportedKotlinPackages.kotlin.text.StringBuilder
) -> Swift.String {
    return __root___buildString__TypesOfArguments__ExportedKotlinPackages_kotlin_text_StringBuilder__(sb.__externalRCRef())
}
public func returnsByteArray() -> ExportedKotlinPackages.kotlin.ByteArray {
    return ExportedKotlinPackages.kotlin.ByteArray.__createClassWrapper(externalRCRef: __root___returnsByteArray())
}

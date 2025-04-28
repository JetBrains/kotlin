@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib

public extension ExportedKotlinPackages.generation.stdlibTypes.stdlibTypes {
    public static func buildString(
        sb: ExportedKotlinPackages.kotlin.text.StringBuilder
    ) -> Swift.String {
        return generation_stdlibTypes_stdlibTypes_buildString__TypesOfArguments__ExportedKotlinPackages_kotlin_text_StringBuilder__(sb.__externalRCRef())
    }
    public static func returnsByteArray() -> ExportedKotlinPackages.kotlin.ByteArray {
        return ExportedKotlinPackages.kotlin.ByteArray.__create(externalRCRef: generation_stdlibTypes_stdlibTypes_returnsByteArray())
    }
}

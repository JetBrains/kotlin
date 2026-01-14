@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public func log(
    messages: Swift.String...
) -> Swift.Void {
    return org_kotlin_foo_log__TypesOfArguments__Swift_Array_Swift_String___(messages)
}
extension ExportedKotlinPackages.org.kotlin.foo {
    public static func log(
        messages: Swift.String...
    ) -> Swift.Void {
        return org_kotlin_foo_log__TypesOfArguments__Swift_Array_Swift_String___(messages)
    }
}

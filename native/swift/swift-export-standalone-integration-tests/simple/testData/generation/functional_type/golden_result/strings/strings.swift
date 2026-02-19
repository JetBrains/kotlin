@_implementationOnly import KotlinBridges_strings
import KotlinRuntime
import KotlinRuntimeSupport

public func consume_block_with_string_id(
    block: @escaping (Swift.String) -> Swift.String
) -> Swift.String {
    return __root___consume_block_with_string_id__TypesOfArguments__U28Swift_StringU29202D_U20Swift_String__({
        let originalBlock = block
        return { arg0 in return originalBlock(arg0) }
    }())
}

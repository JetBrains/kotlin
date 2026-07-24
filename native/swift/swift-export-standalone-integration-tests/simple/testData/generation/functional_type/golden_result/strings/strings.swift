@_implementationOnly import KotlinBridges_strings
import KotlinRuntime
import KotlinRuntimeSupport

public func consume_block_with_string_id(
    block: @escaping (Swift.String) -> Swift.String
) -> Swift.String {
    return __root___consume_block_with_string_id__TypesOfArguments__U28Swift_StringU29202D_U20Swift_String__({
        let originalBlock: (Swift.String) -> Swift.String = block
        return { (arg0: Swift.String) in
            let _arg0: Swift.String = arg0
            let _result = originalBlock(_arg0)
            return _result
        }
    }())
}

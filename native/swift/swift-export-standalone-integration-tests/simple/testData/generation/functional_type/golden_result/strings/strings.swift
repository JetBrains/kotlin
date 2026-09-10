@_implementationOnly import KotlinBridges_strings
import KotlinRuntime
import KotlinRuntimeSupport

public func consume_block_with_string_id(
    block: @escaping (Swift.String) -> Swift.String
) -> Swift.String {
    return __root___consume_block_with_string_id__TypesOfArguments__U28Swift_StringU29202D_U20Swift_String__(Unmanaged.passRetained((block as (Swift.String) -> Swift.String) as AnyObject).toOpaque())
}
@_cdecl("strings_internal_functional_type_callee_SwiftU2EString__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__")
package func strings_internal_functional_type_callee_SwiftU2EString__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.String) -> Swift.String {
    let _result: Swift.String = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.String) -> Swift.String)(_1)
    return _result
}

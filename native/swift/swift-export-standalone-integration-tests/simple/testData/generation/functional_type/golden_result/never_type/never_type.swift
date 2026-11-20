@_implementationOnly import KotlinBridges_never_type
import KotlinRuntime
import KotlinRuntimeSupport

public func consume_block_with_never(
    block: @escaping (Swift.Never) -> Swift.Never
) -> Swift.Never {
    return { __root___consume_block_with_never__TypesOfArguments__U28Swift_NeverU29202D_U20Swift_Never__(Unmanaged.passRetained((block as (Swift.Never) -> Swift.Never) as AnyObject).toOpaque()); fatalError() }()
}
public func consume_block_with_opt_never(
    block: @escaping (Swift.Int32, Swift.Never?) -> Swift.Int32
) -> Swift.Never {
    return { __root___consume_block_with_opt_never__TypesOfArguments__U28Swift_Int32_U20Swift_Optional_Swift_Never_U29202D_U20Swift_Int32__(Unmanaged.passRetained((block as (Swift.Int32, Swift.Optional<Swift.Never>) -> Swift.Int32) as AnyObject).toOpaque()); fatalError() }()
}
@_cdecl("never_type_internal_functional_type_callee_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Optional_Swift_Never___")
package func never_type_internal_functional_type_callee_SwiftU2EInt32__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Optional_Swift_Never___(_ pointerToClosure: Swift.UnsafeMutableRawPointer, _ _1: Swift.Int32, _ _2: Swift.Bool) -> Swift.Int32 {
    let _result: Swift.Int32 = (Unmanaged<AnyObject>.fromOpaque(pointerToClosure).takeUnretainedValue() as! (Swift.Int32, Swift.Optional<Swift.Never>) -> Swift.Int32)(_1, { _2; return nil }())
    return _result
}

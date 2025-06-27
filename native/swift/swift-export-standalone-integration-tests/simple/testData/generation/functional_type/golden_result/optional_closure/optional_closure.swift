@_implementationOnly import KotlinBridges_optional_closure
import KotlinRuntime
import KotlinRuntimeSupport

public func consume_opt_closure(
    arg: (() -> Swift.Void)?
) -> Swift.Void {
    return __root___consume_opt_closure__TypesOfArguments__Swift_Optional_U2829202D_U20Swift_Void___(arg.map { it in {
        let originalBlock = it
        return { return originalBlock() }
    }() } ?? nil)
}

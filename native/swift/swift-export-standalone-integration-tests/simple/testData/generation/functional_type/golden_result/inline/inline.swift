@_implementationOnly import KotlinBridges_inline
import KotlinRuntime
import KotlinRuntimeSupport

public func bar(
    inlined: @escaping () -> Swift.Void,
    notInlined: @escaping () -> Swift.Void
) -> Swift.Void {
    return __root___bar__TypesOfArguments__U2829202D_U20Swift_Void_U2829202D_U20Swift_Void__({
        let originalBlock = inlined
        return { return originalBlock() }
    }(), {
        let originalBlock = notInlined
        return { return originalBlock() }
    }())
}
public func foo(
    inlined: @escaping () -> Swift.Void
) -> Swift.Void {
    return __root___foo__TypesOfArguments__U2829202D_U20Swift_Void__({
        let originalBlock = inlined
        return { return originalBlock() }
    }())
}

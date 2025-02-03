@_implementationOnly import KotlinBridges_simple
import KotlinRuntimeSupport

public var closure_property: () -> Swift.Void {
    get {
        return {
            let nativeBlock = __root___closure_property_get()
            return {
                let result = nativeBlock!()
                return ()
            }
        }()
    }
    set {
        return __root___closure_property_set__TypesOfArguments__U2829202D_U20Swift_Void__({
            let originalBlock = newValue
            return {
                originalBlock()
                return 0
            }
        }())
    }
}
public func foo_1() -> () -> Swift.Void {
    return {
        let nativeBlock = __root___foo_1()
        return {
            let result = nativeBlock!()
            return ()
        }
    }()
}
public func foo_2() -> () -> Swift.Void {
    return {
        let nativeBlock = __root___foo_2()
        return {
            let result = nativeBlock!()
            return ()
        }
    }()
}
public func foo_consume_simple(
    block: @escaping () -> Swift.Void
) -> Swift.Void {
    return __root___foo_consume_simple__TypesOfArguments__U2829202D_U20Swift_Void__({
        let originalBlock = block
        return {
            originalBlock()
            return 0
        }
    }())
}
public func foo_sus() -> Swift.Never {
    fatalError()
}

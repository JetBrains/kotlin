@_implementationOnly import KotlinBridges_main

public func foo_1() -> () -> Swift.Void {
    return {
        let nativeBlock = __root___foo_1()
        return { nativeBlock!() }
    }()
}
public func foo_2() -> () -> Swift.Void {
    return {
        let nativeBlock = __root___foo_2()
        return { nativeBlock!() }
    }()
}
public func foo_sus() -> Swift.Never {
    fatalError()
}

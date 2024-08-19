@_implementationOnly import KotlinBridges_main
import KotlinRuntime

open class Foo : KotlinRuntime.KotlinBase {
    public override init() {
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Foo_init_initialize__TypesOfArguments__uintptr_t__(__kt)
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public var foo: main.Foo {
    get {
        return main.Foo(__externalRCRef: __root___foo_get())
    }
    set {
        return __root___foo_set__TypesOfArguments__uintptr_t__(newValue.__externalRCRef())
    }
}
@_cdecl("SwiftExport_main_Foo_toRetainedSwift")
private func SwiftExport_main_Foo_toRetainedSwift(
    externalRCRef: Swift.UInt
) -> Swift.UnsafeMutableRawPointer {
    return Unmanaged.passRetained(main.Foo(__externalRCRef: externalRCRef)).toOpaque()
}
public func getFoo() -> main.Foo {
    return main.Foo(__externalRCRef: __root___getFoo())
}

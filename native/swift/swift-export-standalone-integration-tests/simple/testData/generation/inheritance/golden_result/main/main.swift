@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

open class Foo: KotlinRuntime.KotlinBase {
    public override init() {
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Foo_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
    }
    package override init(
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
        return __root___foo_set__TypesOfArguments__main_Foo__(newValue.__externalRCRef())
    }
}
public func getFoo() -> main.Foo {
    return main.Foo(__externalRCRef: __root___getFoo())
}

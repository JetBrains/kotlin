@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public final class Context: KotlinRuntime.KotlinBase {
    public override init() {
        let __kt = __root___Context_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Context_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public final class Foo: KotlinRuntime.KotlinBase {
    public static var shared: main.Foo {
        get {
            return main.Foo(__externalRCRef: __root___Foo_get())
        }
    }
    private override init() {
        fatalError()
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}

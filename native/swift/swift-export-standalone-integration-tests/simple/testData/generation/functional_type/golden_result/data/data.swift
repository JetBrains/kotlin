@_implementationOnly import KotlinBridges_data
import KotlinRuntime
import KotlinRuntimeSupport

public final class Bar: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public override init() {
        let __kt = __root___Bar_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Bar_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public final class Foo: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
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

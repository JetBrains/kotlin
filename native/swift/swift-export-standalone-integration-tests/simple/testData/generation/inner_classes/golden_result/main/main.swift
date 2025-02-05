@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public final class Outer: KotlinRuntime.KotlinBase {
    public override init() {
        let __kt = __root___Outer_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Outer_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}

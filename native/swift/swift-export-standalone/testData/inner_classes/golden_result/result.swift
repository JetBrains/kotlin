import KotlinBridges
import KotlinRuntime

public class Outer : KotlinRuntime.KotlinBase {
    public override init() {
        let __kt = __root___Outer_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Outer_init_initialize__TypesOfArguments__uintptr_t__(__kt)
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}


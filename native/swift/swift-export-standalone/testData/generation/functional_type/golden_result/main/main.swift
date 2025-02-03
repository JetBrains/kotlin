@_implementationOnly import KotlinBridges_main
import KotlinRuntimeSupport
import KotlinRuntime

public final class Foo: KotlinRuntime.KotlinBase {
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
public func block_consuming_reftype_collect() -> (main.Foo) -> Swift.Void {
    return {
        let nativeBlock = __root___block_consuming_reftype_collect()
        return { arg0 in
            originalBlock(Foo(__externalRCRef: arg0!.uintValue))
            return 0
        }
    }())
}

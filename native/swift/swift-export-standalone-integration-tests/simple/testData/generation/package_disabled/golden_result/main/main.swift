@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public final class Bar: KotlinRuntime.KotlinBase {
    public init() {
        let __kt = org_kotlin_foo_bar_Bar_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { org_kotlin_foo_bar_Bar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class Baz: KotlinRuntime.KotlinBase {
    public init() {
        let __kt = org_kotlin_baz_Baz_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { org_kotlin_baz_Baz_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class Foo: KotlinRuntime.KotlinBase {
    public init() {
        let __kt = org_kotlin_foo_Foo_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { org_kotlin_foo_Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}

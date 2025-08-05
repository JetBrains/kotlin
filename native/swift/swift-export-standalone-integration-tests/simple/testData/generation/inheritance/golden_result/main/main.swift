@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

open class Foo: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != main.Foo.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Foo ") }
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public var foo: main.Foo {
    get {
        return main.Foo.__createClassWrapper(externalRCRef: __root___foo_get())
    }
    set {
        return __root___foo_set__TypesOfArguments__main_Foo__(newValue.__externalRCRef())
    }
}
public func getFoo() -> main.Foo {
    return main.Foo.__createClassWrapper(externalRCRef: __root___getFoo())
}

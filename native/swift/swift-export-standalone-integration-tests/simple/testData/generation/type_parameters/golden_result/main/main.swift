@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public final class Foo: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public var value: Any {
        get {
            return Foo_value_get(self.__externalRCRef())
        }
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public init(
        value: Any
    ) {
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Foo_init_initialize__TypesOfArguments__Swift_UInt_Any__(__kt, (value as AnyObject).__externalRCRef())
    }
}

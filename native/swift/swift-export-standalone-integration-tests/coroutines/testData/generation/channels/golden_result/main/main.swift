@_implementationOnly import KotlinBridges_main
import KotlinCoroutineSupport
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinxCoroutinesCore

public final class Foo: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != main.Foo.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Foo ") }
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public func produceChannel() -> any KotlinCoroutineSupport.KotlinTypedReceiveChannel<main.Foo> {
    return KotlinCoroutineSupport._KotlinTypedReceiveChannelImpl<main.Foo>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___produceChannel()) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel)
}
public func produceIntChannel() -> any KotlinCoroutineSupport.KotlinTypedReceiveChannel<Swift.Int32> {
    return KotlinCoroutineSupport._KotlinTypedReceiveChannelImpl<Swift.Int32>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___produceIntChannel()) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel)
}
public func produceNullableChannel() -> any KotlinCoroutineSupport.KotlinTypedReceiveChannel<Swift.Optional<main.Foo>> {
    return KotlinCoroutineSupport._KotlinTypedReceiveChannelImpl<Swift.Optional<main.Foo>>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___produceNullableChannel()) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel)
}
public func produceStringChannel() -> any KotlinCoroutineSupport.KotlinTypedReceiveChannel<Swift.String> {
    return KotlinCoroutineSupport._KotlinTypedReceiveChannelImpl<Swift.String>(KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___produceStringChannel()) as! any ExportedKotlinPackages.kotlinx.coroutines.channels.ReceiveChannel)
}

@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public protocol Semaphore: KotlinRuntime.KotlinBase, main._Semaphore {
}
@objc(_main_Semaphore)
public protocol _Semaphore {
}
public protocol __Semaphore: KotlinRuntimeSupport._KotlinBridgeable {
}
public final class ClassA: KotlinRuntime.KotlinBase, main.Semaphore, main.__Semaphore {
    public override init() {
        let __kt = __root___ClassA_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___ClassA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class ClassB: KotlinRuntime.KotlinBase {
    public override init() {
        let __kt = __root___ClassB_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___ClassB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public func hash(
        intoOk: Any
    ) -> Swift.Void {
        return { ClassB_hash__TypesOfArguments__Any__(self.__externalRCRef(), KotlinRuntime.KotlinBase.__externalRCRefForAny(intoOk)); return () }()
    }
    public func mutableCopyOk() -> Any {
        return KotlinRuntime.KotlinBase.__createBridgeableAny(externalRCRef: ClassB_mutableCopy(self.__externalRCRef()))
    }
}
public var hash: Swift.Int32 {
    get {
        return __root___hash_get()
    }
}
public func forwardingTarget(
    `for`: Any
) -> Swift.Void {
    return { __root___forwardingTarget__TypesOfArguments__Any__(KotlinRuntime.KotlinBase.__externalRCRefForAny(`for`)); return () }()
}
@_documentation(visibility: internal)
extension main.Semaphore where Self : main.__Semaphore {
}
extension main.Semaphore {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: main.Semaphore, main.__Semaphore where Wrapped : main._Semaphore {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: main._Semaphore {
}

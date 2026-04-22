@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public protocol Semaphore: KotlinRuntime.KotlinBase {
}
@objc(_Semaphore)
package protocol _Semaphore {
}
public final class ClassA: KotlinRuntime.KotlinBase, main.Semaphore, main._Semaphore {
    public init() {
        if Self.self != main.ClassA.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.ClassA ") }
        let __kt = __root___ClassA_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___ClassA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class ClassB: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != main.ClassB.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.ClassB ") }
        let __kt = __root___ClassB_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___ClassB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public func hash(
        intoOk: any KotlinRuntimeSupport._KotlinBridgeable
    ) -> Swift.Void {
        return { ClassB_hash__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(self.__externalRCRef(), intoOk.__externalRCRef()); return () }()
    }
    public func mutableCopyOk() -> any KotlinRuntimeSupport._KotlinBridgeable {
        return KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: ClassB_mutableCopy(self.__externalRCRef()))
    }
}
public var hash: Swift.Int32 {
    get {
        return __root___hash_get()
    }
}
public func forwardingTarget(
    `for`: any KotlinRuntimeSupport._KotlinBridgeable
) -> Swift.Void {
    return { __root___forwardingTarget__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(`for`.__externalRCRef()); return () }()
}
extension main.Semaphore where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension main.Semaphore {
}
extension KotlinRuntimeSupport._KotlinExistential: main.Semaphore where Wrapped : main._Semaphore {
}

@_exported import ExportedKotlinPackages
import KotlinRuntime
import KotlinBridges_main

public class MyObject : KotlinRuntime.KotlinBase {
    public static var shared: main.MyObject {
        get {
            return main.MyObject(__externalRCRef: __root___MyObject_get())
        }
    }
    private override init() {
        fatalError()
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public func getMainObject() -> KotlinRuntime.KotlinBase {
    return KotlinRuntime.KotlinBase(__externalRCRef: __root___getMainObject())
}
public func isMainObject(
    obj: KotlinRuntime.KotlinBase
) -> Swift.Bool {
    return __root___isMainObject__TypesOfArguments__uintptr_t__(obj.__externalRCRef())
}
public extension ExportedKotlinPackages.opaque {
    public static func produce_ABSTRACT_CLASS() -> KotlinRuntime.KotlinBase {
        return KotlinRuntime.KotlinBase(__externalRCRef: opaque_produce_ABSTRACT_CLASS())
    }
    public static func produce_DATA_CLASS() -> KotlinRuntime.KotlinBase {
        return KotlinRuntime.KotlinBase(__externalRCRef: opaque_produce_DATA_CLASS())
    }
    public static func produce_DATA_OBJECT() -> KotlinRuntime.KotlinBase {
        return KotlinRuntime.KotlinBase(__externalRCRef: opaque_produce_DATA_OBJECT())
    }
    public static func produce_ENUM() -> KotlinRuntime.KotlinBase {
        return KotlinRuntime.KotlinBase(__externalRCRef: opaque_produce_ENUM())
    }
    public static func produce_INTERFACE() -> KotlinRuntime.KotlinBase {
        return KotlinRuntime.KotlinBase(__externalRCRef: opaque_produce_INTERFACE())
    }
    public static func produce_OPEN_CLASS() -> KotlinRuntime.KotlinBase {
        return KotlinRuntime.KotlinBase(__externalRCRef: opaque_produce_OPEN_CLASS())
    }
    public static func produce_VALUE_CLASS() -> KotlinRuntime.KotlinBase {
        return KotlinRuntime.KotlinBase(__externalRCRef: opaque_produce_VALUE_CLASS())
    }
    public static func recieve_ABSTRACT_CLASS(
        x: KotlinRuntime.KotlinBase
    ) -> Swift.Void {
        return opaque_recieve_ABSTRACT_CLASS__TypesOfArguments__uintptr_t__(x.__externalRCRef())
    }
    public static func recieve_DATA_CLASS(
        x: KotlinRuntime.KotlinBase
    ) -> Swift.Void {
        return opaque_recieve_DATA_CLASS__TypesOfArguments__uintptr_t__(x.__externalRCRef())
    }
    public static func recieve_DATA_OBJECT(
        x: KotlinRuntime.KotlinBase
    ) -> Swift.Void {
        return opaque_recieve_DATA_OBJECT__TypesOfArguments__uintptr_t__(x.__externalRCRef())
    }
    public static func recieve_ENUM(
        x: KotlinRuntime.KotlinBase
    ) -> Swift.Void {
        return opaque_recieve_ENUM__TypesOfArguments__uintptr_t__(x.__externalRCRef())
    }
    public static func recieve_INTERFACE(
        x: KotlinRuntime.KotlinBase
    ) -> Swift.Void {
        return opaque_recieve_INTERFACE__TypesOfArguments__uintptr_t__(x.__externalRCRef())
    }
    public static func recieve_OPEN_CLASS(
        x: KotlinRuntime.KotlinBase
    ) -> Swift.Void {
        return opaque_recieve_OPEN_CLASS__TypesOfArguments__uintptr_t__(x.__externalRCRef())
    }
    public static func recieve_VALUE_CLASS(
        x: KotlinRuntime.KotlinBase
    ) -> Swift.Void {
        return opaque_recieve_VALUE_CLASS__TypesOfArguments__uintptr_t__(x.__externalRCRef())
    }
}

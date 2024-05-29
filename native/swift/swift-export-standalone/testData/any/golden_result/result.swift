import KotlinRuntime
import KotlinBridges

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


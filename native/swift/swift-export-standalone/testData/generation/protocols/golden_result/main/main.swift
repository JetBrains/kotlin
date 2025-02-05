@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public protocol Barable: KotlinRuntime.KotlinBase, main.Foeble {
}
public protocol Bazzable: KotlinRuntime.KotlinBase {
}
public protocol Foeble: KotlinRuntime.KotlinBase {
    var baz: Swift.Int32 {
        get
    }
    func bar(
        arg: Swift.Int32
    ) -> Swift.Int32
}
public protocol OUTSIDE_PROTO: KotlinRuntime.KotlinBase {
}
public final class Bar: KotlinRuntime.KotlinBase, main.Barable, main.Foeble, main.Bazzable {
    public var baz: Swift.Int32 {
        get {
            return Bar_baz_get(self.__externalRCRef())
        }
    }
    public override init() {
        let __kt = __root___Bar_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Bar_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public func bar(
        arg: Swift.Int32
    ) -> Swift.Int32 {
        return Bar_bar__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), arg)
    }
}
public final class ENUM_WITH_INTERFACE_INHERITANCE: KotlinRuntime.KotlinBase, main.OUTSIDE_PROTO, Swift.CaseIterable {
    public static var allCases: [main.ENUM_WITH_INTERFACE_INHERITANCE] {
        get {
            return ENUM_WITH_INTERFACE_INHERITANCE_entries_get() as! Swift.Array<main.ENUM_WITH_INTERFACE_INHERITANCE>
        }
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public static func valueOf(
        value: Swift.String
    ) -> main.ENUM_WITH_INTERFACE_INHERITANCE {
        return main.ENUM_WITH_INTERFACE_INHERITANCE(__externalRCRef: ENUM_WITH_INTERFACE_INHERITANCE_valueOf__TypesOfArguments__Swift_String__(value))
    }
}
public final class MyObject: KotlinRuntime.KotlinBase {
    public static var shared: main.MyObject {
        get {
            return main.MyObject(__externalRCRef: __root___MyObject_get())
        }
    }
    private override init() {
        fatalError()
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public final class OBJECT_WITH_INTERFACE_INHERITANCE: KotlinRuntime.KotlinBase, main.OUTSIDE_PROTO {
    public static var shared: main.OBJECT_WITH_INTERFACE_INHERITANCE {
        get {
            return main.OBJECT_WITH_INTERFACE_INHERITANCE(__externalRCRef: __root___OBJECT_WITH_INTERFACE_INHERITANCE_get())
        }
    }
    private override init() {
        fatalError()
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}

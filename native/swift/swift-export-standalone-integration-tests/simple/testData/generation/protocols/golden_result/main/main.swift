@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public protocol Barable: KotlinRuntime.KotlinBase, main.Foeble {
    var baz: any main.Foeble {
        get
    }
    func bar(
        arg: any main.Foeble
    ) -> any main.Barable
}
public protocol Bazzable: KotlinRuntime.KotlinBase {
}
public protocol Foeble: KotlinRuntime.KotlinBase {
    var baz: any main.Foeble {
        get
    }
    func bar(
        arg: any main.Foeble
    ) -> any main.Foeble
}
public protocol OUTSIDE_PROTO: KotlinRuntime.KotlinBase {
}
public final class Bar: KotlinRuntime.KotlinBase, main.Barable, main.Foeble, main.Bazzable, KotlinRuntimeSupport._KotlinBridged {
    public var baz: main.Bar {
        get {
            return main.Bar(__externalRCRef: Bar_baz_get(self.__externalRCRef()))
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
        arg: any main.Foeble
    ) -> main.Bar {
        return main.Bar(__externalRCRef: Bar_bar__TypesOfArguments__anyU20main_Foeble__(self.__externalRCRef(), arg.__externalRCRef()))
    }
}
public final class ENUM_WITH_INTERFACE_INHERITANCE: KotlinRuntime.KotlinBase, main.OUTSIDE_PROTO, KotlinRuntimeSupport._KotlinBridged, Swift.CaseIterable {
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
public final class Foo: KotlinRuntime.KotlinBase, main.Foeble, KotlinRuntimeSupport._KotlinBridged {
    public var baz: any main.Foeble {
        get {
            return KotlinRuntime.KotlinBase(__externalRCRef: Foo_baz_get(self.__externalRCRef())) as! any main.Foeble
        }
    }
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
    public func bar(
        arg: any main.Foeble
    ) -> main.Foo {
        return main.Foo(__externalRCRef: Foo_bar__TypesOfArguments__anyU20main_Foeble__(self.__externalRCRef(), arg.__externalRCRef()))
    }
}
public final class MyObject: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
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
public final class OBJECT_WITH_INTERFACE_INHERITANCE: KotlinRuntime.KotlinBase, main.OUTSIDE_PROTO, KotlinRuntimeSupport._KotlinBridged {
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
public var list: [any main.Foeble] {
    get {
        return __root___list_get() as! Swift.Array<any main.Foeble>
    }
    set {
        return __root___list_set__TypesOfArguments__Swift_Array_anyU20main_Foeble___(newValue)
    }
}
public var normal: any main.Foeble {
    get {
        return KotlinRuntime.KotlinBase(__externalRCRef: __root___normal_get()) as! any main.Foeble
    }
    set {
        return __root___normal_set__TypesOfArguments__anyU20main_Foeble__(newValue.__externalRCRef())
    }
}
public var nullable: (any main.Foeble)? {
    get {
        return switch __root___nullable_get() { case 0: .none; case let res: KotlinRuntime.KotlinBase(__externalRCRef: res) as! any main.Foeble; }
    }
    set {
        return __root___nullable_set__TypesOfArguments__anyU20main_Foeble_opt___(newValue.map { it in it.__externalRCRef() } ?? 0)
    }
}
public func list(
    value: [any main.Foeble]
) -> [any main.Foeble] {
    return __root___list__TypesOfArguments__Swift_Array_anyU20main_Foeble___(value) as! Swift.Array<any main.Foeble>
}
public func normal(
    value: any main.Foeble
) -> any main.Foeble {
    return KotlinRuntime.KotlinBase(__externalRCRef: __root___normal__TypesOfArguments__anyU20main_Foeble__(value.__externalRCRef())) as! any main.Foeble
}
public func nullable(
    value: (any main.Foeble)?
) -> (any main.Foeble)? {
    return switch __root___nullable__TypesOfArguments__anyU20main_Foeble_opt___(value.map { it in it.__externalRCRef() } ?? 0) { case 0: .none; case let res: KotlinRuntime.KotlinBase(__externalRCRef: res) as! any main.Foeble; }
}
public extension main.Barable where Self : KotlinRuntimeSupport._KotlinBridged {
    public var baz: any main.Foeble {
        get {
            return KotlinRuntime.KotlinBase(__externalRCRef: Barable_baz_get(self.__externalRCRef())) as! any main.Foeble
        }
    }
    public func bar(
        arg: any main.Foeble
    ) -> any main.Barable {
        return KotlinRuntime.KotlinBase(__externalRCRef: Barable_bar__TypesOfArguments__anyU20main_Foeble__(self.__externalRCRef(), arg.__externalRCRef())) as! any main.Barable
    }
}
public extension main.Bazzable where Self : KotlinRuntimeSupport._KotlinBridged {
}
public extension main.Foeble where Self : KotlinRuntimeSupport._KotlinBridged {
    public var baz: any main.Foeble {
        get {
            return KotlinRuntime.KotlinBase(__externalRCRef: Foeble_baz_get(self.__externalRCRef())) as! any main.Foeble
        }
    }
    public func bar(
        arg: any main.Foeble
    ) -> any main.Foeble {
        return KotlinRuntime.KotlinBase(__externalRCRef: Foeble_bar__TypesOfArguments__anyU20main_Foeble__(self.__externalRCRef(), arg.__externalRCRef())) as! any main.Foeble
    }
}
public extension main.OUTSIDE_PROTO where Self : KotlinRuntimeSupport._KotlinBridged {
}

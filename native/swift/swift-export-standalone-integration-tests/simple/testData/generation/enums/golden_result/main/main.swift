@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib

public final class Enum: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged, Swift.CaseIterable {
    public static var a: main.Enum {
        get {
            return main.Enum.__createClassWrapper(externalRCRef: Enum_a_get())
        }
    }
    public static var allCases: [main.Enum] {
        get {
            return Enum_entries_get() as! Swift.Array<main.Enum>
        }
    }
    public static var b: main.Enum {
        get {
            return main.Enum.__createClassWrapper(externalRCRef: Enum_b_get())
        }
    }
    public var i: Swift.Int32 {
        get {
            return Enum_i_get(self.__externalRCRef())
        }
        set {
            return Enum_i_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue)
        }
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    public func print() -> Swift.String {
        return Enum_print(self.__externalRCRef())
    }
    public static func valueOf(
        value: Swift.String
    ) -> main.Enum {
        return main.Enum.__createClassWrapper(externalRCRef: Enum_valueOf__TypesOfArguments__Swift_String__(value))
    }
}
public final class EnumSimple: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged, Swift.CaseIterable {
    public static var FIRST: main.EnumSimple {
        get {
            return main.EnumSimple.__createClassWrapper(externalRCRef: EnumSimple_FIRST_get())
        }
    }
    public static var LAST: main.EnumSimple {
        get {
            return main.EnumSimple.__createClassWrapper(externalRCRef: EnumSimple_LAST_get())
        }
    }
    public static var SECOND: main.EnumSimple {
        get {
            return main.EnumSimple.__createClassWrapper(externalRCRef: EnumSimple_SECOND_get())
        }
    }
    public static var allCases: [main.EnumSimple] {
        get {
            return EnumSimple_entries_get() as! Swift.Array<main.EnumSimple>
        }
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    public static func valueOf(
        value: Swift.String
    ) -> main.EnumSimple {
        return main.EnumSimple.__createClassWrapper(externalRCRef: EnumSimple_valueOf__TypesOfArguments__Swift_String__(value))
    }
}
public final class EnumWithAbstractMembers: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged, Swift.CaseIterable {
    public static var MAGENTA: main.EnumWithAbstractMembers {
        get {
            return main.EnumWithAbstractMembers.__createClassWrapper(externalRCRef: EnumWithAbstractMembers_MAGENTA_get())
        }
    }
    public static var SKY: main.EnumWithAbstractMembers {
        get {
            return main.EnumWithAbstractMembers.__createClassWrapper(externalRCRef: EnumWithAbstractMembers_SKY_get())
        }
    }
    public static var YELLOW: main.EnumWithAbstractMembers {
        get {
            return main.EnumWithAbstractMembers.__createClassWrapper(externalRCRef: EnumWithAbstractMembers_YELLOW_get())
        }
    }
    public static var allCases: [main.EnumWithAbstractMembers] {
        get {
            return EnumWithAbstractMembers_entries_get() as! Swift.Array<main.EnumWithAbstractMembers>
        }
    }
    public var red: Swift.Int32 {
        get {
            return EnumWithAbstractMembers_red_get(self.__externalRCRef())
        }
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    public func blue() -> Swift.Int32 {
        return EnumWithAbstractMembers_blue(self.__externalRCRef())
    }
    public func green() -> Swift.Int32 {
        return EnumWithAbstractMembers_green(self.__externalRCRef())
    }
    public func ordinalSquare() -> Swift.Int32 {
        return EnumWithAbstractMembers_ordinalSquare(self.__externalRCRef())
    }
    public static func valueOf(
        value: Swift.String
    ) -> main.EnumWithAbstractMembers {
        return main.EnumWithAbstractMembers.__createClassWrapper(externalRCRef: EnumWithAbstractMembers_valueOf__TypesOfArguments__Swift_String__(value))
    }
}
public final class EnumWithMembers: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged, Swift.CaseIterable {
    public static var NORTH: main.EnumWithMembers {
        get {
            return main.EnumWithMembers.__createClassWrapper(externalRCRef: EnumWithMembers_NORTH_get())
        }
    }
    public static var SOUTH: main.EnumWithMembers {
        get {
            return main.EnumWithMembers.__createClassWrapper(externalRCRef: EnumWithMembers_SOUTH_get())
        }
    }
    public static var allCases: [main.EnumWithMembers] {
        get {
            return EnumWithMembers_entries_get() as! Swift.Array<main.EnumWithMembers>
        }
    }
    public var isNorth: Swift.Bool {
        get {
            return EnumWithMembers_isNorth_get(self.__externalRCRef())
        }
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    public func foo() -> Swift.String {
        return EnumWithMembers_foo(self.__externalRCRef())
    }
    public static func valueOf(
        value: Swift.String
    ) -> main.EnumWithMembers {
        return main.EnumWithMembers.__createClassWrapper(externalRCRef: EnumWithMembers_valueOf__TypesOfArguments__Swift_String__(value))
    }
}
public func enumId(
    e: ExportedKotlinPackages.kotlin.Enum
) -> ExportedKotlinPackages.kotlin.Enum {
    return ExportedKotlinPackages.kotlin.Enum.__createClassWrapper(externalRCRef: __root___enumId__TypesOfArguments__ExportedKotlinPackages_kotlin_Enum__(e.__externalRCRef()))
}
public func ewamValues() -> ExportedKotlinPackages.kotlin.Array {
    return ExportedKotlinPackages.kotlin.Array.__createClassWrapper(externalRCRef: __root___ewamValues())
}
public func yellow() -> main.EnumWithAbstractMembers {
    return main.EnumWithAbstractMembers.__createClassWrapper(externalRCRef: __root___yellow())
}

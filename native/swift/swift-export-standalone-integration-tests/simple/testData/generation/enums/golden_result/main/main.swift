@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib

public enum Enum: KotlinRuntimeSupport._KotlinBridgeable, Swift.CaseIterable, Swift.LosslessStringConvertible, Swift.RawRepresentable {
    case a
    case b
    public var description: Swift.String {
        get {
            switch self {
            case .a: "a"
            case .b: "b"
            default: fatalError()
            }
        }
    }
    public var i: Swift.Int32 {
        get {
            return Enum_i_get(self.__externalRCRef())
        }
        set {
            return { Enum_i_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue); return () }()
        }
    }
    public var rawValue: Swift.Int32 {
        get {
            switch self {
            case .a: 0
            case .b: 1
            default: fatalError()
            }
        }
    }
    public init?(
        _ description: Swift.String
    ) {
        switch description {
        case "a": self = .a
        case "b": self = .b
        default: return nil
        }
    }
    public init?(
        rawValue: Swift.Int32
    ) {
        guard 0..<2 ~= rawValue else { return nil }
        self = Enum.allCases[Int(rawValue)]
    }
    public init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        switch __root___Enum_ordinal(__externalRCRefUnsafe) {
        case 0: self = .a
        case 1: self = .b
        default: fatalError()
        }
    }
    public func __externalRCRef() -> Swift.UnsafeMutableRawPointer {
        return switch self {
        case .a: Enum_a()
        case .b: Enum_b()
        default: fatalError()
        }
    }
    public func print() -> Swift.String {
        return Enum_print(self.__externalRCRef())
    }
}
public enum EnumSimple: KotlinRuntimeSupport._KotlinBridgeable, Swift.CaseIterable, Swift.LosslessStringConvertible, Swift.RawRepresentable {
    case FIRST
    case SECOND
    case LAST
    public var description: Swift.String {
        get {
            switch self {
            case .FIRST: "FIRST"
            case .SECOND: "SECOND"
            case .LAST: "LAST"
            default: fatalError()
            }
        }
    }
    public var rawValue: Swift.Int32 {
        get {
            switch self {
            case .FIRST: 0
            case .SECOND: 1
            case .LAST: 2
            default: fatalError()
            }
        }
    }
    public init?(
        _ description: Swift.String
    ) {
        switch description {
        case "FIRST": self = .FIRST
        case "SECOND": self = .SECOND
        case "LAST": self = .LAST
        default: return nil
        }
    }
    public init?(
        rawValue: Swift.Int32
    ) {
        guard 0..<3 ~= rawValue else { return nil }
        self = EnumSimple.allCases[Int(rawValue)]
    }
    public init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        switch __root___EnumSimple_ordinal(__externalRCRefUnsafe) {
        case 0: self = .FIRST
        case 1: self = .SECOND
        case 2: self = .LAST
        default: fatalError()
        }
    }
    public func __externalRCRef() -> Swift.UnsafeMutableRawPointer {
        return switch self {
        case .FIRST: EnumSimple_FIRST()
        case .SECOND: EnumSimple_SECOND()
        case .LAST: EnumSimple_LAST()
        default: fatalError()
        }
    }
}
public enum EnumWithAbstractMembers: KotlinRuntimeSupport._KotlinBridgeable, Swift.CaseIterable, Swift.LosslessStringConvertible, Swift.RawRepresentable {
    case YELLOW
    case SKY
    case MAGENTA
    public var description: Swift.String {
        get {
            switch self {
            case .YELLOW: "YELLOW"
            case .SKY: "SKY"
            case .MAGENTA: "MAGENTA"
            default: fatalError()
            }
        }
    }
    public var rawValue: Swift.Int32 {
        get {
            switch self {
            case .YELLOW: 0
            case .SKY: 1
            case .MAGENTA: 2
            default: fatalError()
            }
        }
    }
    public var red: Swift.Int32 {
        get {
            return EnumWithAbstractMembers_red_get(self.__externalRCRef())
        }
    }
    public init?(
        _ description: Swift.String
    ) {
        switch description {
        case "YELLOW": self = .YELLOW
        case "SKY": self = .SKY
        case "MAGENTA": self = .MAGENTA
        default: return nil
        }
    }
    public init?(
        rawValue: Swift.Int32
    ) {
        guard 0..<3 ~= rawValue else { return nil }
        self = EnumWithAbstractMembers.allCases[Int(rawValue)]
    }
    public init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        switch __root___EnumWithAbstractMembers_ordinal(__externalRCRefUnsafe) {
        case 0: self = .YELLOW
        case 1: self = .SKY
        case 2: self = .MAGENTA
        default: fatalError()
        }
    }
    public func __externalRCRef() -> Swift.UnsafeMutableRawPointer {
        return switch self {
        case .YELLOW: EnumWithAbstractMembers_YELLOW()
        case .SKY: EnumWithAbstractMembers_SKY()
        case .MAGENTA: EnumWithAbstractMembers_MAGENTA()
        default: fatalError()
        }
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
}
public enum EnumWithMembers: KotlinRuntimeSupport._KotlinBridgeable, Swift.CaseIterable, Swift.LosslessStringConvertible, Swift.RawRepresentable {
    case NORTH
    case SOUTH
    public var description: Swift.String {
        get {
            switch self {
            case .NORTH: "NORTH"
            case .SOUTH: "SOUTH"
            default: fatalError()
            }
        }
    }
    public var isNorth: Swift.Bool {
        get {
            return EnumWithMembers_isNorth_get(self.__externalRCRef())
        }
    }
    public var rawValue: Swift.Int32 {
        get {
            switch self {
            case .NORTH: 0
            case .SOUTH: 1
            default: fatalError()
            }
        }
    }
    public init?(
        _ description: Swift.String
    ) {
        switch description {
        case "NORTH": self = .NORTH
        case "SOUTH": self = .SOUTH
        default: return nil
        }
    }
    public init?(
        rawValue: Swift.Int32
    ) {
        guard 0..<2 ~= rawValue else { return nil }
        self = EnumWithMembers.allCases[Int(rawValue)]
    }
    public init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        switch __root___EnumWithMembers_ordinal(__externalRCRefUnsafe) {
        case 0: self = .NORTH
        case 1: self = .SOUTH
        default: fatalError()
        }
    }
    public func __externalRCRef() -> Swift.UnsafeMutableRawPointer {
        return switch self {
        case .NORTH: EnumWithMembers_NORTH()
        case .SOUTH: EnumWithMembers_SOUTH()
        default: fatalError()
        }
    }
    public func foo() -> Swift.String {
        return EnumWithMembers_foo(self.__externalRCRef())
    }
}
@inlinable
public func enumId<_T0: KotlinRuntimeSupport._KotlinBridgeable>(
    e: ExportedKotlinPackages.kotlin.Enum<_T0>
) -> ExportedKotlinPackages.kotlin.Enum<KotlinRuntimeSupport.KotlinAny> {
    return ExportedKotlinPackages.kotlin.Enum<KotlinRuntimeSupport.KotlinAny>.__fromKotlinPointer(_cbridge___root___enumId__TypesOfArguments__ExportedKotlinPackages_kotlin_Enum__T0___(e.__externalRCRef()))
}
public func ewamEntries() -> any ExportedKotlinPackages.kotlin.enums.EnumEntries<main.EnumWithAbstractMembers> {
    return { let _ptr = __root___ewamEntries(); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.enums.EnumEntries<main.EnumWithAbstractMembers>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.enums.AnyEnumEntries<main.EnumWithAbstractMembers>.__createTypedWrapper(externalRCRef: _ptr) }()
}
public func ewamValues() -> ExportedKotlinPackages.kotlin.Array<main.EnumWithAbstractMembers> {
    return ExportedKotlinPackages.kotlin.Array<main.EnumWithAbstractMembers>.__fromKotlinPointer(__root___ewamValues())
}
public func yellow() -> main.EnumWithAbstractMembers {
    return main.EnumWithAbstractMembers.__fromKotlinPointer(__root___yellow())
}
@usableFromInline
@discardableResult
internal func _cbridge___root___enumId__TypesOfArguments__ExportedKotlinPackages_kotlin_Enum__T0___(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    return __root___enumId__TypesOfArguments__ExportedKotlinPackages_kotlin_Enum__T0___(_p0)
}

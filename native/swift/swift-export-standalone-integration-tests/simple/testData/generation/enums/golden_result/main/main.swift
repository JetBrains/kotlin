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
            default: "b"
            }
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
    public var rawValue: Swift.Int32 {
        get {
            switch self {
            case .a: 0
            default: 1
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
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer!,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        switch __externalRCRefUnsafe {
        case Enum_a(): self = .a
        default: self = .b
        }
    }
    public func __externalRCRef() -> Swift.UnsafeMutableRawPointer! {
        return switch self {
        case .a: Enum_a()
        default: Enum_b()
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
            default: "LAST"
            }
        }
    }
    public var rawValue: Swift.Int32 {
        get {
            switch self {
            case .FIRST: 0
            case .SECOND: 1
            default: 2
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
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer!,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        switch __externalRCRefUnsafe {
        case EnumSimple_FIRST(): self = .FIRST
        case EnumSimple_SECOND(): self = .SECOND
        default: self = .LAST
        }
    }
    public func __externalRCRef() -> Swift.UnsafeMutableRawPointer! {
        return switch self {
        case .FIRST: EnumSimple_FIRST()
        case .SECOND: EnumSimple_SECOND()
        default: EnumSimple_LAST()
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
            default: "MAGENTA"
            }
        }
    }
    public var rawValue: Swift.Int32 {
        get {
            switch self {
            case .YELLOW: 0
            case .SKY: 1
            default: 2
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
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer!,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        switch __externalRCRefUnsafe {
        case EnumWithAbstractMembers_YELLOW(): self = .YELLOW
        case EnumWithAbstractMembers_SKY(): self = .SKY
        default: self = .MAGENTA
        }
    }
    public func __externalRCRef() -> Swift.UnsafeMutableRawPointer! {
        return switch self {
        case .YELLOW: EnumWithAbstractMembers_YELLOW()
        case .SKY: EnumWithAbstractMembers_SKY()
        default: EnumWithAbstractMembers_MAGENTA()
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
            default: "SOUTH"
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
            default: 1
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
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer!,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        switch __externalRCRefUnsafe {
        case EnumWithMembers_NORTH(): self = .NORTH
        default: self = .SOUTH
        }
    }
    public func __externalRCRef() -> Swift.UnsafeMutableRawPointer! {
        return switch self {
        case .NORTH: EnumWithMembers_NORTH()
        default: EnumWithMembers_SOUTH()
        }
    }
    public func foo() -> Swift.String {
        return EnumWithMembers_foo(self.__externalRCRef())
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
    return main.EnumWithAbstractMembers(__externalRCRefUnsafe: __root___yellow(), options: .asBestFittingWrapper)
}

@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_common
import KotlinRuntime
import KotlinRuntimeSupport

public typealias ClassC = ExportedKotlinPackages.org.kotlin.foo.ClassC
public typealias ClassC_SealedType = ExportedKotlinPackages.org.kotlin.foo.ClassC_SealedType
public typealias ClassD = ExportedKotlinPackages.org.kotlin.foo.ClassD
public typealias ClassD_SealedType = ExportedKotlinPackages.org.kotlin.foo.ClassD_SealedType
public typealias ClassE = ExportedKotlinPackages.org.kotlin.foo.ClassE
public typealias ClassE_SealedType = ExportedKotlinPackages.org.kotlin.foo.ClassE_SealedType
@available(*, unavailable, message: "unavailable")
public typealias DeprecatedErrorSubClass = ExportedKotlinPackages.org.kotlin.foo.DeprecatedErrorSubClass
@available(*, unavailable, message: "unavailable")
public typealias DeprecatedErrorSubClass_SealedType = ExportedKotlinPackages.org.kotlin.foo.DeprecatedErrorSubClass_SealedType
@available(*, deprecated, message: "deprecated")
public typealias DeprecatedWarningSubClass = ExportedKotlinPackages.org.kotlin.foo.DeprecatedWarningSubClass
@available(*, deprecated, message: "deprecated")
public typealias DeprecatedWarningSubClass_SealedType = ExportedKotlinPackages.org.kotlin.foo.DeprecatedWarningSubClass_SealedType
public typealias InterfaceC = ExportedKotlinPackages.org.kotlin.foo.InterfaceC
public typealias InterfaceC_SealedType = ExportedKotlinPackages.org.kotlin.foo.InterfaceC_SealedType
public typealias MyClassA = ExportedKotlinPackages.org.kotlin.foo.MyClassA
public typealias MyClassB = ExportedKotlinPackages.org.kotlin.foo.MyClassB
public typealias MySealedClass = ExportedKotlinPackages.org.kotlin.foo.MySealedClass
public typealias MySealedClass_SealedType = ExportedKotlinPackages.org.kotlin.foo.MySealedClass_SealedType
@available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.org.kotlin.foo.SealedClassDeprecatedError")
public typealias NonDeprecatedSubClassA = ExportedKotlinPackages.org.kotlin.foo.NonDeprecatedSubClassA
@available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.org.kotlin.foo.SealedClassDeprecatedError")
public typealias NonDeprecatedSubClassA_SealedType = ExportedKotlinPackages.org.kotlin.foo.NonDeprecatedSubClassA_SealedType
public typealias NonDeprecatedSubClassB = ExportedKotlinPackages.org.kotlin.foo.NonDeprecatedSubClassB
public typealias NonDeprecatedSubClassB_SealedType = ExportedKotlinPackages.org.kotlin.foo.NonDeprecatedSubClassB_SealedType
public typealias NonSealedNonOptInClassA = ExportedKotlinPackages.org.kotlin.foo.NonSealedNonOptInClassA
public typealias NonSealedNonOptInClassA_SealedType = ExportedKotlinPackages.org.kotlin.foo.NonSealedNonOptInClassA_SealedType
@_spi(org$kotlin$foo$OptInA)
public typealias NonSealedNonOptInClassB = ExportedKotlinPackages.org.kotlin.foo.NonSealedNonOptInClassB
@_spi(org$kotlin$foo$OptInA)
public typealias NonSealedNonOptInClassB_SealedType = ExportedKotlinPackages.org.kotlin.foo.NonSealedNonOptInClassB_SealedType
@_spi(org$kotlin$foo$OptInA) @_spi(org$kotlin$foo$OptInB)
public typealias NonSealedOptInClass = ExportedKotlinPackages.org.kotlin.foo.NonSealedOptInClass
@_spi(org$kotlin$foo$OptInA) @_spi(org$kotlin$foo$OptInB)
public typealias NonSealedOptInClass_SealedType = ExportedKotlinPackages.org.kotlin.foo.NonSealedOptInClass_SealedType
public typealias QueryResult = ExportedKotlinPackages.org.kotlin.foo.QueryResult
public typealias SealedClassA = ExportedKotlinPackages.org.kotlin.foo.SealedClassA
public typealias SealedClassA_SealedType = ExportedKotlinPackages.org.kotlin.foo.SealedClassA_SealedType
public typealias SealedClassB = ExportedKotlinPackages.org.kotlin.foo.SealedClassB
public typealias SealedClassB_SealedType = ExportedKotlinPackages.org.kotlin.foo.SealedClassB_SealedType
@available(*, unavailable, message: "unavailable")
public typealias SealedClassDeprecatedError = ExportedKotlinPackages.org.kotlin.foo.SealedClassDeprecatedError
@available(*, unavailable, message: "unavailable")
public typealias SealedClassDeprecatedError_SealedType = ExportedKotlinPackages.org.kotlin.foo.SealedClassDeprecatedError_SealedType
@available(*, deprecated, message: "deprecated")
public typealias SealedClassDeprecatedWarning = ExportedKotlinPackages.org.kotlin.foo.SealedClassDeprecatedWarning
@available(*, deprecated, message: "deprecated")
public typealias SealedClassDeprecatedWarning_SealedType = ExportedKotlinPackages.org.kotlin.foo.SealedClassDeprecatedWarning_SealedType
public typealias SealedClassNonDeprecated = ExportedKotlinPackages.org.kotlin.foo.SealedClassNonDeprecated
public typealias SealedClassNonDeprecated_SealedType = ExportedKotlinPackages.org.kotlin.foo.SealedClassNonDeprecated_SealedType
public typealias SealedInterfaceA = ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceA
public typealias SealedInterfaceA_SealedType = ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceA_SealedType
public typealias SealedInterfaceB = ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceB
public typealias SealedInterfaceB_SealedType = ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceB_SealedType
public typealias SealedNonOptInClass = ExportedKotlinPackages.org.kotlin.foo.SealedNonOptInClass
public typealias SealedNonOptInClass_SealedType = ExportedKotlinPackages.org.kotlin.foo.SealedNonOptInClass_SealedType
@_spi(org$kotlin$foo$OptInA)
public typealias SealedOptInClass = ExportedKotlinPackages.org.kotlin.foo.SealedOptInClass
@_spi(org$kotlin$foo$OptInA)
public typealias SealedOptInClass_SealedType = ExportedKotlinPackages.org.kotlin.foo.SealedOptInClass_SealedType
public typealias _InterfaceC = ExportedKotlinPackages.org.kotlin.foo._InterfaceC
public typealias _QueryResult = ExportedKotlinPackages.org.kotlin.foo._QueryResult
public typealias _SealedInterfaceA = ExportedKotlinPackages.org.kotlin.foo._SealedInterfaceA
public typealias _SealedInterfaceB = ExportedKotlinPackages.org.kotlin.foo._SealedInterfaceB
public typealias __InterfaceC = ExportedKotlinPackages.org.kotlin.foo.__InterfaceC
public typealias __QueryResult = ExportedKotlinPackages.org.kotlin.foo.__QueryResult
public typealias __SealedInterfaceA = ExportedKotlinPackages.org.kotlin.foo.__SealedInterfaceA
public typealias __SealedInterfaceB = ExportedKotlinPackages.org.kotlin.foo.__SealedInterfaceB
public final class _ExportedKotlinPackages_org_kotlin_foo_QueryResult_AsyncValue: KotlinRuntime.KotlinBase {
    public var value: (any KotlinRuntimeSupport._KotlinBridgeable)? {
        get {
            return { switch org_kotlin_foo_QueryResult_AsyncValue_value_get(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
    }
    public init(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) {
        let __kt = org_kotlin_foo_QueryResult_AsyncValue_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { org_kotlin_foo_QueryResult_AsyncValue_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(__kt, value.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class _ExportedKotlinPackages_org_kotlin_foo_QueryResult_Value: KotlinRuntime.KotlinBase {
    public var value: (any KotlinRuntimeSupport._KotlinBridgeable)? {
        get {
            return { switch org_kotlin_foo_QueryResult_Value_value_get(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
    }
    public init(
        value: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) {
        let __kt = org_kotlin_foo_QueryResult_Value_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { org_kotlin_foo_QueryResult_Value_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(__kt, value.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
extension ExportedKotlinPackages.org.kotlin.foo.InterfaceC where Self : ExportedKotlinPackages.org.kotlin.foo.__InterfaceC {
    public func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceA_SealedType {
        .interfaceC(.init(self))
    }
}
extension ExportedKotlinPackages.org.kotlin.foo.InterfaceC {
}
extension ExportedKotlinPackages.org.kotlin.foo.QueryResult where Self : ExportedKotlinPackages.org.kotlin.foo.__QueryResult {
}
extension ExportedKotlinPackages.org.kotlin.foo.QueryResult {
    typealias AsyncValue = common._ExportedKotlinPackages_org_kotlin_foo_QueryResult_AsyncValue
    typealias Value = common._ExportedKotlinPackages_org_kotlin_foo_QueryResult_Value
}
extension ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceA where Self : ExportedKotlinPackages.org.kotlin.foo.__SealedInterfaceA {
    public func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceA_SealedType {
        .unknown(.init(self))
    }
}
extension ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceA {
}
extension ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceB where Self : ExportedKotlinPackages.org.kotlin.foo.__SealedInterfaceB {
    public func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceA_SealedType {
        .sealedInterfaceB(.init(self))
    }
}
extension ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceB {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.org.kotlin.foo.QueryResult, ExportedKotlinPackages.org.kotlin.foo.__QueryResult where Wrapped : ExportedKotlinPackages.org.kotlin.foo._QueryResult {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceA, ExportedKotlinPackages.org.kotlin.foo.__SealedInterfaceA where Wrapped : ExportedKotlinPackages.org.kotlin.foo._SealedInterfaceA {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceB, ExportedKotlinPackages.org.kotlin.foo.__SealedInterfaceB where Wrapped : ExportedKotlinPackages.org.kotlin.foo._SealedInterfaceB {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.org.kotlin.foo.InterfaceC, ExportedKotlinPackages.org.kotlin.foo.__InterfaceC where Wrapped : ExportedKotlinPackages.org.kotlin.foo._InterfaceC {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.org.kotlin.foo._QueryResult {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.org.kotlin.foo._SealedInterfaceA {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.org.kotlin.foo._SealedInterfaceB {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.org.kotlin.foo._InterfaceC {
}
extension ExportedKotlinPackages.org.kotlin.foo {
    public enum MySealedClass_SealedType: KotlinRuntimeSupport.SealedType {
        case myClassAInner(ExportedKotlinPackages.org.kotlin.foo.MyClassA.Inner_SealedType)
        case myClassBInner(ExportedKotlinPackages.org.kotlin.foo.MyClassB.Inner_SealedType)
        public var value: ExportedKotlinPackages.org.kotlin.foo.MySealedClass {
            get {
                switch self {
                case let .myClassAInner(type): type.value
                case let .myClassBInner(type): type.value
                }
            }
        }
    }
    public enum SealedClassA_SealedType: KotlinRuntimeSupport.SealedType {
        case classC(ExportedKotlinPackages.org.kotlin.foo.ClassC_SealedType)
        case sealedClassB(ExportedKotlinPackages.org.kotlin.foo.SealedClassB_SealedType)
        case unknown(ExportedKotlinPackages.org.kotlin.foo.SealedClassA_SealedType.Unknown)
        public struct Unknown: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.org.kotlin.foo.SealedClassA
            init(
                _ value: ExportedKotlinPackages.org.kotlin.foo.SealedClassA
            ) {
                self.value = value
            }
        }
        public var value: ExportedKotlinPackages.org.kotlin.foo.SealedClassA {
            get {
                switch self {
                case let .classC(type): type.value
                case let .sealedClassB(type): type.value
                case let .unknown(type): type.value
                }
            }
        }
    }
    public enum SealedClassB_SealedType: KotlinRuntimeSupport.SealedType {
        case classD(ExportedKotlinPackages.org.kotlin.foo.ClassD_SealedType)
        public var value: ExportedKotlinPackages.org.kotlin.foo.SealedClassB {
            get {
                switch self {
                case let .classD(type): type.value
                }
            }
        }
    }
    @available(*, unavailable, message: "unavailable")
    public enum SealedClassDeprecatedError_SealedType: KotlinRuntimeSupport.SealedType {
        case unknown(ExportedKotlinPackages.org.kotlin.foo.SealedClassDeprecatedError_SealedType.Unknown)
        @available(*, unavailable, message: "unavailable")
        public struct Unknown: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.org.kotlin.foo.SealedClassDeprecatedError
            init(
                _ value: ExportedKotlinPackages.org.kotlin.foo.SealedClassDeprecatedError
            ) {
                self.value = value
            }
        }
        public var value: ExportedKotlinPackages.org.kotlin.foo.SealedClassDeprecatedError {
            get {
                switch self {
                case let .unknown(type): type.value
                }
            }
        }
    }
    @available(*, deprecated, message: "deprecated")
    public enum SealedClassDeprecatedWarning_SealedType: KotlinRuntimeSupport.SealedType {
        case nonDeprecatedSubClassB(ExportedKotlinPackages.org.kotlin.foo.NonDeprecatedSubClassB_SealedType)
        public var value: ExportedKotlinPackages.org.kotlin.foo.SealedClassDeprecatedWarning {
            get {
                switch self {
                case let .nonDeprecatedSubClassB(type): type.value
                }
            }
        }
    }
    public enum SealedClassNonDeprecated_SealedType: KotlinRuntimeSupport.SealedType {
        @available(*, deprecated, message: "deprecated")
        case deprecatedWarningSubClass(ExportedKotlinPackages.org.kotlin.foo.DeprecatedWarningSubClass_SealedType)
        case unknown(ExportedKotlinPackages.org.kotlin.foo.SealedClassNonDeprecated_SealedType.Unknown)
        public struct Unknown: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.org.kotlin.foo.SealedClassNonDeprecated
            init(
                _ value: ExportedKotlinPackages.org.kotlin.foo.SealedClassNonDeprecated
            ) {
                self.value = value
            }
        }
        public var value: ExportedKotlinPackages.org.kotlin.foo.SealedClassNonDeprecated {
            get {
                switch self {
                case let .deprecatedWarningSubClass(type): type.value
                case let .unknown(type): type.value
                }
            }
        }
    }
    public enum SealedInterfaceA_SealedType: KotlinRuntimeSupport.SealedType {
        case classE(ExportedKotlinPackages.org.kotlin.foo.ClassE_SealedType)
        case interfaceC(ExportedKotlinPackages.org.kotlin.foo.InterfaceC_SealedType)
        case sealedClassA(ExportedKotlinPackages.org.kotlin.foo.SealedClassA_SealedType)
        case sealedInterfaceB(ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceB_SealedType)
        case unknown(ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceA_SealedType.Unknown)
        public struct Unknown: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceA
            init(
                _ value: ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceA
            ) {
                self.value = value
            }
        }
        public var value: ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceA {
            get {
                switch self {
                case let .classE(type): type.value
                case let .interfaceC(type): type.value
                case let .sealedClassA(type): type.value
                case let .sealedInterfaceB(type): type.value
                case let .unknown(type): type.value
                }
            }
        }
    }
    public enum SealedNonOptInClass_SealedType: KotlinRuntimeSupport.SealedType {
        case nonSealedNonOptInClassA(ExportedKotlinPackages.org.kotlin.foo.NonSealedNonOptInClassA_SealedType)
        @_spi(org$kotlin$foo$OptInA)
        case sealedOptInClass(ExportedKotlinPackages.org.kotlin.foo.SealedOptInClass_SealedType)
        public var value: ExportedKotlinPackages.org.kotlin.foo.SealedNonOptInClass {
            get {
                switch self {
                case let .nonSealedNonOptInClassA(type): type.value
                case let .sealedOptInClass(type): type.value
                }
            }
        }
    }
    @_spi(org$kotlin$foo$OptInA)
    public enum SealedOptInClass_SealedType: KotlinRuntimeSupport.SealedType {
        @_spi(org$kotlin$foo$OptInA)
        case nonSealedNonOptInClassB(ExportedKotlinPackages.org.kotlin.foo.NonSealedNonOptInClassB_SealedType)
        @_spi(org$kotlin$foo$OptInA) @_spi(org$kotlin$foo$OptInB)
        case nonSealedOptInClass(ExportedKotlinPackages.org.kotlin.foo.NonSealedOptInClass_SealedType)
        public var value: ExportedKotlinPackages.org.kotlin.foo.SealedOptInClass {
            get {
                switch self {
                case let .nonSealedNonOptInClassB(type): type.value
                case let .nonSealedOptInClass(type): type.value
                }
            }
        }
    }
    public protocol InterfaceC: KotlinRuntime.KotlinBase, ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceA, ExportedKotlinPackages.org.kotlin.foo._InterfaceC {
        func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceA_SealedType
    }
    public protocol QueryResult: KotlinRuntime.KotlinBase, ExportedKotlinPackages.org.kotlin.foo._QueryResult {
    }
    public protocol SealedInterfaceA: KotlinRuntime.KotlinBase, ExportedKotlinPackages.org.kotlin.foo._SealedInterfaceA {
        func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceA_SealedType
    }
    public protocol SealedInterfaceB: KotlinRuntime.KotlinBase, ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceA, ExportedKotlinPackages.org.kotlin.foo._SealedInterfaceB {
        func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceA_SealedType
    }
    @objc(_InterfaceC)
    public protocol _InterfaceC: ExportedKotlinPackages.org.kotlin.foo._SealedInterfaceA {
    }
    @objc(_QueryResult)
    public protocol _QueryResult {
    }
    @objc(_SealedInterfaceA)
    public protocol _SealedInterfaceA {
    }
    @objc(_SealedInterfaceB)
    public protocol _SealedInterfaceB: ExportedKotlinPackages.org.kotlin.foo._SealedInterfaceA {
    }
    public protocol __InterfaceC: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.org.kotlin.foo.__SealedInterfaceA {
    }
    public protocol __QueryResult: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol __SealedInterfaceA: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol __SealedInterfaceB: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.org.kotlin.foo.__SealedInterfaceA {
    }
    public final class ClassC: ExportedKotlinPackages.org.kotlin.foo.SealedClassA {
        public init() {
            let __kt = org_kotlin_foo_ClassC_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { org_kotlin_foo_ClassC_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public override func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedClassA_SealedType {
            .classC(.init(self))
        }
    }
    public final class ClassD: ExportedKotlinPackages.org.kotlin.foo.SealedClassB {
        public init() {
            let __kt = org_kotlin_foo_ClassD_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { org_kotlin_foo_ClassD_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public override func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedClassB_SealedType {
            .classD(.init(self))
        }
    }
    public final class ClassE: KotlinRuntime.KotlinBase, ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceA, ExportedKotlinPackages.org.kotlin.foo.__SealedInterfaceA {
        public init() {
            let __kt = org_kotlin_foo_ClassE_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { org_kotlin_foo_ClassE_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceA_SealedType {
            .classE(.init(self))
        }
    }
    @available(*, unavailable, message: "unavailable")
    public final class DeprecatedErrorSubClass: ExportedKotlinPackages.org.kotlin.foo.SealedClassNonDeprecated {
        public init() {
            let __kt = org_kotlin_foo_DeprecatedErrorSubClass_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { org_kotlin_foo_DeprecatedErrorSubClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public override func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedClassNonDeprecated_SealedType {
            .unknown(.init(self))
        }
    }
    @available(*, deprecated, message: "deprecated")
    public final class DeprecatedWarningSubClass: ExportedKotlinPackages.org.kotlin.foo.SealedClassNonDeprecated {
        public init() {
            let __kt = org_kotlin_foo_DeprecatedWarningSubClass_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { org_kotlin_foo_DeprecatedWarningSubClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public override func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedClassNonDeprecated_SealedType {
            .deprecatedWarningSubClass(.init(self))
        }
    }
    public final class MyClassA: KotlinRuntime.KotlinBase {
        open class Inner: ExportedKotlinPackages.org.kotlin.foo.MySealedClass {
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            public final override func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.MySealedClass_SealedType {
                .myClassAInner(.init(self))
            }
        }
        public struct Inner_SealedType: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.org.kotlin.foo.MyClassA.Inner
            init(
                _ value: ExportedKotlinPackages.org.kotlin.foo.MyClassA.Inner
            ) {
                self.value = value
            }
        }
        public init() {
            let __kt = org_kotlin_foo_MyClassA_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { org_kotlin_foo_MyClassA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class MyClassB: KotlinRuntime.KotlinBase {
        open class Inner: ExportedKotlinPackages.org.kotlin.foo.MySealedClass {
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            public final override func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.MySealedClass_SealedType {
                .myClassBInner(.init(self))
            }
        }
        public struct Inner_SealedType: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.org.kotlin.foo.MyClassB.Inner
            init(
                _ value: ExportedKotlinPackages.org.kotlin.foo.MyClassB.Inner
            ) {
                self.value = value
            }
        }
        public init() {
            let __kt = org_kotlin_foo_MyClassB_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { org_kotlin_foo_MyClassB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    open class MySealedClass: KotlinRuntime.KotlinBase {
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        open func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.MySealedClass_SealedType {
            fatalError("must implement sealedType in subclass")
        }
    }
    @available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.org.kotlin.foo.SealedClassDeprecatedError")
    public final class NonDeprecatedSubClassA: ExportedKotlinPackages.org.kotlin.foo.SealedClassDeprecatedError {
        public init() {
            let __kt = org_kotlin_foo_NonDeprecatedSubClassA_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { org_kotlin_foo_NonDeprecatedSubClassA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @available(*, unavailable, message: "unavailable")
        public override func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedClassDeprecatedError_SealedType {
            .unknown(.init(self))
        }
    }
    public final class NonDeprecatedSubClassB: ExportedKotlinPackages.org.kotlin.foo.SealedClassDeprecatedWarning {
        public init() {
            let __kt = org_kotlin_foo_NonDeprecatedSubClassB_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { org_kotlin_foo_NonDeprecatedSubClassB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @available(*, deprecated, message: "deprecated")
        public override func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedClassDeprecatedWarning_SealedType {
            .nonDeprecatedSubClassB(.init(self))
        }
    }
    public final class NonSealedNonOptInClassA: ExportedKotlinPackages.org.kotlin.foo.SealedNonOptInClass {
        public init() {
            let __kt = org_kotlin_foo_NonSealedNonOptInClassA_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { org_kotlin_foo_NonSealedNonOptInClassA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public override func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedNonOptInClass_SealedType {
            .nonSealedNonOptInClassA(.init(self))
        }
    }
    @_spi(org$kotlin$foo$OptInA)
    public final class NonSealedNonOptInClassB: ExportedKotlinPackages.org.kotlin.foo.SealedOptInClass {
        @_spi(org$kotlin$foo$OptInA)
        public init() {
            let __kt = org_kotlin_foo_NonSealedNonOptInClassB_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { org_kotlin_foo_NonSealedNonOptInClassB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @_spi(org$kotlin$foo$OptInA)
        public override func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedOptInClass_SealedType {
            .nonSealedNonOptInClassB(.init(self))
        }
    }
    @_spi(org$kotlin$foo$OptInA) @_spi(org$kotlin$foo$OptInB)
    public final class NonSealedOptInClass: ExportedKotlinPackages.org.kotlin.foo.SealedOptInClass {
        @_spi(org$kotlin$foo$OptInA) @_spi(org$kotlin$foo$OptInB)
        public init() {
            let __kt = org_kotlin_foo_NonSealedOptInClass_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { org_kotlin_foo_NonSealedOptInClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @_spi(org$kotlin$foo$OptInA)
        public override func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedOptInClass_SealedType {
            .nonSealedOptInClass(.init(self))
        }
    }
    open class SealedClassA: KotlinRuntime.KotlinBase, ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceA, ExportedKotlinPackages.org.kotlin.foo.__SealedInterfaceA {
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        open func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedClassA_SealedType {
            .unknown(.init(self))
        }
        @_disfavoredOverload
        public final func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceA_SealedType {
            .sealedClassA(sealedType())
        }
    }
    open class SealedClassB: ExportedKotlinPackages.org.kotlin.foo.SealedClassA {
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        open func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedClassB_SealedType {
            fatalError("must implement sealedType in subclass")
        }
        @_disfavoredOverload
        public final override func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedClassA_SealedType {
            .sealedClassB(sealedType())
        }
    }
    @available(*, unavailable, message: "unavailable")
    open class SealedClassDeprecatedError: KotlinRuntime.KotlinBase {
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @available(*, unavailable, message: "unavailable")
        open func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedClassDeprecatedError_SealedType {
            .unknown(.init(self))
        }
    }
    @available(*, deprecated, message: "deprecated")
    open class SealedClassDeprecatedWarning: KotlinRuntime.KotlinBase {
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @available(*, deprecated, message: "deprecated")
        open func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedClassDeprecatedWarning_SealedType {
            fatalError("must implement sealedType in subclass")
        }
    }
    open class SealedClassNonDeprecated: KotlinRuntime.KotlinBase {
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        open func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedClassNonDeprecated_SealedType {
            .unknown(.init(self))
        }
    }
    open class SealedNonOptInClass: KotlinRuntime.KotlinBase {
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        open func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedNonOptInClass_SealedType {
            fatalError("must implement sealedType in subclass")
        }
    }
    @_spi(org$kotlin$foo$OptInA)
    open class SealedOptInClass: ExportedKotlinPackages.org.kotlin.foo.SealedNonOptInClass {
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        @_spi(org$kotlin$foo$OptInA)
        open func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedOptInClass_SealedType {
            fatalError("must implement sealedType in subclass")
        }
        @_disfavoredOverload
        public final override func sealedType() -> ExportedKotlinPackages.org.kotlin.foo.SealedNonOptInClass_SealedType {
            .sealedOptInClass(sealedType())
        }
    }
    public struct ClassC_SealedType: KotlinRuntimeSupport.SealedType {
        public let value: ExportedKotlinPackages.org.kotlin.foo.ClassC
        init(
            _ value: ExportedKotlinPackages.org.kotlin.foo.ClassC
        ) {
            self.value = value
        }
    }
    public struct ClassD_SealedType: KotlinRuntimeSupport.SealedType {
        public let value: ExportedKotlinPackages.org.kotlin.foo.ClassD
        init(
            _ value: ExportedKotlinPackages.org.kotlin.foo.ClassD
        ) {
            self.value = value
        }
    }
    public struct ClassE_SealedType: KotlinRuntimeSupport.SealedType {
        public let value: ExportedKotlinPackages.org.kotlin.foo.ClassE
        init(
            _ value: ExportedKotlinPackages.org.kotlin.foo.ClassE
        ) {
            self.value = value
        }
    }
    @available(*, unavailable, message: "unavailable")
    public struct DeprecatedErrorSubClass_SealedType: KotlinRuntimeSupport.SealedType {
        public let value: ExportedKotlinPackages.org.kotlin.foo.DeprecatedErrorSubClass
        init(
            _ value: ExportedKotlinPackages.org.kotlin.foo.DeprecatedErrorSubClass
        ) {
            self.value = value
        }
    }
    @available(*, deprecated, message: "deprecated")
    public struct DeprecatedWarningSubClass_SealedType: KotlinRuntimeSupport.SealedType {
        public let value: ExportedKotlinPackages.org.kotlin.foo.DeprecatedWarningSubClass
        init(
            _ value: ExportedKotlinPackages.org.kotlin.foo.DeprecatedWarningSubClass
        ) {
            self.value = value
        }
    }
    public struct InterfaceC_SealedType: KotlinRuntimeSupport.SealedType {
        public let value: ExportedKotlinPackages.org.kotlin.foo.InterfaceC
        init(
            _ value: ExportedKotlinPackages.org.kotlin.foo.InterfaceC
        ) {
            self.value = value
        }
    }
    @available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.org.kotlin.foo.SealedClassDeprecatedError")
    public struct NonDeprecatedSubClassA_SealedType: KotlinRuntimeSupport.SealedType {
        public let value: ExportedKotlinPackages.org.kotlin.foo.NonDeprecatedSubClassA
        init(
            _ value: ExportedKotlinPackages.org.kotlin.foo.NonDeprecatedSubClassA
        ) {
            self.value = value
        }
    }
    public struct NonDeprecatedSubClassB_SealedType: KotlinRuntimeSupport.SealedType {
        public let value: ExportedKotlinPackages.org.kotlin.foo.NonDeprecatedSubClassB
        init(
            _ value: ExportedKotlinPackages.org.kotlin.foo.NonDeprecatedSubClassB
        ) {
            self.value = value
        }
    }
    public struct NonSealedNonOptInClassA_SealedType: KotlinRuntimeSupport.SealedType {
        public let value: ExportedKotlinPackages.org.kotlin.foo.NonSealedNonOptInClassA
        init(
            _ value: ExportedKotlinPackages.org.kotlin.foo.NonSealedNonOptInClassA
        ) {
            self.value = value
        }
    }
    @_spi(org$kotlin$foo$OptInA)
    public struct NonSealedNonOptInClassB_SealedType: KotlinRuntimeSupport.SealedType {
        public let value: ExportedKotlinPackages.org.kotlin.foo.NonSealedNonOptInClassB
        init(
            _ value: ExportedKotlinPackages.org.kotlin.foo.NonSealedNonOptInClassB
        ) {
            self.value = value
        }
    }
    @_spi(org$kotlin$foo$OptInA) @_spi(org$kotlin$foo$OptInB)
    public struct NonSealedOptInClass_SealedType: KotlinRuntimeSupport.SealedType {
        public let value: ExportedKotlinPackages.org.kotlin.foo.NonSealedOptInClass
        init(
            _ value: ExportedKotlinPackages.org.kotlin.foo.NonSealedOptInClass
        ) {
            self.value = value
        }
    }
    public struct SealedInterfaceB_SealedType: KotlinRuntimeSupport.SealedType {
        public let value: ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceB
        init(
            _ value: ExportedKotlinPackages.org.kotlin.foo.SealedInterfaceB
        ) {
            self.value = value
        }
    }
}

@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_funinterface
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.funinterface.FunctionalInterface where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func callAsFunction() -> Swift.Int32 {
        return funinterface_FunctionalInterface_invoke(self.__externalRCRef())
    }
}
extension ExportedKotlinPackages.funinterface.FunctionalInterface {
}
extension ExportedKotlinPackages.funinterface.XMLFunctionalInterfaceWithLeadingAbbreviation where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func callAsFunction() -> Swift.Int32 {
        return funinterface_XMLFunctionalInterfaceWithLeadingAbbreviation_invoke(self.__externalRCRef())
    }
}
extension ExportedKotlinPackages.funinterface.XMLFunctionalInterfaceWithLeadingAbbreviation {
}
extension ExportedKotlinPackages.funinterface._123FunctionalInterfaceWithLeadingNumbers where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func callAsFunction() -> Swift.Int32 {
        return funinterface__123FunctionalInterfaceWithLeadingNumbers_invoke(self.__externalRCRef())
    }
}
extension ExportedKotlinPackages.funinterface._123FunctionalInterfaceWithLeadingNumbers {
}
extension ExportedKotlinPackages.funinterface._123XMLFunctionalInterfaceWithLeadingUnderscoreNumbersAndAbbreviation where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func callAsFunction() -> Swift.Int32 {
        return funinterface__123XMLFunctionalInterfaceWithLeadingUnderscoreNumbersAndAbbreviation_invoke(self.__externalRCRef())
    }
}
extension ExportedKotlinPackages.funinterface._123XMLFunctionalInterfaceWithLeadingUnderscoreNumbersAndAbbreviation {
}
extension ExportedKotlinPackages.funinterface._FunctionalInterfaceWithLeadingUnderscore where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func callAsFunction() -> Swift.Int32 {
        return funinterface__FunctionalInterfaceWithLeadingUnderscore_invoke(self.__externalRCRef())
    }
}
extension ExportedKotlinPackages.funinterface._FunctionalInterfaceWithLeadingUnderscore {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.funinterface.FunctionalInterface where Wrapped : ExportedKotlinPackages.funinterface._FunctionalInterface {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.funinterface._FunctionalInterfaceWithLeadingUnderscore where Wrapped : ExportedKotlinPackages.funinterface.__FunctionalInterfaceWithLeadingUnderscore {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.funinterface._123FunctionalInterfaceWithLeadingNumbers where Wrapped : ExportedKotlinPackages.funinterface.__123FunctionalInterfaceWithLeadingNumbers {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.funinterface.XMLFunctionalInterfaceWithLeadingAbbreviation where Wrapped : ExportedKotlinPackages.funinterface._XMLFunctionalInterfaceWithLeadingAbbreviation {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.funinterface._123XMLFunctionalInterfaceWithLeadingUnderscoreNumbersAndAbbreviation where Wrapped : ExportedKotlinPackages.funinterface.__123XMLFunctionalInterfaceWithLeadingUnderscoreNumbersAndAbbreviation {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.funinterface.functionalInterfaceWithAlreadyLowercaseLeading where Wrapped : ExportedKotlinPackages.funinterface._functionalInterfaceWithAlreadyLowercaseLeading {
}
extension ExportedKotlinPackages.funinterface.functionalInterfaceWithAlreadyLowercaseLeading where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func callAsFunction() -> Swift.Int32 {
        return funinterface_functionalInterfaceWithAlreadyLowercaseLeading_invoke(self.__externalRCRef())
    }
}
extension ExportedKotlinPackages.funinterface.functionalInterfaceWithAlreadyLowercaseLeading {
}
extension ExportedKotlinPackages.funinterface {
    public protocol FunctionalInterface: KotlinRuntime.KotlinBase {
        func callAsFunction() -> Swift.Int32
    }
    public protocol XMLFunctionalInterfaceWithLeadingAbbreviation: KotlinRuntime.KotlinBase {
        func callAsFunction() -> Swift.Int32
    }
    public protocol _123FunctionalInterfaceWithLeadingNumbers: KotlinRuntime.KotlinBase {
        func callAsFunction() -> Swift.Int32
    }
    public protocol _123XMLFunctionalInterfaceWithLeadingUnderscoreNumbersAndAbbreviation: KotlinRuntime.KotlinBase {
        func callAsFunction() -> Swift.Int32
    }
    @objc(_FunctionalInterface)
    package protocol _FunctionalInterface {
    }
    public protocol _FunctionalInterfaceWithLeadingUnderscore: KotlinRuntime.KotlinBase {
        func callAsFunction() -> Swift.Int32
    }
    @objc(_XMLFunctionalInterfaceWithLeadingAbbreviation)
    package protocol _XMLFunctionalInterfaceWithLeadingAbbreviation {
    }
    @objc(__123FunctionalInterfaceWithLeadingNumbers)
    package protocol __123FunctionalInterfaceWithLeadingNumbers {
    }
    @objc(__123XMLFunctionalInterfaceWithLeadingUnderscoreNumbersAndAbbreviation)
    package protocol __123XMLFunctionalInterfaceWithLeadingUnderscoreNumbersAndAbbreviation {
    }
    @objc(__FunctionalInterfaceWithLeadingUnderscore)
    package protocol __FunctionalInterfaceWithLeadingUnderscore {
    }
    @objc(_functionalInterfaceWithAlreadyLowercaseLeading)
    package protocol _functionalInterfaceWithAlreadyLowercaseLeading {
    }
    public protocol functionalInterfaceWithAlreadyLowercaseLeading: KotlinRuntime.KotlinBase {
        func callAsFunction() -> Swift.Int32
    }
    public final class FunctorClass: KotlinRuntime.KotlinBase, ExportedKotlinPackages.funinterface.FunctionalInterface, ExportedKotlinPackages.funinterface._FunctionalInterface {
        public init() {
            if Self.self != ExportedKotlinPackages.funinterface.FunctorClass.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.funinterface.FunctorClass ") }
            let __kt = funinterface_FunctorClass_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            funinterface_FunctorClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public func callAsFunction() -> Swift.Int32 {
            return funinterface_FunctorClass_invoke(self.__externalRCRef())
        }
    }
    public static func _123functionalInterfaceWithLeadingNumbers(
        function: @escaping () -> Swift.Int32
    ) -> any ExportedKotlinPackages.funinterface._123FunctionalInterfaceWithLeadingNumbers {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: funinterface__123FunctionalInterfaceWithLeadingNumbers__TypesOfArguments__U2829202D_U20Swift_Int32__({
            let originalBlock = function
            return { return originalBlock() }
        }())) as! any ExportedKotlinPackages.funinterface._123FunctionalInterfaceWithLeadingNumbers
    }
    public static func _123xmlFunctionalInterfaceWithLeadingUnderscoreNumbersAndAbbreviation(
        function: @escaping () -> Swift.Int32
    ) -> any ExportedKotlinPackages.funinterface._123XMLFunctionalInterfaceWithLeadingUnderscoreNumbersAndAbbreviation {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: funinterface__123XMLFunctionalInterfaceWithLeadingUnderscoreNumbersAndAbbreviation__TypesOfArguments__U2829202D_U20Swift_Int32__({
            let originalBlock = function
            return { return originalBlock() }
        }())) as! any ExportedKotlinPackages.funinterface._123XMLFunctionalInterfaceWithLeadingUnderscoreNumbersAndAbbreviation
    }
    public static func _functionalInterfaceWithLeadingUnderscore(
        function: @escaping () -> Swift.Int32
    ) -> any ExportedKotlinPackages.funinterface._FunctionalInterfaceWithLeadingUnderscore {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: funinterface__FunctionalInterfaceWithLeadingUnderscore__TypesOfArguments__U2829202D_U20Swift_Int32__({
            let originalBlock = function
            return { return originalBlock() }
        }())) as! any ExportedKotlinPackages.funinterface._FunctionalInterfaceWithLeadingUnderscore
    }
    public static func functionalInterface(
        function: @escaping () -> Swift.Int32
    ) -> any ExportedKotlinPackages.funinterface.FunctionalInterface {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: funinterface_FunctionalInterface__TypesOfArguments__U2829202D_U20Swift_Int32__({
            let originalBlock = function
            return { return originalBlock() }
        }())) as! any ExportedKotlinPackages.funinterface.FunctionalInterface
    }
    public static func functionalInterfaceWithAlreadyLowercaseLeadingFromFunction(
        function: @escaping () -> Swift.Int32
    ) -> any ExportedKotlinPackages.funinterface.functionalInterfaceWithAlreadyLowercaseLeading {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: funinterface_functionalInterfaceWithAlreadyLowercaseLeading__TypesOfArguments__U2829202D_U20Swift_Int32__({
            let originalBlock = function
            return { return originalBlock() }
        }())) as! any ExportedKotlinPackages.funinterface.functionalInterfaceWithAlreadyLowercaseLeading
    }
    public static func xmlFunctionalInterfaceWithLeadingAbbreviation(
        function: @escaping () -> Swift.Int32
    ) -> any ExportedKotlinPackages.funinterface.XMLFunctionalInterfaceWithLeadingAbbreviation {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: funinterface_XMLFunctionalInterfaceWithLeadingAbbreviation__TypesOfArguments__U2829202D_U20Swift_Int32__({
            let originalBlock = function
            return { return originalBlock() }
        }())) as! any ExportedKotlinPackages.funinterface.XMLFunctionalInterfaceWithLeadingAbbreviation
    }
}

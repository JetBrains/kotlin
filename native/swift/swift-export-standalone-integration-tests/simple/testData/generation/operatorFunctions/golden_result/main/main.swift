@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public extension ExportedKotlinPackages.generation.operatorFunctions.operatorFunctions {
    public final class Vector: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public var x: Swift.Int32 {
            get {
                return generation_operatorFunctions_operatorFunctions_Vector_x_get(self.__externalRCRef())
            }
        }
        public var y: Swift.Int32 {
            get {
                return generation_operatorFunctions_operatorFunctions_Vector_y_get(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRef: Swift.UnsafeMutableRawPointer?
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public init(
            x: Swift.Int32,
            y: Swift.Int32
        ) {
            let __kt = generation_operatorFunctions_operatorFunctions_Vector_init_allocate()
            super.init(__externalRCRef: __kt)
            generation_operatorFunctions_operatorFunctions_Vector_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Int32__(__kt, x, y)
        }
        public func compareTo(
            other: ExportedKotlinPackages.generation.operatorFunctions.operatorFunctions.Vector
        ) -> Swift.Int32 {
            return generation_operatorFunctions_operatorFunctions_Vector_compareTo__TypesOfArguments__ExportedKotlinPackages_generation_operatorFunctions_operatorFunctions_Vector__(self.__externalRCRef(), other.__externalRCRef())
        }
        public func div(
            scalar: Swift.Int32
        ) -> ExportedKotlinPackages.generation.operatorFunctions.operatorFunctions.Vector {
            return ExportedKotlinPackages.generation.operatorFunctions.operatorFunctions.Vector(__externalRCRef: generation_operatorFunctions_operatorFunctions_Vector_div__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), scalar))
        }
        public func get(
            index: Swift.Int32
        ) -> Swift.Int32 {
            return generation_operatorFunctions_operatorFunctions_Vector_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)
        }
        public func minus(
            other: ExportedKotlinPackages.generation.operatorFunctions.operatorFunctions.Vector
        ) -> ExportedKotlinPackages.generation.operatorFunctions.operatorFunctions.Vector {
            return ExportedKotlinPackages.generation.operatorFunctions.operatorFunctions.Vector(__externalRCRef: generation_operatorFunctions_operatorFunctions_Vector_minus__TypesOfArguments__ExportedKotlinPackages_generation_operatorFunctions_operatorFunctions_Vector__(self.__externalRCRef(), other.__externalRCRef()))
        }
        public func plus(
            other: ExportedKotlinPackages.generation.operatorFunctions.operatorFunctions.Vector
        ) -> ExportedKotlinPackages.generation.operatorFunctions.operatorFunctions.Vector {
            return ExportedKotlinPackages.generation.operatorFunctions.operatorFunctions.Vector(__externalRCRef: generation_operatorFunctions_operatorFunctions_Vector_plus__TypesOfArguments__ExportedKotlinPackages_generation_operatorFunctions_operatorFunctions_Vector__(self.__externalRCRef(), other.__externalRCRef()))
        }
        public func times(
            scalar: Swift.Int32
        ) -> ExportedKotlinPackages.generation.operatorFunctions.operatorFunctions.Vector {
            return ExportedKotlinPackages.generation.operatorFunctions.operatorFunctions.Vector(__externalRCRef: generation_operatorFunctions_operatorFunctions_Vector_times__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), scalar))
        }
        public func toString() -> Swift.String {
            return generation_operatorFunctions_operatorFunctions_Vector_toString(self.__externalRCRef())
        }
        public func unaryMinus() -> ExportedKotlinPackages.generation.operatorFunctions.operatorFunctions.Vector {
            return ExportedKotlinPackages.generation.operatorFunctions.operatorFunctions.Vector(__externalRCRef: generation_operatorFunctions_operatorFunctions_Vector_unaryMinus(self.__externalRCRef()))
        }
    }
}

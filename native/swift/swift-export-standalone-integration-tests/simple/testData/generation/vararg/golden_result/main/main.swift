@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib

public final class Accessor: KotlinRuntime.KotlinBase {
    public final class Inner: KotlinRuntime.KotlinBase {
        public var y: Swift.Double {
            get {
                return Accessor_Inner_y_get(self.__externalRCRef())
            }
        }
        public var z: ExportedKotlinPackages.kotlin.BooleanArray {
            get {
                return ExportedKotlinPackages.kotlin.BooleanArray.__createClassWrapper(externalRCRef: Accessor_Inner_z_get(self.__externalRCRef()))
            }
            set {
                return Accessor_Inner_z_set__TypesOfArguments__ExportedKotlinPackages_kotlin_BooleanArray__(self.__externalRCRef(), newValue.__externalRCRef())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public init(
            y: Swift.Double,
            z: ExportedKotlinPackages.kotlin.BooleanArray,
            outer__: main.Accessor
        ) {
            if Self.self != main.Accessor.Inner.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Accessor.Inner ") }
            let __kt = Accessor_Inner_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            Accessor_Inner_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Double_ExportedKotlinPackages_kotlin_BooleanArray_main_Accessor__(__kt, y, z.__externalRCRef(), outer__.__externalRCRef())
        }
    }
    public var x: ExportedKotlinPackages.kotlin.IntArray {
        get {
            return ExportedKotlinPackages.kotlin.IntArray.__createClassWrapper(externalRCRef: Accessor_x_get(self.__externalRCRef()))
        }
    }
    public init(
        x: ExportedKotlinPackages.kotlin.IntArray
    ) {
        if Self.self != main.Accessor.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Accessor ") }
        let __kt = __root___Accessor_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___Accessor_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_ExportedKotlinPackages_kotlin_IntArray__(__kt, x.__externalRCRef())
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    public func _get(
        i: Swift.Int32
    ) -> Swift.Int32 {
        return Accessor_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), i)
    }
    public subscript(
        i: Swift.Int32
    ) -> Swift.Int32 {
        get {
            _get(i: i)
        }
    }
}
public func asNumberList(
    x: ExportedKotlinPackages.kotlin.Array
) -> [ExportedKotlinPackages.kotlin.Number]? {
    return __root___asNumberList__TypesOfArguments__ExportedKotlinPackages_kotlin_Array__(x.__externalRCRef()).map { it in it as! Swift.Array<ExportedKotlinPackages.kotlin.Number> }
}
public func `extension`(
    _ receiver: main.Accessor,
    d: ExportedKotlinPackages.kotlin.DoubleArray
) -> Swift.Void {
    return __root___extension__TypesOfArguments__main_Accessor_ExportedKotlinPackages_kotlin_DoubleArray__(receiver.__externalRCRef(), d.__externalRCRef())
}
public func oneMore(
    a: ExportedKotlinPackages.kotlin.Array,
    b: Swift.Int32
) -> Swift.Void {
    return __root___oneMore__TypesOfArguments__ExportedKotlinPackages_kotlin_Array_Swift_Int32__(a.__externalRCRef(), b)
}
public func simple(
    s: ExportedKotlinPackages.kotlin.Array
) -> Swift.String {
    return __root___simple__TypesOfArguments__ExportedKotlinPackages_kotlin_Array__(s.__externalRCRef())
}
public func withDefault(
    a: ExportedKotlinPackages.kotlin.Array,
    b: Swift.Int32
) -> Swift.Void {
    return __root___withDefault__TypesOfArguments__ExportedKotlinPackages_kotlin_Array_Swift_Int32__(a.__externalRCRef(), b)
}

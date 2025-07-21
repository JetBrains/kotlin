@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public protocol INTERFACE: KotlinRuntime.KotlinBase {
}
@objc(_INTERFACE)
package protocol _INTERFACE {
}
open class ABSTRACT_CLASS: KotlinRuntime.KotlinBase {
    package init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class Class_without_package: KotlinRuntime.KotlinBase {
    public final class INNER_CLASS: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != main.Class_without_package.INNER_CLASS.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Class_without_package.INNER_CLASS ") }
            let __kt = Class_without_package_INNER_CLASS_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            Class_without_package_INNER_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class INNER_OBJECT: KotlinRuntime.KotlinBase {
        public static var shared: main.Class_without_package.INNER_OBJECT {
            get {
                return main.Class_without_package.INNER_OBJECT.__createClassWrapper(externalRCRef: Class_without_package_INNER_OBJECT_get())
            }
        }
        private init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public init() {
        if Self.self != main.Class_without_package.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Class_without_package ") }
        let __kt = __root___Class_without_package_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___Class_without_package_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class DATA_CLASS: KotlinRuntime.KotlinBase {
    public var a: Swift.Int32 {
        get {
            return DATA_CLASS_a_get(self.__externalRCRef())
        }
    }
    public init(
        a: Swift.Int32
    ) {
        if Self.self != main.DATA_CLASS.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.DATA_CLASS ") }
        let __kt = __root___DATA_CLASS_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___DATA_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt, a)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    public static func ==(
        this: main.DATA_CLASS,
        other: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        this.equals(other: other)
    }
    public func copy(
        a: Swift.Int32
    ) -> main.DATA_CLASS {
        return main.DATA_CLASS.__createClassWrapper(externalRCRef: DATA_CLASS_copy__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), a))
    }
    public func equals(
        other: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return DATA_CLASS_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
    }
    public func hashCode() -> Swift.Int32 {
        return DATA_CLASS_hashCode(self.__externalRCRef())
    }
    public func toString() -> Swift.String {
        return DATA_CLASS_toString(self.__externalRCRef())
    }
}
public final class Demo: KotlinRuntime.KotlinBase {
    public final class INNER_CLASS: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != main.Demo.INNER_CLASS.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Demo.INNER_CLASS ") }
            let __kt = Demo_INNER_CLASS_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            Demo_INNER_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class INNER_OBJECT: KotlinRuntime.KotlinBase {
        public static var shared: main.Demo.INNER_OBJECT {
            get {
                return main.Demo.INNER_OBJECT.__createClassWrapper(externalRCRef: Demo_INNER_OBJECT_get())
            }
        }
        private init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public var arg1: main.Class_without_package {
        get {
            return main.Class_without_package.__createClassWrapper(externalRCRef: Demo_arg1_get(self.__externalRCRef()))
        }
    }
    public var arg2: ExportedKotlinPackages.namespace.deeper.Class_with_package {
        get {
            return ExportedKotlinPackages.namespace.deeper.Class_with_package.__createClassWrapper(externalRCRef: Demo_arg2_get(self.__externalRCRef()))
        }
    }
    public var arg3: main.Object_without_package {
        get {
            return main.Object_without_package.__createClassWrapper(externalRCRef: Demo_arg3_get(self.__externalRCRef()))
        }
    }
    public var arg4: ExportedKotlinPackages.namespace.deeper.Object_with_package {
        get {
            return ExportedKotlinPackages.namespace.deeper.Object_with_package.__createClassWrapper(externalRCRef: Demo_arg4_get(self.__externalRCRef()))
        }
    }
    public var var1: main.Class_without_package {
        get {
            return main.Class_without_package.__createClassWrapper(externalRCRef: Demo_var1_get(self.__externalRCRef()))
        }
        set {
            return Demo_var1_set__TypesOfArguments__main_Class_without_package__(self.__externalRCRef(), newValue.__externalRCRef())
        }
    }
    public var var2: ExportedKotlinPackages.namespace.deeper.Class_with_package {
        get {
            return ExportedKotlinPackages.namespace.deeper.Class_with_package.__createClassWrapper(externalRCRef: Demo_var2_get(self.__externalRCRef()))
        }
        set {
            return Demo_var2_set__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Class_with_package__(self.__externalRCRef(), newValue.__externalRCRef())
        }
    }
    public var var3: main.Object_without_package {
        get {
            return main.Object_without_package.__createClassWrapper(externalRCRef: Demo_var3_get(self.__externalRCRef()))
        }
        set {
            return Demo_var3_set__TypesOfArguments__main_Object_without_package__(self.__externalRCRef(), newValue.__externalRCRef())
        }
    }
    public var var4: ExportedKotlinPackages.namespace.deeper.Object_with_package {
        get {
            return ExportedKotlinPackages.namespace.deeper.Object_with_package.__createClassWrapper(externalRCRef: Demo_var4_get(self.__externalRCRef()))
        }
        set {
            return Demo_var4_set__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Object_with_package__(self.__externalRCRef(), newValue.__externalRCRef())
        }
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
    public init(
        arg1: main.Class_without_package,
        arg2: ExportedKotlinPackages.namespace.deeper.Class_with_package,
        arg3: main.Object_without_package,
        arg4: ExportedKotlinPackages.namespace.deeper.Object_with_package
    ) {
        if Self.self != main.Demo.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Demo ") }
        let __kt = __root___Demo_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___Demo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Class_without_package_ExportedKotlinPackages_namespace_deeper_Class_with_package_main_Object_without_package_ExportedKotlinPackages_namespace_deeper_Object_with_package__(__kt, arg1.__externalRCRef(), arg2.__externalRCRef(), arg3.__externalRCRef(), arg4.__externalRCRef())
    }
    public func combine(
        arg1: main.Class_without_package,
        arg2: ExportedKotlinPackages.namespace.deeper.Class_with_package,
        arg3: main.Object_without_package,
        arg4: ExportedKotlinPackages.namespace.deeper.Object_with_package
    ) -> main.Demo {
        return main.Demo.__createClassWrapper(externalRCRef: Demo_combine__TypesOfArguments__main_Class_without_package_ExportedKotlinPackages_namespace_deeper_Class_with_package_main_Object_without_package_ExportedKotlinPackages_namespace_deeper_Object_with_package__(self.__externalRCRef(), arg1.__externalRCRef(), arg2.__externalRCRef(), arg3.__externalRCRef(), arg4.__externalRCRef()))
    }
    public func combine_inner_classses(
        arg1: main.Class_without_package.INNER_CLASS,
        arg2: ExportedKotlinPackages.namespace.deeper.Class_with_package.INNER_CLASS,
        arg3: main.Object_without_package.INNER_CLASS,
        arg4: ExportedKotlinPackages.namespace.deeper.Object_with_package.INNER_CLASS
    ) -> main.Demo.INNER_CLASS {
        return main.Demo.INNER_CLASS.__createClassWrapper(externalRCRef: Demo_combine_inner_classses__TypesOfArguments__main_Class_without_package_INNER_CLASS_ExportedKotlinPackages_namespace_deeper_Class_with_package_INNER_CLASS_main_Object_without_package_INNER_CLASS_ExportedKotlinPackages_namespace_deeper_Object_with_package_INNER_CLASS__(self.__externalRCRef(), arg1.__externalRCRef(), arg2.__externalRCRef(), arg3.__externalRCRef(), arg4.__externalRCRef()))
    }
    public func combine_inner_objects(
        arg1: main.Class_without_package.INNER_OBJECT,
        arg2: ExportedKotlinPackages.namespace.deeper.Class_with_package.INNER_OBJECT,
        arg3: main.Object_without_package.INNER_OBJECT,
        arg4: ExportedKotlinPackages.namespace.deeper.Object_with_package.INNER_OBJECT
    ) -> main.Demo.INNER_OBJECT {
        return main.Demo.INNER_OBJECT.__createClassWrapper(externalRCRef: Demo_combine_inner_objects__TypesOfArguments__main_Class_without_package_INNER_OBJECT_ExportedKotlinPackages_namespace_deeper_Class_with_package_INNER_OBJECT_main_Object_without_package_INNER_OBJECT_ExportedKotlinPackages_namespace_deeper_Object_with_package_INNER_OBJECT__(self.__externalRCRef(), arg1.__externalRCRef(), arg2.__externalRCRef(), arg3.__externalRCRef(), arg4.__externalRCRef()))
    }
}
open class OPEN_CLASS: KotlinRuntime.KotlinBase {
    public init() {
        if Self.self != main.OPEN_CLASS.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.OPEN_CLASS ") }
        let __kt = __root___OPEN_CLASS_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___OPEN_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public final class Object_without_package: KotlinRuntime.KotlinBase {
    public final class INNER_CLASS: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != main.Object_without_package.INNER_CLASS.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Object_without_package.INNER_CLASS ") }
            let __kt = Object_without_package_INNER_CLASS_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            Object_without_package_INNER_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class INNER_OBJECT: KotlinRuntime.KotlinBase {
        public static var shared: main.Object_without_package.INNER_OBJECT {
            get {
                return main.Object_without_package.INNER_OBJECT.__createClassWrapper(externalRCRef: Object_without_package_INNER_OBJECT_get())
            }
        }
        private init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public static var shared: main.Object_without_package {
        get {
            return main.Object_without_package.__createClassWrapper(externalRCRef: __root___Object_without_package_get())
        }
    }
    private init() {
        fatalError()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
public var nullablePrim: Swift.Int32? {
    get {
        return __root___nullablePrim_get().map { it in it.int32Value }
    }
    set {
        return __root___nullablePrim_set__TypesOfArguments__Swift_Optional_Swift_Int32___(newValue.map { it in NSNumber(value: it) } ?? nil)
    }
}
public var nullableRef: main.Class_without_package? {
    get {
        return { switch __root___nullableRef_get() { case nil: .none; case let res: main.Class_without_package.__createClassWrapper(externalRCRef: res); } }()
    }
    set {
        return __root___nullableRef_set__TypesOfArguments__Swift_Optional_main_Class_without_package___(newValue.map { it in it.__externalRCRef() } ?? nil)
    }
}
public var val_class: main.Class_without_package {
    get {
        return main.Class_without_package.__createClassWrapper(externalRCRef: __root___val_class_get())
    }
}
public var val_class_wp: ExportedKotlinPackages.namespace.deeper.Class_with_package {
    get {
        return ExportedKotlinPackages.namespace.deeper.Class_with_package.__createClassWrapper(externalRCRef: __root___val_class_wp_get())
    }
}
public var val_object: main.Object_without_package {
    get {
        return main.Object_without_package.__createClassWrapper(externalRCRef: __root___val_object_get())
    }
}
public var val_object_wp: ExportedKotlinPackages.namespace.deeper.Object_with_package {
    get {
        return ExportedKotlinPackages.namespace.deeper.Object_with_package.__createClassWrapper(externalRCRef: __root___val_object_wp_get())
    }
}
public var var_class: main.Class_without_package {
    get {
        return main.Class_without_package.__createClassWrapper(externalRCRef: __root___var_class_get())
    }
    set {
        return __root___var_class_set__TypesOfArguments__main_Class_without_package__(newValue.__externalRCRef())
    }
}
public var var_class_wp: ExportedKotlinPackages.namespace.deeper.Class_with_package {
    get {
        return ExportedKotlinPackages.namespace.deeper.Class_with_package.__createClassWrapper(externalRCRef: __root___var_class_wp_get())
    }
    set {
        return __root___var_class_wp_set__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Class_with_package__(newValue.__externalRCRef())
    }
}
public var var_object: main.Object_without_package {
    get {
        return main.Object_without_package.__createClassWrapper(externalRCRef: __root___var_object_get())
    }
    set {
        return __root___var_object_set__TypesOfArguments__main_Object_without_package__(newValue.__externalRCRef())
    }
}
public var var_object_wp: ExportedKotlinPackages.namespace.deeper.Object_with_package {
    get {
        return ExportedKotlinPackages.namespace.deeper.Object_with_package.__createClassWrapper(externalRCRef: __root___var_object_wp_get())
    }
    set {
        return __root___var_object_wp_set__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Object_with_package__(newValue.__externalRCRef())
    }
}
public func combine(
    arg1: main.Class_without_package,
    arg2: ExportedKotlinPackages.namespace.deeper.Class_with_package,
    arg3: main.Object_without_package,
    arg4: ExportedKotlinPackages.namespace.deeper.Object_with_package
) -> Swift.Void {
    return __root___combine__TypesOfArguments__main_Class_without_package_ExportedKotlinPackages_namespace_deeper_Class_with_package_main_Object_without_package_ExportedKotlinPackages_namespace_deeper_Object_with_package__(arg1.__externalRCRef(), arg2.__externalRCRef(), arg3.__externalRCRef(), arg4.__externalRCRef())
}
public func extensionOnNullabeRef(
    _ receiver: main.Class_without_package?
) -> Swift.Void {
    return __root___extensionOnNullabeRef__TypesOfArguments__Swift_Optional_main_Class_without_package___(receiver.map { it in it.__externalRCRef() } ?? nil)
}
public func extensionOnNullablePrimitive(
    _ receiver: Swift.Int32?
) -> Swift.Void {
    return __root___extensionOnNullablePrimitive__TypesOfArguments__Swift_Optional_Swift_Int32___(receiver.map { it in NSNumber(value: it) } ?? nil)
}
public func getExtensionVarOnNullablePrimitive(
    _ receiver: Swift.Int32?
) -> Swift.String {
    return __root___extensionVarOnNullablePrimitive_get__TypesOfArguments__Swift_Optional_Swift_Int32___(receiver.map { it in NSNumber(value: it) } ?? nil)
}
public func getExtensionVarOnNullableRef(
    _ receiver: main.Class_without_package?
) -> Swift.String {
    return __root___extensionVarOnNullableRef_get__TypesOfArguments__Swift_Optional_main_Class_without_package___(receiver.map { it in it.__externalRCRef() } ?? nil)
}
public func nullable_input_prim(
    i: Swift.Int32?
) -> Swift.Void {
    return __root___nullable_input_prim__TypesOfArguments__Swift_Optional_Swift_Int32___(i.map { it in NSNumber(value: it) } ?? nil)
}
public func nullable_input_ref(
    i: main.Class_without_package?
) -> Swift.Void {
    return __root___nullable_input_ref__TypesOfArguments__Swift_Optional_main_Class_without_package___(i.map { it in it.__externalRCRef() } ?? nil)
}
public func nullable_output_prim() -> Swift.Int32? {
    return __root___nullable_output_prim().map { it in it.int32Value }
}
public func nullable_output_ref() -> main.Class_without_package? {
    return { switch __root___nullable_output_ref() { case nil: .none; case let res: main.Class_without_package.__createClassWrapper(externalRCRef: res); } }()
}
public func produce_ABSTRACT_CLASS() -> main.ABSTRACT_CLASS {
    return main.ABSTRACT_CLASS.__createClassWrapper(externalRCRef: __root___produce_ABSTRACT_CLASS())
}
public func produce_DATA_CLASS() -> main.DATA_CLASS {
    return main.DATA_CLASS.__createClassWrapper(externalRCRef: __root___produce_DATA_CLASS())
}
public func produce_DATA_OBJECT() -> ExportedKotlinPackages.namespace.deeper.DATA_OBJECT {
    return ExportedKotlinPackages.namespace.deeper.DATA_OBJECT.__createClassWrapper(externalRCRef: __root___produce_DATA_OBJECT())
}
public func produce_INTERFACE() -> any main.INTERFACE {
    return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___produce_INTERFACE()) as! any main.INTERFACE
}
public func produce_OPEN_CLASS() -> main.OPEN_CLASS {
    return main.OPEN_CLASS.__createClassWrapper(externalRCRef: __root___produce_OPEN_CLASS())
}
public func produce_class() -> main.Class_without_package {
    return main.Class_without_package.__createClassWrapper(externalRCRef: __root___produce_class())
}
public func produce_class_wp() -> ExportedKotlinPackages.namespace.deeper.Class_with_package {
    return ExportedKotlinPackages.namespace.deeper.Class_with_package.__createClassWrapper(externalRCRef: __root___produce_class_wp())
}
public func produce_object() -> main.Object_without_package {
    return main.Object_without_package.__createClassWrapper(externalRCRef: __root___produce_object())
}
public func produce_object_wp() -> ExportedKotlinPackages.namespace.deeper.Object_with_package {
    return ExportedKotlinPackages.namespace.deeper.Object_with_package.__createClassWrapper(externalRCRef: __root___produce_object_wp())
}
public func receive_ABSTRACT_CLASS(
    x: main.ABSTRACT_CLASS
) -> Swift.Void {
    return __root___receive_ABSTRACT_CLASS__TypesOfArguments__main_ABSTRACT_CLASS__(x.__externalRCRef())
}
public func receive_DATA_CLASS(
    x: main.DATA_CLASS
) -> Swift.Void {
    return __root___receive_DATA_CLASS__TypesOfArguments__main_DATA_CLASS__(x.__externalRCRef())
}
public func receive_INTERFACE(
    x: any main.INTERFACE
) -> Swift.Void {
    return __root___receive_INTERFACE__TypesOfArguments__anyU20main_INTERFACE__(x.__externalRCRef())
}
public func recieve_DATA_OBJECT(
    x: ExportedKotlinPackages.namespace.deeper.DATA_OBJECT
) -> Swift.Void {
    return __root___recieve_DATA_OBJECT__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_DATA_OBJECT__(x.__externalRCRef())
}
public func recieve_OPEN_CLASS(
    x: main.OPEN_CLASS
) -> Swift.Void {
    return __root___recieve_OPEN_CLASS__TypesOfArguments__main_OPEN_CLASS__(x.__externalRCRef())
}
public func recieve_class(
    arg: main.Class_without_package
) -> Swift.Void {
    return __root___recieve_class__TypesOfArguments__main_Class_without_package__(arg.__externalRCRef())
}
public func recieve_class_wp(
    arg: ExportedKotlinPackages.namespace.deeper.Class_with_package
) -> Swift.Void {
    return __root___recieve_class_wp__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Class_with_package__(arg.__externalRCRef())
}
public func recieve_object(
    arg: main.Object_without_package
) -> Swift.Void {
    return __root___recieve_object__TypesOfArguments__main_Object_without_package__(arg.__externalRCRef())
}
public func recieve_object_wp(
    arg: ExportedKotlinPackages.namespace.deeper.Object_with_package
) -> Swift.Void {
    return __root___recieve_object_wp__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Object_with_package__(arg.__externalRCRef())
}
public func setExtensionVarOnNullablePrimitive(
    _ receiver: Swift.Int32?,
    v: Swift.String
) -> Swift.Void {
    return __root___extensionVarOnNullablePrimitive_set__TypesOfArguments__Swift_Optional_Swift_Int32__Swift_String__(receiver.map { it in NSNumber(value: it) } ?? nil, v)
}
public func setExtensionVarOnNullableRef(
    _ receiver: main.Class_without_package?,
    v: Swift.String
) -> Swift.Void {
    return __root___extensionVarOnNullableRef_set__TypesOfArguments__Swift_Optional_main_Class_without_package__Swift_String__(receiver.map { it in it.__externalRCRef() } ?? nil, v)
}
extension main.INTERFACE where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension KotlinRuntimeSupport._KotlinExistential: main.INTERFACE where Wrapped : main._INTERFACE {
}
extension ExportedKotlinPackages.namespace.deeper {
    public final class Class_with_package: KotlinRuntime.KotlinBase {
        public final class INNER_CLASS: KotlinRuntime.KotlinBase {
            public init() {
                if Self.self != ExportedKotlinPackages.namespace.deeper.Class_with_package.INNER_CLASS.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.namespace.deeper.Class_with_package.INNER_CLASS ") }
                let __kt = namespace_deeper_Class_with_package_INNER_CLASS_init_allocate()
                super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
                namespace_deeper_Class_with_package_INNER_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
        }
        public final class INNER_OBJECT: KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.namespace.deeper.Class_with_package.INNER_OBJECT {
                get {
                    return ExportedKotlinPackages.namespace.deeper.Class_with_package.INNER_OBJECT.__createClassWrapper(externalRCRef: namespace_deeper_Class_with_package_INNER_OBJECT_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
        }
        public init() {
            if Self.self != ExportedKotlinPackages.namespace.deeper.Class_with_package.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.namespace.deeper.Class_with_package ") }
            let __kt = namespace_deeper_Class_with_package_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            namespace_deeper_Class_with_package_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class DATA_OBJECT: KotlinRuntime.KotlinBase {
        public var a: Swift.Int32 {
            get {
                return namespace_deeper_DATA_OBJECT_a_get(self.__externalRCRef())
            }
        }
        public static var shared: ExportedKotlinPackages.namespace.deeper.DATA_OBJECT {
            get {
                return ExportedKotlinPackages.namespace.deeper.DATA_OBJECT.__createClassWrapper(externalRCRef: namespace_deeper_DATA_OBJECT_get())
            }
        }
        private init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public static func ==(
            this: ExportedKotlinPackages.namespace.deeper.DATA_OBJECT,
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public func equals(
            other: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool {
            return namespace_deeper_DATA_OBJECT_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public func hashCode() -> Swift.Int32 {
            return namespace_deeper_DATA_OBJECT_hashCode(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return namespace_deeper_DATA_OBJECT_toString(self.__externalRCRef())
        }
    }
    public final class Object_with_package: KotlinRuntime.KotlinBase {
        public final class INNER_CLASS: KotlinRuntime.KotlinBase {
            public init() {
                if Self.self != ExportedKotlinPackages.namespace.deeper.Object_with_package.INNER_CLASS.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.namespace.deeper.Object_with_package.INNER_CLASS ") }
                let __kt = namespace_deeper_Object_with_package_INNER_CLASS_init_allocate()
                super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
                namespace_deeper_Object_with_package_INNER_CLASS_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
        }
        public final class INNER_OBJECT: KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.namespace.deeper.Object_with_package.INNER_OBJECT {
                get {
                    return ExportedKotlinPackages.namespace.deeper.Object_with_package.INNER_OBJECT.__createClassWrapper(externalRCRef: namespace_deeper_Object_with_package_INNER_OBJECT_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
        }
        public static var shared: ExportedKotlinPackages.namespace.deeper.Object_with_package {
            get {
                return ExportedKotlinPackages.namespace.deeper.Object_with_package.__createClassWrapper(externalRCRef: namespace_deeper_Object_with_package_get())
            }
        }
        private init() {
            fatalError()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
}
extension ExportedKotlinPackages.ignored {
    public enum ENUM {
        case A
        public static var allCases: [ExportedKotlinPackages.ignored.ENUM] {
            get {
                return ignored_ENUM_entries_get() as! Swift.Array<ExportedKotlinPackages.ignored.ENUM>
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
        ) -> ExportedKotlinPackages.ignored.ENUM {
            return ExportedKotlinPackages.ignored.ENUM.__createClassWrapper(externalRCRef: ignored_ENUM_valueOf__TypesOfArguments__Swift_String__(value))
        }
    }
    public static func produce_ENUM() -> ExportedKotlinPackages.ignored.ENUM {
        return ExportedKotlinPackages.ignored.ENUM.__createClassWrapper(externalRCRef: ignored_produce_ENUM())
    }
    public static func produce_VALUE_CLASS() -> Swift.Never {
        fatalError()
    }
    public static func receive_ENUM(
        x: ExportedKotlinPackages.ignored.ENUM
    ) -> Swift.Void {
        return ignored_receive_ENUM__TypesOfArguments__ExportedKotlinPackages_ignored_ENUM__(x.__externalRCRef())
    }
    public static func receive_VALUE_CLASS(
        x: Swift.Never
    ) -> Swift.Void {
        fatalError()
    }
}

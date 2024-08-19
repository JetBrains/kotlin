@_exported import ExportedKotlinPackages
import KotlinRuntime
@_implementationOnly import KotlinBridges_main

public final class Class_without_package : KotlinRuntime.KotlinBase {
    public final class INNER_CLASS : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = Class_without_package_INNER_CLASS_init_allocate()
            super.init(__externalRCRef: __kt)
            Class_without_package_INNER_CLASS_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public final class INNER_OBJECT : KotlinRuntime.KotlinBase {
        public static var shared: main.Class_without_package.INNER_OBJECT {
            get {
                return main.Class_without_package.INNER_OBJECT(__externalRCRef: Class_without_package_INNER_OBJECT_get())
            }
        }
        private override init() {
            fatalError()
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public override init() {
        let __kt = __root___Class_without_package_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Class_without_package_init_initialize__TypesOfArguments__uintptr_t__(__kt)
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public final class Demo : KotlinRuntime.KotlinBase {
    public final class INNER_CLASS : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = Demo_INNER_CLASS_init_allocate()
            super.init(__externalRCRef: __kt)
            Demo_INNER_CLASS_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public final class INNER_OBJECT : KotlinRuntime.KotlinBase {
        public static var shared: main.Demo.INNER_OBJECT {
            get {
                return main.Demo.INNER_OBJECT(__externalRCRef: Demo_INNER_OBJECT_get())
            }
        }
        private override init() {
            fatalError()
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public var arg1: main.Class_without_package {
        get {
            return main.Class_without_package(__externalRCRef: Demo_arg1_get(self.__externalRCRef()))
        }
    }
    public var arg2: ExportedKotlinPackages.namespace.deeper.Class_with_package {
        get {
            return ExportedKotlinPackages.namespace.deeper.Class_with_package(__externalRCRef: Demo_arg2_get(self.__externalRCRef()))
        }
    }
    public var arg3: main.Object_without_package {
        get {
            return main.Object_without_package(__externalRCRef: Demo_arg3_get(self.__externalRCRef()))
        }
    }
    public var arg4: ExportedKotlinPackages.namespace.deeper.Object_with_package {
        get {
            return ExportedKotlinPackages.namespace.deeper.Object_with_package(__externalRCRef: Demo_arg4_get(self.__externalRCRef()))
        }
    }
    public var var1: main.Class_without_package {
        get {
            return main.Class_without_package(__externalRCRef: Demo_var1_get(self.__externalRCRef()))
        }
        set {
            return Demo_var1_set__TypesOfArguments__uintptr_t__(self.__externalRCRef(), newValue.__externalRCRef())
        }
    }
    public var var2: ExportedKotlinPackages.namespace.deeper.Class_with_package {
        get {
            return ExportedKotlinPackages.namespace.deeper.Class_with_package(__externalRCRef: Demo_var2_get(self.__externalRCRef()))
        }
        set {
            return Demo_var2_set__TypesOfArguments__uintptr_t__(self.__externalRCRef(), newValue.__externalRCRef())
        }
    }
    public var var3: main.Object_without_package {
        get {
            return main.Object_without_package(__externalRCRef: Demo_var3_get(self.__externalRCRef()))
        }
        set {
            return Demo_var3_set__TypesOfArguments__uintptr_t__(self.__externalRCRef(), newValue.__externalRCRef())
        }
    }
    public var var4: ExportedKotlinPackages.namespace.deeper.Object_with_package {
        get {
            return ExportedKotlinPackages.namespace.deeper.Object_with_package(__externalRCRef: Demo_var4_get(self.__externalRCRef()))
        }
        set {
            return Demo_var4_set__TypesOfArguments__uintptr_t__(self.__externalRCRef(), newValue.__externalRCRef())
        }
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public init(
        arg1: main.Class_without_package,
        arg2: ExportedKotlinPackages.namespace.deeper.Class_with_package,
        arg3: main.Object_without_package,
        arg4: ExportedKotlinPackages.namespace.deeper.Object_with_package
    ) {
        let __kt = __root___Demo_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Demo_init_initialize__TypesOfArguments__uintptr_t_uintptr_t_uintptr_t_uintptr_t_uintptr_t__(__kt, arg1.__externalRCRef(), arg2.__externalRCRef(), arg3.__externalRCRef(), arg4.__externalRCRef())
    }
    public func combine(
        arg1: main.Class_without_package,
        arg2: ExportedKotlinPackages.namespace.deeper.Class_with_package,
        arg3: main.Object_without_package,
        arg4: ExportedKotlinPackages.namespace.deeper.Object_with_package
    ) -> main.Demo {
        return main.Demo(__externalRCRef: Demo_combine__TypesOfArguments__uintptr_t_uintptr_t_uintptr_t_uintptr_t__(self.__externalRCRef(), arg1.__externalRCRef(), arg2.__externalRCRef(), arg3.__externalRCRef(), arg4.__externalRCRef()))
    }
    public func combine_inner_classses(
        arg1: main.Class_without_package.INNER_CLASS,
        arg2: ExportedKotlinPackages.namespace.deeper.Class_with_package.INNER_CLASS,
        arg3: main.Object_without_package.INNER_CLASS,
        arg4: ExportedKotlinPackages.namespace.deeper.Object_with_package.INNER_CLASS
    ) -> main.Demo.INNER_CLASS {
        return main.Demo.INNER_CLASS(__externalRCRef: Demo_combine_inner_classses__TypesOfArguments__uintptr_t_uintptr_t_uintptr_t_uintptr_t__(self.__externalRCRef(), arg1.__externalRCRef(), arg2.__externalRCRef(), arg3.__externalRCRef(), arg4.__externalRCRef()))
    }
    public func combine_inner_objects(
        arg1: main.Class_without_package.INNER_OBJECT,
        arg2: ExportedKotlinPackages.namespace.deeper.Class_with_package.INNER_OBJECT,
        arg3: main.Object_without_package.INNER_OBJECT,
        arg4: ExportedKotlinPackages.namespace.deeper.Object_with_package.INNER_OBJECT
    ) -> main.Demo.INNER_OBJECT {
        return main.Demo.INNER_OBJECT(__externalRCRef: Demo_combine_inner_objects__TypesOfArguments__uintptr_t_uintptr_t_uintptr_t_uintptr_t__(self.__externalRCRef(), arg1.__externalRCRef(), arg2.__externalRCRef(), arg3.__externalRCRef(), arg4.__externalRCRef()))
    }
}
open class OPEN_CLASS : KotlinRuntime.KotlinBase {
    public override init() {
        let __kt = __root___OPEN_CLASS_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___OPEN_CLASS_init_initialize__TypesOfArguments__uintptr_t__(__kt)
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public final class Object_without_package : KotlinRuntime.KotlinBase {
    public final class INNER_CLASS : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = Object_without_package_INNER_CLASS_init_allocate()
            super.init(__externalRCRef: __kt)
            Object_without_package_INNER_CLASS_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public final class INNER_OBJECT : KotlinRuntime.KotlinBase {
        public static var shared: main.Object_without_package.INNER_OBJECT {
            get {
                return main.Object_without_package.INNER_OBJECT(__externalRCRef: Object_without_package_INNER_OBJECT_get())
            }
        }
        private override init() {
            fatalError()
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public static var shared: main.Object_without_package {
        get {
            return main.Object_without_package(__externalRCRef: __root___Object_without_package_get())
        }
    }
    private override init() {
        fatalError()
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public var val_class: main.Class_without_package {
    get {
        return main.Class_without_package(__externalRCRef: __root___val_class_get())
    }
}
public var val_class_wp: ExportedKotlinPackages.namespace.deeper.Class_with_package {
    get {
        return ExportedKotlinPackages.namespace.deeper.Class_with_package(__externalRCRef: __root___val_class_wp_get())
    }
}
public var val_object: main.Object_without_package {
    get {
        return main.Object_without_package(__externalRCRef: __root___val_object_get())
    }
}
public var val_object_wp: ExportedKotlinPackages.namespace.deeper.Object_with_package {
    get {
        return ExportedKotlinPackages.namespace.deeper.Object_with_package(__externalRCRef: __root___val_object_wp_get())
    }
}
public var var_class: main.Class_without_package {
    get {
        return main.Class_without_package(__externalRCRef: __root___var_class_get())
    }
    set {
        return __root___var_class_set__TypesOfArguments__uintptr_t__(newValue.__externalRCRef())
    }
}
public var var_class_wp: ExportedKotlinPackages.namespace.deeper.Class_with_package {
    get {
        return ExportedKotlinPackages.namespace.deeper.Class_with_package(__externalRCRef: __root___var_class_wp_get())
    }
    set {
        return __root___var_class_wp_set__TypesOfArguments__uintptr_t__(newValue.__externalRCRef())
    }
}
public var var_object: main.Object_without_package {
    get {
        return main.Object_without_package(__externalRCRef: __root___var_object_get())
    }
    set {
        return __root___var_object_set__TypesOfArguments__uintptr_t__(newValue.__externalRCRef())
    }
}
public var var_object_wp: ExportedKotlinPackages.namespace.deeper.Object_with_package {
    get {
        return ExportedKotlinPackages.namespace.deeper.Object_with_package(__externalRCRef: __root___var_object_wp_get())
    }
    set {
        return __root___var_object_wp_set__TypesOfArguments__uintptr_t__(newValue.__externalRCRef())
    }
}
@_cdecl("SwiftExport_ExportedKotlinPackages_namespace_deeper_Class_with_package_toRetainedSwift")
private func SwiftExport_ExportedKotlinPackages_namespace_deeper_Class_with_package_toRetainedSwift(
    externalRCRef: Swift.UInt
) -> Swift.UnsafeMutableRawPointer {
    return Unmanaged.passRetained(ExportedKotlinPackages.namespace.deeper.Class_with_package(__externalRCRef: externalRCRef)).toOpaque()
}
@_cdecl("SwiftExport_ExportedKotlinPackages_namespace_deeper_DATA_OBJECT_toRetainedSwift")
private func SwiftExport_ExportedKotlinPackages_namespace_deeper_DATA_OBJECT_toRetainedSwift(
    externalRCRef: Swift.UInt
) -> Swift.UnsafeMutableRawPointer {
    return Unmanaged.passRetained(ExportedKotlinPackages.namespace.deeper.DATA_OBJECT(__externalRCRef: externalRCRef)).toOpaque()
}
@_cdecl("SwiftExport_ExportedKotlinPackages_namespace_deeper_Object_with_package_toRetainedSwift")
private func SwiftExport_ExportedKotlinPackages_namespace_deeper_Object_with_package_toRetainedSwift(
    externalRCRef: Swift.UInt
) -> Swift.UnsafeMutableRawPointer {
    return Unmanaged.passRetained(ExportedKotlinPackages.namespace.deeper.Object_with_package(__externalRCRef: externalRCRef)).toOpaque()
}
@_cdecl("SwiftExport_main_Class_without_package_toRetainedSwift")
private func SwiftExport_main_Class_without_package_toRetainedSwift(
    externalRCRef: Swift.UInt
) -> Swift.UnsafeMutableRawPointer {
    return Unmanaged.passRetained(main.Class_without_package(__externalRCRef: externalRCRef)).toOpaque()
}
@_cdecl("SwiftExport_main_Demo_toRetainedSwift")
private func SwiftExport_main_Demo_toRetainedSwift(
    externalRCRef: Swift.UInt
) -> Swift.UnsafeMutableRawPointer {
    return Unmanaged.passRetained(main.Demo(__externalRCRef: externalRCRef)).toOpaque()
}
@_cdecl("SwiftExport_main_OPEN_CLASS_toRetainedSwift")
private func SwiftExport_main_OPEN_CLASS_toRetainedSwift(
    externalRCRef: Swift.UInt
) -> Swift.UnsafeMutableRawPointer {
    return Unmanaged.passRetained(main.OPEN_CLASS(__externalRCRef: externalRCRef)).toOpaque()
}
@_cdecl("SwiftExport_main_Object_without_package_toRetainedSwift")
private func SwiftExport_main_Object_without_package_toRetainedSwift(
    externalRCRef: Swift.UInt
) -> Swift.UnsafeMutableRawPointer {
    return Unmanaged.passRetained(main.Object_without_package(__externalRCRef: externalRCRef)).toOpaque()
}
public func combine(
    arg1: main.Class_without_package,
    arg2: ExportedKotlinPackages.namespace.deeper.Class_with_package,
    arg3: main.Object_without_package,
    arg4: ExportedKotlinPackages.namespace.deeper.Object_with_package
) -> Swift.Void {
    return __root___combine__TypesOfArguments__uintptr_t_uintptr_t_uintptr_t_uintptr_t__(arg1.__externalRCRef(), arg2.__externalRCRef(), arg3.__externalRCRef(), arg4.__externalRCRef())
}
public func produce_DATA_OBJECT() -> ExportedKotlinPackages.namespace.deeper.DATA_OBJECT {
    return ExportedKotlinPackages.namespace.deeper.DATA_OBJECT(__externalRCRef: __root___produce_DATA_OBJECT())
}
public func produce_OPEN_CLASS() -> main.OPEN_CLASS {
    return main.OPEN_CLASS(__externalRCRef: __root___produce_OPEN_CLASS())
}
public func produce_class() -> main.Class_without_package {
    return main.Class_without_package(__externalRCRef: __root___produce_class())
}
public func produce_class_wp() -> ExportedKotlinPackages.namespace.deeper.Class_with_package {
    return ExportedKotlinPackages.namespace.deeper.Class_with_package(__externalRCRef: __root___produce_class_wp())
}
public func produce_object() -> main.Object_without_package {
    return main.Object_without_package(__externalRCRef: __root___produce_object())
}
public func produce_object_wp() -> ExportedKotlinPackages.namespace.deeper.Object_with_package {
    return ExportedKotlinPackages.namespace.deeper.Object_with_package(__externalRCRef: __root___produce_object_wp())
}
public func recieve_DATA_OBJECT(
    x: ExportedKotlinPackages.namespace.deeper.DATA_OBJECT
) -> Swift.Void {
    return __root___recieve_DATA_OBJECT__TypesOfArguments__uintptr_t__(x.__externalRCRef())
}
public func recieve_OPEN_CLASS(
    x: main.OPEN_CLASS
) -> Swift.Void {
    return __root___recieve_OPEN_CLASS__TypesOfArguments__uintptr_t__(x.__externalRCRef())
}
public func recieve_class(
    arg: main.Class_without_package
) -> Swift.Void {
    return __root___recieve_class__TypesOfArguments__uintptr_t__(arg.__externalRCRef())
}
public func recieve_class_wp(
    arg: ExportedKotlinPackages.namespace.deeper.Class_with_package
) -> Swift.Void {
    return __root___recieve_class_wp__TypesOfArguments__uintptr_t__(arg.__externalRCRef())
}
public func recieve_object(
    arg: main.Object_without_package
) -> Swift.Void {
    return __root___recieve_object__TypesOfArguments__uintptr_t__(arg.__externalRCRef())
}
public func recieve_object_wp(
    arg: ExportedKotlinPackages.namespace.deeper.Object_with_package
) -> Swift.Void {
    return __root___recieve_object_wp__TypesOfArguments__uintptr_t__(arg.__externalRCRef())
}
public extension ExportedKotlinPackages.namespace.deeper {
    public final class Class_with_package : KotlinRuntime.KotlinBase {
        public final class INNER_CLASS : KotlinRuntime.KotlinBase {
            public override init() {
                let __kt = namespace_deeper_Class_with_package_INNER_CLASS_init_allocate()
                super.init(__externalRCRef: __kt)
                namespace_deeper_Class_with_package_INNER_CLASS_init_initialize__TypesOfArguments__uintptr_t__(__kt)
            }
            public override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
        }
        public final class INNER_OBJECT : KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.namespace.deeper.Class_with_package.INNER_OBJECT {
                get {
                    return ExportedKotlinPackages.namespace.deeper.Class_with_package.INNER_OBJECT(__externalRCRef: namespace_deeper_Class_with_package_INNER_OBJECT_get())
                }
            }
            private override init() {
                fatalError()
            }
            public override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
        }
        public override init() {
            let __kt = namespace_deeper_Class_with_package_init_allocate()
            super.init(__externalRCRef: __kt)
            namespace_deeper_Class_with_package_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public final class DATA_OBJECT : KotlinRuntime.KotlinBase {
        public var a: Swift.Int32 {
            get {
                return namespace_deeper_DATA_OBJECT_a_get(self.__externalRCRef())
            }
        }
        public static var shared: ExportedKotlinPackages.namespace.deeper.DATA_OBJECT {
            get {
                return ExportedKotlinPackages.namespace.deeper.DATA_OBJECT(__externalRCRef: namespace_deeper_DATA_OBJECT_get())
            }
        }
        private override init() {
            fatalError()
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public func hashCode() -> Swift.Int32 {
            return namespace_deeper_DATA_OBJECT_hashCode(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return namespace_deeper_DATA_OBJECT_toString(self.__externalRCRef())
        }
    }
    public final class Object_with_package : KotlinRuntime.KotlinBase {
        public final class INNER_CLASS : KotlinRuntime.KotlinBase {
            public override init() {
                let __kt = namespace_deeper_Object_with_package_INNER_CLASS_init_allocate()
                super.init(__externalRCRef: __kt)
                namespace_deeper_Object_with_package_INNER_CLASS_init_initialize__TypesOfArguments__uintptr_t__(__kt)
            }
            public override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
        }
        public final class INNER_OBJECT : KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.namespace.deeper.Object_with_package.INNER_OBJECT {
                get {
                    return ExportedKotlinPackages.namespace.deeper.Object_with_package.INNER_OBJECT(__externalRCRef: namespace_deeper_Object_with_package_INNER_OBJECT_get())
                }
            }
            private override init() {
                fatalError()
            }
            public override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
        }
        public static var shared: ExportedKotlinPackages.namespace.deeper.Object_with_package {
            get {
                return ExportedKotlinPackages.namespace.deeper.Object_with_package(__externalRCRef: namespace_deeper_Object_with_package_get())
            }
        }
        private override init() {
            fatalError()
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
}
public extension ExportedKotlinPackages.ignored {
    public static func produce_ABSTRACT_CLASS() -> Swift.Never {
        fatalError()
    }
    public static func produce_DATA_CLASS() -> Swift.Never {
        fatalError()
    }
    public static func produce_ENUM() -> Swift.Never {
        fatalError()
    }
    public static func produce_INTERFACE() -> Swift.Never {
        fatalError()
    }
    public static func produce_NULLABlE() -> Swift.Never {
        fatalError()
    }
    public static func produce_VALUE_CLASS() -> Swift.Never {
        fatalError()
    }
    public static func receive_NULLABLE(
        x: Swift.Never
    ) -> Swift.Void {
        fatalError()
    }
    public static func recieve_ABSTRACT_CLASS(
        x: Swift.Never
    ) -> Swift.Void {
        fatalError()
    }
    public static func recieve_DATA_CLASS(
        x: Swift.Never
    ) -> Swift.Void {
        fatalError()
    }
    public static func recieve_ENUM(
        x: Swift.Never
    ) -> Swift.Void {
        fatalError()
    }
    public static func recieve_INTERFACE(
        x: Swift.Never
    ) -> Swift.Void {
        fatalError()
    }
    public static func recieve_VALUE_CLASS(
        x: Swift.Never
    ) -> Swift.Void {
        fatalError()
    }
}

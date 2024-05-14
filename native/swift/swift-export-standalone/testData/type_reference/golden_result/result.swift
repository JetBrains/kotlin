import KotlinRuntime
import KotlinBridges

public class Class_without_package : KotlinRuntime.KotlinBase {
    public class INNER_CLASS : KotlinRuntime.KotlinBase {
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
    public class INNER_OBJECT : KotlinRuntime.KotlinBase {
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
public class Demo : KotlinRuntime.KotlinBase {
    public class INNER_CLASS : KotlinRuntime.KotlinBase {
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
    public class INNER_OBJECT : KotlinRuntime.KotlinBase {
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
    public var arg2: main.namespace.deeper.Class_with_package {
        get {
            return main.namespace.deeper.Class_with_package(__externalRCRef: Demo_arg2_get(self.__externalRCRef()))
        }
    }
    public var arg3: main.Object_without_package {
        get {
            return main.Object_without_package(__externalRCRef: Demo_arg3_get(self.__externalRCRef()))
        }
    }
    public var arg4: main.namespace.deeper.Object_with_package {
        get {
            return main.namespace.deeper.Object_with_package(__externalRCRef: Demo_arg4_get(self.__externalRCRef()))
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
    public var var2: main.namespace.deeper.Class_with_package {
        get {
            return main.namespace.deeper.Class_with_package(__externalRCRef: Demo_var2_get(self.__externalRCRef()))
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
    public var var4: main.namespace.deeper.Object_with_package {
        get {
            return main.namespace.deeper.Object_with_package(__externalRCRef: Demo_var4_get(self.__externalRCRef()))
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
        arg2: main.namespace.deeper.Class_with_package,
        arg3: main.Object_without_package,
        arg4: main.namespace.deeper.Object_with_package
    ) {
        let __kt = __root___Demo_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Demo_init_initialize__TypesOfArguments__uintptr_t_uintptr_t_uintptr_t_uintptr_t_uintptr_t__(__kt, arg1.__externalRCRef(), arg2.__externalRCRef(), arg3.__externalRCRef(), arg4.__externalRCRef())
    }
    public func combine(
        arg1: main.Class_without_package,
        arg2: main.namespace.deeper.Class_with_package,
        arg3: main.Object_without_package,
        arg4: main.namespace.deeper.Object_with_package
    ) -> main.Demo {
        return main.Demo(__externalRCRef: Demo_combine__TypesOfArguments__uintptr_t_uintptr_t_uintptr_t_uintptr_t__(self.__externalRCRef(), arg1.__externalRCRef(), arg2.__externalRCRef(), arg3.__externalRCRef(), arg4.__externalRCRef()))
    }
    public func combine_inner_classses(
        arg1: main.Class_without_package.INNER_CLASS,
        arg2: main.namespace.deeper.Class_with_package.INNER_CLASS,
        arg3: main.Object_without_package.INNER_CLASS,
        arg4: main.namespace.deeper.Object_with_package.INNER_CLASS
    ) -> main.Demo.INNER_CLASS {
        return main.Demo.INNER_CLASS(__externalRCRef: Demo_combine_inner_classses__TypesOfArguments__uintptr_t_uintptr_t_uintptr_t_uintptr_t__(self.__externalRCRef(), arg1.__externalRCRef(), arg2.__externalRCRef(), arg3.__externalRCRef(), arg4.__externalRCRef()))
    }
    public func combine_inner_objects(
        arg1: main.Class_without_package.INNER_OBJECT,
        arg2: main.namespace.deeper.Class_with_package.INNER_OBJECT,
        arg3: main.Object_without_package.INNER_OBJECT,
        arg4: main.namespace.deeper.Object_with_package.INNER_OBJECT
    ) -> main.Demo.INNER_OBJECT {
        return main.Demo.INNER_OBJECT(__externalRCRef: Demo_combine_inner_objects__TypesOfArguments__uintptr_t_uintptr_t_uintptr_t_uintptr_t__(self.__externalRCRef(), arg1.__externalRCRef(), arg2.__externalRCRef(), arg3.__externalRCRef(), arg4.__externalRCRef()))
    }
}
public class Object_without_package : KotlinRuntime.KotlinBase {
    public class INNER_CLASS : KotlinRuntime.KotlinBase {
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
    public class INNER_OBJECT : KotlinRuntime.KotlinBase {
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
public var val_class_wp: main.namespace.deeper.Class_with_package {
    get {
        return main.namespace.deeper.Class_with_package(__externalRCRef: __root___val_class_wp_get())
    }
}
public var val_object: main.Object_without_package {
    get {
        return main.Object_without_package(__externalRCRef: __root___val_object_get())
    }
}
public var val_object_wp: main.namespace.deeper.Object_with_package {
    get {
        return main.namespace.deeper.Object_with_package(__externalRCRef: __root___val_object_wp_get())
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
public var var_class_wp: main.namespace.deeper.Class_with_package {
    get {
        return main.namespace.deeper.Class_with_package(__externalRCRef: __root___var_class_wp_get())
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
public var var_object_wp: main.namespace.deeper.Object_with_package {
    get {
        return main.namespace.deeper.Object_with_package(__externalRCRef: __root___var_object_wp_get())
    }
    set {
        return __root___var_object_wp_set__TypesOfArguments__uintptr_t__(newValue.__externalRCRef())
    }
}
public func combine(
    arg1: main.Class_without_package,
    arg2: main.namespace.deeper.Class_with_package,
    arg3: main.Object_without_package,
    arg4: main.namespace.deeper.Object_with_package
) -> Swift.Void {
    return __root___combine__TypesOfArguments__uintptr_t_uintptr_t_uintptr_t_uintptr_t__(arg1.__externalRCRef(), arg2.__externalRCRef(), arg3.__externalRCRef(), arg4.__externalRCRef())
}
public func produce_class() -> main.Class_without_package {
    return main.Class_without_package(__externalRCRef: __root___produce_class())
}
public func produce_class_wp() -> main.namespace.deeper.Class_with_package {
    return main.namespace.deeper.Class_with_package(__externalRCRef: __root___produce_class_wp())
}
public func produce_object() -> main.Object_without_package {
    return main.Object_without_package(__externalRCRef: __root___produce_object())
}
public func produce_object_wp() -> main.namespace.deeper.Object_with_package {
    return main.namespace.deeper.Object_with_package(__externalRCRef: __root___produce_object_wp())
}
public func recieve_class(
    arg: main.Class_without_package
) -> Swift.Void {
    return __root___recieve_class__TypesOfArguments__uintptr_t__(arg.__externalRCRef())
}
public func recieve_class_wp(
    arg: main.namespace.deeper.Class_with_package
) -> Swift.Void {
    return __root___recieve_class_wp__TypesOfArguments__uintptr_t__(arg.__externalRCRef())
}
public func recieve_object(
    arg: main.Object_without_package
) -> Swift.Void {
    return __root___recieve_object__TypesOfArguments__uintptr_t__(arg.__externalRCRef())
}
public func recieve_object_wp(
    arg: main.namespace.deeper.Object_with_package
) -> Swift.Void {
    return __root___recieve_object_wp__TypesOfArguments__uintptr_t__(arg.__externalRCRef())
}
public extension main.namespace.deeper {
    public class Class_with_package : KotlinRuntime.KotlinBase {
        public class INNER_CLASS : KotlinRuntime.KotlinBase {
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
        public class INNER_OBJECT : KotlinRuntime.KotlinBase {
            public static var shared: main.namespace.deeper.Class_with_package.INNER_OBJECT {
                get {
                    return main.namespace.deeper.Class_with_package.INNER_OBJECT(__externalRCRef: namespace_deeper_Class_with_package_INNER_OBJECT_get())
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
    public class Object_with_package : KotlinRuntime.KotlinBase {
        public class INNER_CLASS : KotlinRuntime.KotlinBase {
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
        public class INNER_OBJECT : KotlinRuntime.KotlinBase {
            public static var shared: main.namespace.deeper.Object_with_package.INNER_OBJECT {
                get {
                    return main.namespace.deeper.Object_with_package.INNER_OBJECT(__externalRCRef: namespace_deeper_Object_with_package_INNER_OBJECT_get())
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
        public static var shared: main.namespace.deeper.Object_with_package {
            get {
                return main.namespace.deeper.Object_with_package(__externalRCRef: namespace_deeper_Object_with_package_get())
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
public extension main.ignored {
    public static func produce_ABSTRACT_CLASS() -> KotlinRuntime.KotlinBase {
        fatalError()
    }
    public static func produce_DATA_CLASS() -> KotlinRuntime.KotlinBase {
        fatalError()
    }
    public static func produce_DATA_OBJECT() -> KotlinRuntime.KotlinBase {
        fatalError()
    }
    public static func produce_ENUM() -> KotlinRuntime.KotlinBase {
        fatalError()
    }
    public static func produce_INTERFACE() -> KotlinRuntime.KotlinBase {
        fatalError()
    }
    public static func produce_OPEN_CLASS() -> KotlinRuntime.KotlinBase {
        fatalError()
    }
    public static func produce_VALUE_CLASS() -> KotlinRuntime.KotlinBase {
        fatalError()
    }
    public static func recieve_ABSTRACT_CLASS(
        x: KotlinRuntime.KotlinBase
    ) -> Swift.Void {
        fatalError()
    }
    public static func recieve_DATA_CLASS(
        x: KotlinRuntime.KotlinBase
    ) -> Swift.Void {
        fatalError()
    }
    public static func recieve_DATA_OBJECT(
        x: KotlinRuntime.KotlinBase
    ) -> Swift.Void {
        fatalError()
    }
    public static func recieve_ENUM(
        x: KotlinRuntime.KotlinBase
    ) -> Swift.Void {
        fatalError()
    }
    public static func recieve_INTERFACE(
        x: KotlinRuntime.KotlinBase
    ) -> Swift.Void {
        fatalError()
    }
    public static func recieve_OPEN_CLASS(
        x: KotlinRuntime.KotlinBase
    ) -> Swift.Void {
        fatalError()
    }
    public static func recieve_VALUE_CLASS(
        x: KotlinRuntime.KotlinBase
    ) -> Swift.Void {
        fatalError()
    }
}
public enum ignored {
}
public enum namespace {
    public enum deeper {
    }
}

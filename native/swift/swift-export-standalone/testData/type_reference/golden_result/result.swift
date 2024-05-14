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
                fatalError()
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
                fatalError()
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
            fatalError()
        }
    }
    public var arg2: main.namespace.deeper.Class_with_package {
        get {
            fatalError()
        }
    }
    public var arg3: main.Object_without_package {
        get {
            fatalError()
        }
    }
    public var arg4: main.namespace.deeper.Object_with_package {
        get {
            fatalError()
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
        fatalError()
    }
    public func combine(
        arg1: main.Class_without_package,
        arg2: main.namespace.deeper.Class_with_package,
        arg3: main.Object_without_package,
        arg4: main.namespace.deeper.Object_with_package
    ) -> main.Demo {
        fatalError()
    }
    public func combine_inner_classses(
        arg1: main.Class_without_package.INNER_CLASS,
        arg2: main.namespace.deeper.Class_with_package.INNER_CLASS,
        arg3: main.Object_without_package.INNER_CLASS,
        arg4: main.namespace.deeper.Object_with_package.INNER_CLASS
    ) -> main.Demo.INNER_CLASS {
        fatalError()
    }
    public func combine_inner_objects(
        arg1: main.Class_without_package.INNER_OBJECT,
        arg2: main.namespace.deeper.Class_with_package.INNER_OBJECT,
        arg3: main.Object_without_package.INNER_OBJECT,
        arg4: main.namespace.deeper.Object_with_package.INNER_OBJECT
    ) -> main.Demo.INNER_OBJECT {
        fatalError()
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
                fatalError()
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
            fatalError()
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
public func combine(
    arg1: main.Class_without_package,
    arg2: main.namespace.deeper.Class_with_package,
    arg3: main.Object_without_package,
    arg4: main.namespace.deeper.Object_with_package
) -> Swift.Void {
    fatalError()
}
public func produce_class() -> main.Class_without_package {
    fatalError()
}
public func produce_class_wp() -> main.namespace.deeper.Class_with_package {
    fatalError()
}
public func produce_object() -> main.Object_without_package {
    fatalError()
}
public func produce_object_wp() -> main.namespace.deeper.Object_with_package {
    fatalError()
}
public func recieve_class(
    arg: main.Class_without_package
) -> Swift.Void {
    fatalError()
}
public func recieve_class_wp(
    arg: main.namespace.deeper.Class_with_package
) -> Swift.Void {
    fatalError()
}
public func recieve_object(
    arg: main.Object_without_package
) -> Swift.Void {
    fatalError()
}
public func recieve_object_wp(
    arg: main.namespace.deeper.Object_with_package
) -> Swift.Void {
    fatalError()
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
                    fatalError()
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
                    fatalError()
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
                fatalError()
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
public enum namespace {
    public enum deeper {
    }
}

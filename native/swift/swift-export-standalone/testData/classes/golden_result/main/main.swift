@_exported import ExportedKotlinPackages
import KotlinRuntime
@_implementationOnly import KotlinBridges_main

public final class CLASS_WITH_SAME_NAME : KotlinRuntime.KotlinBase {
    public override init() {
        let __kt = __root___CLASS_WITH_SAME_NAME_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___CLASS_WITH_SAME_NAME_init_initialize__TypesOfArguments__uintptr_t__(__kt)
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public func foo() -> Swift.Int32 {
        return CLASS_WITH_SAME_NAME_foo(self.__externalRCRef())
    }
}
public final class ClassWithNonPublicConstructor : KotlinRuntime.KotlinBase {
    public var a: Swift.Int32 {
        get {
            return ClassWithNonPublicConstructor_a_get(self.__externalRCRef())
        }
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public final class Foo : KotlinRuntime.KotlinBase {
    public final class INSIDE_CLASS : KotlinRuntime.KotlinBase {
        public var my_value_inner: Swift.UInt32 {
            get {
                return Foo_INSIDE_CLASS_my_value_inner_get(self.__externalRCRef())
            }
        }
        public var my_variable_inner: Swift.Int64 {
            get {
                return Foo_INSIDE_CLASS_my_variable_inner_get(self.__externalRCRef())
            }
            set {
                return Foo_INSIDE_CLASS_my_variable_inner_set__TypesOfArguments__int64_t__(self.__externalRCRef(), newValue)
            }
        }
        public override init() {
            let __kt = Foo_INSIDE_CLASS_init_allocate()
            super.init(__externalRCRef: __kt)
            Foo_INSIDE_CLASS_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public func my_func() -> Swift.Bool {
            return Foo_INSIDE_CLASS_my_func(self.__externalRCRef())
        }
    }
    public var my_value: Swift.UInt32 {
        get {
            return Foo_my_value_get(self.__externalRCRef())
        }
    }
    public var my_variable: Swift.Int64 {
        get {
            return Foo_my_variable_get(self.__externalRCRef())
        }
        set {
            return Foo_my_variable_set__TypesOfArguments__int64_t__(self.__externalRCRef(), newValue)
        }
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public init(
        a: Swift.Int32
    ) {
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Foo_init_initialize__TypesOfArguments__uintptr_t_int32_t__(__kt, a)
    }
    public init(
        f: Swift.Float
    ) {
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Foo_init_initialize__TypesOfArguments__uintptr_t_float__(__kt, f)
    }
    public func foo() -> Swift.Bool {
        return Foo_foo(self.__externalRCRef())
    }
}
public final class OBJECT_NO_PACKAGE : KotlinRuntime.KotlinBase {
    public final class Bar : KotlinRuntime.KotlinBase {
        public final class CLASS_INSIDE_CLASS_INSIDE_OBJECT : KotlinRuntime.KotlinBase {
            public override init() {
                let __kt = OBJECT_NO_PACKAGE_Bar_CLASS_INSIDE_CLASS_INSIDE_OBJECT_init_allocate()
                super.init(__externalRCRef: __kt)
                OBJECT_NO_PACKAGE_Bar_CLASS_INSIDE_CLASS_INSIDE_OBJECT_init_initialize__TypesOfArguments__uintptr_t__(__kt)
            }
            public override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
        }
        public var i: Swift.Int32 {
            get {
                return OBJECT_NO_PACKAGE_Bar_i_get(self.__externalRCRef())
            }
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public init(
            i: Swift.Int32
        ) {
            let __kt = OBJECT_NO_PACKAGE_Bar_init_allocate()
            super.init(__externalRCRef: __kt)
            OBJECT_NO_PACKAGE_Bar_init_initialize__TypesOfArguments__uintptr_t_int32_t__(__kt, i)
        }
        public func bar() -> Swift.Int32 {
            return OBJECT_NO_PACKAGE_Bar_bar(self.__externalRCRef())
        }
    }
    public final class Foo : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = OBJECT_NO_PACKAGE_Foo_init_allocate()
            super.init(__externalRCRef: __kt)
            OBJECT_NO_PACKAGE_Foo_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public final class OBJECT_INSIDE_OBJECT : KotlinRuntime.KotlinBase {
        public static var shared: main.OBJECT_NO_PACKAGE.OBJECT_INSIDE_OBJECT {
            get {
                return main.OBJECT_NO_PACKAGE.OBJECT_INSIDE_OBJECT(__externalRCRef: OBJECT_NO_PACKAGE_OBJECT_INSIDE_OBJECT_get())
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
    public static var shared: main.OBJECT_NO_PACKAGE {
        get {
            return main.OBJECT_NO_PACKAGE(__externalRCRef: __root___OBJECT_NO_PACKAGE_get())
        }
    }
    public var value: Swift.Int32 {
        get {
            return OBJECT_NO_PACKAGE_value_get(self.__externalRCRef())
        }
    }
    public var variable: Swift.Int32 {
        get {
            return OBJECT_NO_PACKAGE_variable_get(self.__externalRCRef())
        }
        set {
            return OBJECT_NO_PACKAGE_variable_set__TypesOfArguments__int32_t__(self.__externalRCRef(), newValue)
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
    public func foo() -> Swift.Int32 {
        return OBJECT_NO_PACKAGE_foo(self.__externalRCRef())
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
public extension ExportedKotlinPackages.namespace.deeper {
    public final class DATA_OBJECT_WITH_PACKAGE : KotlinRuntime.KotlinBase {
        public static var shared: ExportedKotlinPackages.namespace.deeper.DATA_OBJECT_WITH_PACKAGE {
            get {
                return ExportedKotlinPackages.namespace.deeper.DATA_OBJECT_WITH_PACKAGE(__externalRCRef: namespace_deeper_DATA_OBJECT_WITH_PACKAGE_get())
            }
        }
        public var value: Swift.Int32 {
            get {
                return namespace_deeper_DATA_OBJECT_WITH_PACKAGE_value_get(self.__externalRCRef())
            }
        }
        public var variable: Swift.Int32 {
            get {
                return namespace_deeper_DATA_OBJECT_WITH_PACKAGE_variable_get(self.__externalRCRef())
            }
            set {
                return namespace_deeper_DATA_OBJECT_WITH_PACKAGE_variable_set__TypesOfArguments__int32_t__(self.__externalRCRef(), newValue)
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
        public func foo() -> Swift.Int32 {
            return namespace_deeper_DATA_OBJECT_WITH_PACKAGE_foo(self.__externalRCRef())
        }
        public func hashCode() -> Swift.Int32 {
            return namespace_deeper_DATA_OBJECT_WITH_PACKAGE_hashCode(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return namespace_deeper_DATA_OBJECT_WITH_PACKAGE_toString(self.__externalRCRef())
        }
    }
    public final class Foo : KotlinRuntime.KotlinBase {
        public final class INSIDE_CLASS : KotlinRuntime.KotlinBase {
            public final class DEEPER_INSIDE_CLASS : KotlinRuntime.KotlinBase {
                public var my_value: Swift.UInt32 {
                    get {
                        return namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_my_value_get(self.__externalRCRef())
                    }
                }
                public var my_variable: Swift.Int64 {
                    get {
                        return namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_my_variable_get(self.__externalRCRef())
                    }
                    set {
                        return namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_my_variable_set__TypesOfArguments__int64_t__(self.__externalRCRef(), newValue)
                    }
                }
                public override init() {
                    let __kt = namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_init_allocate()
                    super.init(__externalRCRef: __kt)
                    namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_init_initialize__TypesOfArguments__uintptr_t__(__kt)
                }
                public override init(
                    __externalRCRef: Swift.UInt
                ) {
                    super.init(__externalRCRef: __externalRCRef)
                }
                public func foo() -> Swift.Bool {
                    return namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_foo(self.__externalRCRef())
                }
            }
            public var my_value: Swift.UInt32 {
                get {
                    return namespace_deeper_Foo_INSIDE_CLASS_my_value_get(self.__externalRCRef())
                }
            }
            public var my_variable: Swift.Int64 {
                get {
                    return namespace_deeper_Foo_INSIDE_CLASS_my_variable_get(self.__externalRCRef())
                }
                set {
                    return namespace_deeper_Foo_INSIDE_CLASS_my_variable_set__TypesOfArguments__int64_t__(self.__externalRCRef(), newValue)
                }
            }
            public override init() {
                let __kt = namespace_deeper_Foo_INSIDE_CLASS_init_allocate()
                super.init(__externalRCRef: __kt)
                namespace_deeper_Foo_INSIDE_CLASS_init_initialize__TypesOfArguments__uintptr_t__(__kt)
            }
            public override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            public func foo() -> Swift.Bool {
                return namespace_deeper_Foo_INSIDE_CLASS_foo(self.__externalRCRef())
            }
        }
        public var my_value: Swift.UInt32 {
            get {
                return namespace_deeper_Foo_my_value_get(self.__externalRCRef())
            }
        }
        public var my_variable: Swift.Int64 {
            get {
                return namespace_deeper_Foo_my_variable_get(self.__externalRCRef())
            }
            set {
                return namespace_deeper_Foo_my_variable_set__TypesOfArguments__int64_t__(self.__externalRCRef(), newValue)
            }
        }
        public override init() {
            let __kt = namespace_deeper_Foo_init_allocate()
            super.init(__externalRCRef: __kt)
            namespace_deeper_Foo_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public func foo() -> Swift.Bool {
            return namespace_deeper_Foo_foo(self.__externalRCRef())
        }
    }
    public final class NAMESPACED_CLASS : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = namespace_deeper_NAMESPACED_CLASS_init_allocate()
            super.init(__externalRCRef: __kt)
            namespace_deeper_NAMESPACED_CLASS_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public final class OBJECT_WITH_PACKAGE : KotlinRuntime.KotlinBase {
        public final class Bar : KotlinRuntime.KotlinBase {
            public final class OBJECT_INSIDE_CLASS : KotlinRuntime.KotlinBase {
                public static var shared: ExportedKotlinPackages.namespace.deeper.OBJECT_WITH_PACKAGE.Bar.OBJECT_INSIDE_CLASS {
                    get {
                        return ExportedKotlinPackages.namespace.deeper.OBJECT_WITH_PACKAGE.Bar.OBJECT_INSIDE_CLASS(__externalRCRef: namespace_deeper_OBJECT_WITH_PACKAGE_Bar_OBJECT_INSIDE_CLASS_get())
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
            public var i: Swift.Int32 {
                get {
                    return namespace_deeper_OBJECT_WITH_PACKAGE_Bar_i_get(self.__externalRCRef())
                }
            }
            public override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            public init(
                i: Swift.Int32
            ) {
                let __kt = namespace_deeper_OBJECT_WITH_PACKAGE_Bar_init_allocate()
                super.init(__externalRCRef: __kt)
                namespace_deeper_OBJECT_WITH_PACKAGE_Bar_init_initialize__TypesOfArguments__uintptr_t_int32_t__(__kt, i)
            }
            public func bar() -> Swift.Int32 {
                return namespace_deeper_OBJECT_WITH_PACKAGE_Bar_bar(self.__externalRCRef())
            }
        }
        public final class Foo : KotlinRuntime.KotlinBase {
            public override init() {
                let __kt = namespace_deeper_OBJECT_WITH_PACKAGE_Foo_init_allocate()
                super.init(__externalRCRef: __kt)
                namespace_deeper_OBJECT_WITH_PACKAGE_Foo_init_initialize__TypesOfArguments__uintptr_t__(__kt)
            }
            public override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
        }
        public final class OBJECT_INSIDE_OBJECT : KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.namespace.deeper.OBJECT_WITH_PACKAGE.OBJECT_INSIDE_OBJECT {
                get {
                    return ExportedKotlinPackages.namespace.deeper.OBJECT_WITH_PACKAGE.OBJECT_INSIDE_OBJECT(__externalRCRef: namespace_deeper_OBJECT_WITH_PACKAGE_OBJECT_INSIDE_OBJECT_get())
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
        public static var shared: ExportedKotlinPackages.namespace.deeper.OBJECT_WITH_PACKAGE {
            get {
                return ExportedKotlinPackages.namespace.deeper.OBJECT_WITH_PACKAGE(__externalRCRef: namespace_deeper_OBJECT_WITH_PACKAGE_get())
            }
        }
        public var value: Swift.Int32 {
            get {
                return namespace_deeper_OBJECT_WITH_PACKAGE_value_get(self.__externalRCRef())
            }
        }
        public var variable: Swift.Int32 {
            get {
                return namespace_deeper_OBJECT_WITH_PACKAGE_variable_get(self.__externalRCRef())
            }
            set {
                return namespace_deeper_OBJECT_WITH_PACKAGE_variable_set__TypesOfArguments__int32_t__(self.__externalRCRef(), newValue)
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
        public func foo() -> Swift.Int32 {
            return namespace_deeper_OBJECT_WITH_PACKAGE_foo(self.__externalRCRef())
        }
    }
}
public extension ExportedKotlinPackages.namespace {
    public final class Foo : KotlinRuntime.KotlinBase {
        public final class INSIDE_CLASS : KotlinRuntime.KotlinBase {
            public override init() {
                let __kt = namespace_Foo_INSIDE_CLASS_init_allocate()
                super.init(__externalRCRef: __kt)
                namespace_Foo_INSIDE_CLASS_init_initialize__TypesOfArguments__uintptr_t__(__kt)
            }
            public override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
        }
        public var my_value: Swift.UInt32 {
            get {
                return namespace_Foo_my_value_get(self.__externalRCRef())
            }
        }
        public var my_variable: Swift.Int64 {
            get {
                return namespace_Foo_my_variable_get(self.__externalRCRef())
            }
            set {
                return namespace_Foo_my_variable_set__TypesOfArguments__int64_t__(self.__externalRCRef(), newValue)
            }
        }
        public override init() {
            let __kt = namespace_Foo_init_allocate()
            super.init(__externalRCRef: __kt)
            namespace_Foo_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public func foo() -> Swift.Bool {
            return namespace_Foo_foo(self.__externalRCRef())
        }
    }
    public final class NAMESPACED_CLASS : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = namespace_NAMESPACED_CLASS_init_allocate()
            super.init(__externalRCRef: __kt)
            namespace_NAMESPACED_CLASS_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
}
public extension ExportedKotlinPackages.why_we_need_module_names {
    public final class CLASS_WITH_SAME_NAME : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = why_we_need_module_names_CLASS_WITH_SAME_NAME_init_allocate()
            super.init(__externalRCRef: __kt)
            why_we_need_module_names_CLASS_WITH_SAME_NAME_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public func foo() -> Swift.Void {
            return why_we_need_module_names_CLASS_WITH_SAME_NAME_foo(self.__externalRCRef())
        }
    }
    public static func bar() -> Swift.Int32 {
        return why_we_need_module_names_bar()
    }
    public static func foo() -> main.CLASS_WITH_SAME_NAME {
        return main.CLASS_WITH_SAME_NAME(__externalRCRef: why_we_need_module_names_foo())
    }
}

@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

open class ABSTRACT_CLASS: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    package override init() {
        fatalError()
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public final class CLASS_WITH_SAME_NAME: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public override init() {
        let __kt = __root___CLASS_WITH_SAME_NAME_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___CLASS_WITH_SAME_NAME_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public func foo() -> Swift.Int32 {
        return CLASS_WITH_SAME_NAME_foo(self.__externalRCRef())
    }
}
public final class ClassWithNonPublicConstructor: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public var a: Swift.Int32 {
        get {
            return ClassWithNonPublicConstructor_a_get(self.__externalRCRef())
        }
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public final class DATA_CLASS: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public var a: Swift.Int32 {
        get {
            return DATA_CLASS_a_get(self.__externalRCRef())
        }
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public init(
        a: Swift.Int32
    ) {
        let __kt = __root___DATA_CLASS_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___DATA_CLASS_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt, a)
    }
    public func copy(
        a: Swift.Int32
    ) -> main.DATA_CLASS {
        return main.DATA_CLASS(__externalRCRef: DATA_CLASS_copy__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), a))
    }
    public func hashCode() -> Swift.Int32 {
        return DATA_CLASS_hashCode(self.__externalRCRef())
    }
    public func toString() -> Swift.String {
        return DATA_CLASS_toString(self.__externalRCRef())
    }
}
public final class DATA_CLASS_WITH_MANY_FIELDS: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public var a: Swift.Int32 {
        get {
            return DATA_CLASS_WITH_MANY_FIELDS_a_get(self.__externalRCRef())
        }
    }
    public var b: Swift.String {
        get {
            return DATA_CLASS_WITH_MANY_FIELDS_b_get(self.__externalRCRef())
        }
    }
    public var c: KotlinRuntime.KotlinBase {
        get {
            return KotlinRuntime.KotlinBase(__externalRCRef: DATA_CLASS_WITH_MANY_FIELDS_c_get(self.__externalRCRef()))
        }
    }
    public var d: Swift.Double {
        get {
            return DATA_CLASS_WITH_MANY_FIELDS_d_get(self.__externalRCRef())
        }
    }
    public var e: Swift.String {
        get {
            return DATA_CLASS_WITH_MANY_FIELDS_e_get(self.__externalRCRef())
        }
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public init(
        a: Swift.Int32,
        b: Swift.String,
        c: KotlinRuntime.KotlinBase
    ) {
        let __kt = __root___DATA_CLASS_WITH_MANY_FIELDS_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___DATA_CLASS_WITH_MANY_FIELDS_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32_Swift_String_KotlinRuntime_KotlinBase__(__kt, a, b, c.__externalRCRef())
    }
    public func copy(
        a: Swift.Int32,
        b: Swift.String,
        c: KotlinRuntime.KotlinBase
    ) -> main.DATA_CLASS_WITH_MANY_FIELDS {
        return main.DATA_CLASS_WITH_MANY_FIELDS(__externalRCRef: DATA_CLASS_WITH_MANY_FIELDS_copy__TypesOfArguments__Swift_Int32_Swift_String_KotlinRuntime_KotlinBase__(self.__externalRCRef(), a, b, c.__externalRCRef()))
    }
    public func hashCode() -> Swift.Int32 {
        return DATA_CLASS_WITH_MANY_FIELDS_hashCode(self.__externalRCRef())
    }
    public func toString() -> Swift.String {
        return DATA_CLASS_WITH_MANY_FIELDS_toString(self.__externalRCRef())
    }
}
public final class DATA_CLASS_WITH_REF: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public var o: KotlinRuntime.KotlinBase {
        get {
            return KotlinRuntime.KotlinBase(__externalRCRef: DATA_CLASS_WITH_REF_o_get(self.__externalRCRef()))
        }
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public init(
        o: KotlinRuntime.KotlinBase
    ) {
        let __kt = __root___DATA_CLASS_WITH_REF_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___DATA_CLASS_WITH_REF_init_initialize__TypesOfArguments__Swift_UInt_KotlinRuntime_KotlinBase__(__kt, o.__externalRCRef())
    }
    public func copy(
        o: KotlinRuntime.KotlinBase
    ) -> main.DATA_CLASS_WITH_REF {
        return main.DATA_CLASS_WITH_REF(__externalRCRef: DATA_CLASS_WITH_REF_copy__TypesOfArguments__KotlinRuntime_KotlinBase__(self.__externalRCRef(), o.__externalRCRef()))
    }
    public func hashCode() -> Swift.Int32 {
        return DATA_CLASS_WITH_REF_hashCode(self.__externalRCRef())
    }
    public func toString() -> Swift.String {
        return DATA_CLASS_WITH_REF_toString(self.__externalRCRef())
    }
}
public final class ENUM: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged, Swift.CaseIterable {
    public final class INSIDE_ENUM: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public override init() {
            let __kt = ENUM_INSIDE_ENUM_init_allocate()
            super.init(__externalRCRef: __kt)
            ENUM_INSIDE_ENUM_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public static var A: main.ENUM {
        get {
            return main.ENUM(__externalRCRef: ENUM_A_get())
        }
    }
    public static var B: main.ENUM {
        get {
            return main.ENUM(__externalRCRef: ENUM_B_get())
        }
    }
    public static var C: main.ENUM {
        get {
            return main.ENUM(__externalRCRef: ENUM_C_get())
        }
    }
    public static var allCases: [main.ENUM] {
        get {
            return ENUM_entries_get() as! Swift.Array<main.ENUM>
        }
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public static func valueOf(
        value: Swift.String
    ) -> main.ENUM {
        return main.ENUM(__externalRCRef: ENUM_valueOf__TypesOfArguments__Swift_String__(value))
    }
}
public final class Foo: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public final class Companion: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public var my_value_inner: Swift.UInt32 {
            get {
                return Foo_Companion_my_value_inner_get(self.__externalRCRef())
            }
        }
        public var my_variable_inner: Swift.Int64 {
            get {
                return Foo_Companion_my_variable_inner_get(self.__externalRCRef())
            }
            set {
                return Foo_Companion_my_variable_inner_set__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), newValue)
            }
        }
        public static var shared: main.Foo.Companion {
            get {
                return main.Foo.Companion(__externalRCRef: Foo_Companion_get())
            }
        }
        private override init() {
            fatalError()
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public func my_func() -> Swift.Bool {
            return Foo_Companion_my_func(self.__externalRCRef())
        }
    }
    public final class INSIDE_CLASS: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
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
                return Foo_INSIDE_CLASS_my_variable_inner_set__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), newValue)
            }
        }
        public override init() {
            let __kt = Foo_INSIDE_CLASS_init_allocate()
            super.init(__externalRCRef: __kt)
            Foo_INSIDE_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
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
            return Foo_my_variable_set__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), newValue)
        }
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public init(
        a: Swift.Int32
    ) {
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Foo_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt, a)
    }
    public init(
        f: Swift.Float
    ) {
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Foo_init_initialize__TypesOfArguments__Swift_UInt_Swift_Float__(__kt, f)
    }
    public func foo() -> Swift.Bool {
        return Foo_foo(self.__externalRCRef())
    }
}
public final class OBJECT_NO_PACKAGE: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public final class Bar: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public final class CLASS_INSIDE_CLASS_INSIDE_OBJECT: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public override init() {
                let __kt = OBJECT_NO_PACKAGE_Bar_CLASS_INSIDE_CLASS_INSIDE_OBJECT_init_allocate()
                super.init(__externalRCRef: __kt)
                OBJECT_NO_PACKAGE_Bar_CLASS_INSIDE_CLASS_INSIDE_OBJECT_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
        }
        public final class NamedCompanion: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public static var shared: main.OBJECT_NO_PACKAGE.Bar.NamedCompanion {
                get {
                    return main.OBJECT_NO_PACKAGE.Bar.NamedCompanion(__externalRCRef: OBJECT_NO_PACKAGE_Bar_NamedCompanion_get())
                }
            }
            private override init() {
                fatalError()
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            public func foo() -> Swift.Int32 {
                return OBJECT_NO_PACKAGE_Bar_NamedCompanion_foo(self.__externalRCRef())
            }
        }
        public var i: Swift.Int32 {
            get {
                return OBJECT_NO_PACKAGE_Bar_i_get(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public init(
            i: Swift.Int32
        ) {
            let __kt = OBJECT_NO_PACKAGE_Bar_init_allocate()
            super.init(__externalRCRef: __kt)
            OBJECT_NO_PACKAGE_Bar_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt, i)
        }
        public func bar() -> Swift.Int32 {
            return OBJECT_NO_PACKAGE_Bar_bar(self.__externalRCRef())
        }
    }
    public final class Foo: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public override init() {
            let __kt = OBJECT_NO_PACKAGE_Foo_init_allocate()
            super.init(__externalRCRef: __kt)
            OBJECT_NO_PACKAGE_Foo_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public final class OBJECT_INSIDE_OBJECT: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public static var shared: main.OBJECT_NO_PACKAGE.OBJECT_INSIDE_OBJECT {
            get {
                return main.OBJECT_NO_PACKAGE.OBJECT_INSIDE_OBJECT(__externalRCRef: OBJECT_NO_PACKAGE_OBJECT_INSIDE_OBJECT_get())
            }
        }
        private override init() {
            fatalError()
        }
        package override init(
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
            return OBJECT_NO_PACKAGE_variable_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue)
        }
    }
    private override init() {
        fatalError()
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public func foo() -> Swift.Int32 {
        return OBJECT_NO_PACKAGE_foo(self.__externalRCRef())
    }
}
open class SEALED: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
    public final class C: main.SEALED {
        public override init() {
            let __kt = SEALED_C_init_allocate()
            super.init(__externalRCRef: __kt)
            SEALED_C_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public final class O: main.SEALED {
        public static var shared: main.SEALED.O {
            get {
                return main.SEALED.O(__externalRCRef: SEALED_O_get())
            }
        }
        private override init() {
            fatalError()
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    package override init() {
        fatalError()
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public extension ExportedKotlinPackages.namespace.deeper {
    public final class DATA_OBJECT_WITH_PACKAGE: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
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
                return namespace_deeper_DATA_OBJECT_WITH_PACKAGE_variable_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue)
            }
        }
        private override init() {
            fatalError()
        }
        package override init(
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
    public final class Foo: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public final class INSIDE_CLASS: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public final class DEEPER_INSIDE_CLASS: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
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
                        return namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_my_variable_set__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), newValue)
                    }
                }
                public override init() {
                    let __kt = namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_init_allocate()
                    super.init(__externalRCRef: __kt)
                    namespace_deeper_Foo_INSIDE_CLASS_DEEPER_INSIDE_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
                }
                package override init(
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
                    return namespace_deeper_Foo_INSIDE_CLASS_my_variable_set__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), newValue)
                }
            }
            public override init() {
                let __kt = namespace_deeper_Foo_INSIDE_CLASS_init_allocate()
                super.init(__externalRCRef: __kt)
                namespace_deeper_Foo_INSIDE_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
            }
            package override init(
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
                return namespace_deeper_Foo_my_variable_set__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), newValue)
            }
        }
        public override init() {
            let __kt = namespace_deeper_Foo_init_allocate()
            super.init(__externalRCRef: __kt)
            namespace_deeper_Foo_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public func foo() -> Swift.Bool {
            return namespace_deeper_Foo_foo(self.__externalRCRef())
        }
    }
    public final class NAMESPACED_CLASS: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public override init() {
            let __kt = namespace_deeper_NAMESPACED_CLASS_init_allocate()
            super.init(__externalRCRef: __kt)
            namespace_deeper_NAMESPACED_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public final class OBJECT_WITH_PACKAGE: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public final class Bar: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public final class OBJECT_INSIDE_CLASS: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
                public static var shared: ExportedKotlinPackages.namespace.deeper.OBJECT_WITH_PACKAGE.Bar.OBJECT_INSIDE_CLASS {
                    get {
                        return ExportedKotlinPackages.namespace.deeper.OBJECT_WITH_PACKAGE.Bar.OBJECT_INSIDE_CLASS(__externalRCRef: namespace_deeper_OBJECT_WITH_PACKAGE_Bar_OBJECT_INSIDE_CLASS_get())
                    }
                }
                private override init() {
                    fatalError()
                }
                package override init(
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
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            public init(
                i: Swift.Int32
            ) {
                let __kt = namespace_deeper_OBJECT_WITH_PACKAGE_Bar_init_allocate()
                super.init(__externalRCRef: __kt)
                namespace_deeper_OBJECT_WITH_PACKAGE_Bar_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt, i)
            }
            public func bar() -> Swift.Int32 {
                return namespace_deeper_OBJECT_WITH_PACKAGE_Bar_bar(self.__externalRCRef())
            }
        }
        public final class Foo: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public override init() {
                let __kt = namespace_deeper_OBJECT_WITH_PACKAGE_Foo_init_allocate()
                super.init(__externalRCRef: __kt)
                namespace_deeper_OBJECT_WITH_PACKAGE_Foo_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
            }
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
        }
        public final class OBJECT_INSIDE_OBJECT: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public static var shared: ExportedKotlinPackages.namespace.deeper.OBJECT_WITH_PACKAGE.OBJECT_INSIDE_OBJECT {
                get {
                    return ExportedKotlinPackages.namespace.deeper.OBJECT_WITH_PACKAGE.OBJECT_INSIDE_OBJECT(__externalRCRef: namespace_deeper_OBJECT_WITH_PACKAGE_OBJECT_INSIDE_OBJECT_get())
                }
            }
            private override init() {
                fatalError()
            }
            package override init(
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
                return namespace_deeper_OBJECT_WITH_PACKAGE_variable_set__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), newValue)
            }
        }
        private override init() {
            fatalError()
        }
        package override init(
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
    public final class Foo: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public final class INSIDE_CLASS: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public override init() {
                let __kt = namespace_Foo_INSIDE_CLASS_init_allocate()
                super.init(__externalRCRef: __kt)
                namespace_Foo_INSIDE_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
            }
            package override init(
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
                return namespace_Foo_my_variable_set__TypesOfArguments__Swift_Int64__(self.__externalRCRef(), newValue)
            }
        }
        public override init() {
            let __kt = namespace_Foo_init_allocate()
            super.init(__externalRCRef: __kt)
            namespace_Foo_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public func foo() -> Swift.Bool {
            return namespace_Foo_foo(self.__externalRCRef())
        }
    }
    public final class NAMESPACED_CLASS: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public override init() {
            let __kt = namespace_NAMESPACED_CLASS_init_allocate()
            super.init(__externalRCRef: __kt)
            namespace_NAMESPACED_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
}
public extension ExportedKotlinPackages.why_we_need_module_names {
    public final class CLASS_WITH_SAME_NAME: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public override init() {
            let __kt = why_we_need_module_names_CLASS_WITH_SAME_NAME_init_allocate()
            super.init(__externalRCRef: __kt)
            why_we_need_module_names_CLASS_WITH_SAME_NAME_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
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

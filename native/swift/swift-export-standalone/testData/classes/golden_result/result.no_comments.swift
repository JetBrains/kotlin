import KotlinRuntime
import KotlinBridges

public class CLASS_WITH_SAME_NAME : KotlinRuntime.KotlinBase {
    public override init() {
        fatalError()
    }
    public func foo() -> Swift.Int32 {
        fatalError()
    }
}
public class ClassWithNonPublicConstructor : KotlinRuntime.KotlinBase {
    public var a: Swift.Int32 {
        get {
            fatalError()
        }
    }
}
public class Foo : KotlinRuntime.KotlinBase {
    public class INSIDE_CLASS : KotlinRuntime.KotlinBase {
        public var my_value_inner: Swift.UInt32 {
            get {
                fatalError()
            }
        }
        public var my_variable_inner: Swift.Int64 {
            get {
                fatalError()
            }
            set {
                fatalError()
            }
        }
        public override init() {
            fatalError()
        }
        public func my_func() -> Swift.Bool {
            fatalError()
        }
    }
    public var my_value: Swift.UInt32 {
        get {
            fatalError()
        }
    }
    public var my_variable: Swift.Int64 {
        get {
            fatalError()
        }
        set {
            fatalError()
        }
    }
    public init(
        a: Swift.Int32
    ) {
        fatalError()
    }
    public init(
        f: Swift.Float
    ) {
        fatalError()
    }
    public func foo() -> Swift.Bool {
        fatalError()
    }
}
public class OBJECT_NO_PACKAGE : KotlinRuntime.KotlinBase {
    public class Bar : KotlinRuntime.KotlinBase {
        public class CLASS_INSIDE_CLASS_INSIDE_OBJECT : KotlinRuntime.KotlinBase {
            public override init() {
                fatalError()
            }
        }
        public var i: Swift.Int32 {
            get {
                fatalError()
            }
        }
        public init(
            i: Swift.Int32
        ) {
            fatalError()
        }
        public func bar() -> Swift.Int32 {
            fatalError()
        }
    }
    public class Foo : KotlinRuntime.KotlinBase {
        public override init() {
            fatalError()
        }
    }
    public class OBJECT_INSIDE_OBJECT : KotlinRuntime.KotlinBase {
        public static var shared: Swift.Int32 {
            get {
                fatalError()
            }
        }
        private override init() {
            fatalError()
        }
    }
    public static var shared: Swift.Int32 {
        get {
            fatalError()
        }
    }
    public var value: Swift.Int32 {
        get {
            fatalError()
        }
    }
    public var variable: Swift.Int32 {
        get {
            fatalError()
        }
        set {
            fatalError()
        }
    }
    private override init() {
        fatalError()
    }
    public func foo() -> Swift.Int32 {
        fatalError()
    }
}
public extension main.namespace.deeper {
    public class Foo : KotlinRuntime.KotlinBase {
        public class INSIDE_CLASS : KotlinRuntime.KotlinBase {
            public class DEEPER_INSIDE_CLASS : KotlinRuntime.KotlinBase {
                public var my_value: Swift.UInt32 {
                    get {
                        fatalError()
                    }
                }
                public var my_variable: Swift.Int64 {
                    get {
                        fatalError()
                    }
                    set {
                        fatalError()
                    }
                }
                public override init() {
                    fatalError()
                }
                public func foo() -> Swift.Bool {
                    fatalError()
                }
            }
            public var my_value: Swift.UInt32 {
                get {
                    fatalError()
                }
            }
            public var my_variable: Swift.Int64 {
                get {
                    fatalError()
                }
                set {
                    fatalError()
                }
            }
            public override init() {
                fatalError()
            }
            public func foo() -> Swift.Bool {
                fatalError()
            }
        }
        public var my_value: Swift.UInt32 {
            get {
                fatalError()
            }
        }
        public var my_variable: Swift.Int64 {
            get {
                fatalError()
            }
            set {
                fatalError()
            }
        }
        public override init() {
            fatalError()
        }
        public func foo() -> Swift.Bool {
            fatalError()
        }
    }
    public class NAMESPACED_CLASS : KotlinRuntime.KotlinBase {
        public override init() {
            fatalError()
        }
    }
    public class OBJECT_WITH_PACKAGE : KotlinRuntime.KotlinBase {
        public class Bar : KotlinRuntime.KotlinBase {
            public class OBJECT_INSIDE_CLASS : KotlinRuntime.KotlinBase {
                public static var shared: Swift.Int32 {
                    get {
                        fatalError()
                    }
                }
                private override init() {
                    fatalError()
                }
            }
            public var i: Swift.Int32 {
                get {
                    fatalError()
                }
            }
            public init(
                i: Swift.Int32
            ) {
                fatalError()
            }
            public func bar() -> Swift.Int32 {
                fatalError()
            }
        }
        public class Foo : KotlinRuntime.KotlinBase {
            public override init() {
                fatalError()
            }
        }
        public class OBJECT_INSIDE_OBJECT : KotlinRuntime.KotlinBase {
            public static var shared: Swift.Int32 {
                get {
                    fatalError()
                }
            }
            private override init() {
                fatalError()
            }
        }
        public static var shared: Swift.Int32 {
            get {
                fatalError()
            }
        }
        public var value: Swift.Int32 {
            get {
                fatalError()
            }
        }
        public var variable: Swift.Int32 {
            get {
                fatalError()
            }
            set {
                fatalError()
            }
        }
        private override init() {
            fatalError()
        }
        public func foo() -> Swift.Int32 {
            fatalError()
        }
    }
}
public extension main.namespace {
    public class Foo : KotlinRuntime.KotlinBase {
        public class INSIDE_CLASS : KotlinRuntime.KotlinBase {
            public override init() {
                fatalError()
            }
        }
        public var my_value: Swift.UInt32 {
            get {
                fatalError()
            }
        }
        public var my_variable: Swift.Int64 {
            get {
                fatalError()
            }
            set {
                fatalError()
            }
        }
        public override init() {
            fatalError()
        }
        public func foo() -> Swift.Bool {
            fatalError()
        }
    }
    public class NAMESPACED_CLASS : KotlinRuntime.KotlinBase {
        public override init() {
            fatalError()
        }
    }
}
public extension main.why_we_need_module_names {
    public class CLASS_WITH_SAME_NAME : KotlinRuntime.KotlinBase {
        public override init() {
            fatalError()
        }
        public func foo() -> Swift.Void {
            fatalError()
        }
    }
    public static func bar() -> Swift.Int32 {
        return why_we_need_module_names_bar()
    }
    public static func foo() -> main.CLASS_WITH_SAME_NAME {
        fatalError()
    }
}
public enum namespace {
    public enum deeper {
    }
}
public enum why_we_need_module_names {
}

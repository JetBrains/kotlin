import KotlinBridges
import KotlinRuntime

public class ClassWithNonPublicConstructor {
    public var a: Swift.Int32 {
        get {
            fatalError()
        }
    }
}
public class DATA_OBJECT_WITHOUT_PACKAGE {
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
    private init() {
        fatalError()
    }
    public func foo() -> Swift.Int32 {
        fatalError()
    }
}
public class Foo {
    public class INSIDE_CLASS {
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
        public init() {
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
public class OBJECT_NO_PACKAGE {
    public class Bar {
        public class CLASS_INSIDE_CLASS_INSIDE_OBJECT {
            public init() {
                fatalError()
            }
        }
        public class DATA_OBJECT_INSIDE_CLASS_INSIDE_OBJECT {
            public static var shared: Swift.Int32 {
                get {
                    fatalError()
                }
            }
            private init() {
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
    public class Foo {
        public init() {
            fatalError()
        }
    }
    public class OBJECT_INSIDE_OBJECT {
        public static var shared: Swift.Int32 {
            get {
                fatalError()
            }
        }
        private init() {
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
    private init() {
        fatalError()
    }
    public func foo() -> Swift.Int32 {
        fatalError()
    }
}
public extension main.namespace.deeper {
    public class DATA_OBJECT_WITH_PACKAGE {
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
        private init() {
            fatalError()
        }
        public func foo() -> Swift.Int32 {
            fatalError()
        }
    }
    public class Foo {
        public class INSIDE_CLASS {
            public class DEEPER_INSIDE_CLASS {
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
                public init() {
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
            public init() {
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
        public init() {
            fatalError()
        }
        public func foo() -> Swift.Bool {
            fatalError()
        }
    }
    public class NAMESPACED_CLASS {
        public init() {
            fatalError()
        }
    }
    public class OBJECT_WITH_PACKAGE {
        public class Bar {
            public class OBJECT_INSIDE_CLASS {
                public static var shared: Swift.Int32 {
                    get {
                        fatalError()
                    }
                }
                private init() {
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
        public class Foo {
            public init() {
                fatalError()
            }
        }
        public class OBJECT_INSIDE_OBJECT {
            public static var shared: Swift.Int32 {
                get {
                    fatalError()
                }
            }
            private init() {
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
        private init() {
            fatalError()
        }
        public func foo() -> Swift.Int32 {
            fatalError()
        }
    }
}
public extension main.namespace {
    public class Foo {
        public class INSIDE_CLASS {
            public init() {
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
        public init() {
            fatalError()
        }
        public func foo() -> Swift.Bool {
            fatalError()
        }
    }
    public class NAMESPACED_CLASS {
        public init() {
            fatalError()
        }
    }
}
public enum namespace {
    public enum deeper {
    }
}

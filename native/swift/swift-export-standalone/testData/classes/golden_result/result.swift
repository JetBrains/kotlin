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
/**
* this is a sample comment for class without public constructor
*/
public class ClassWithNonPublicConstructor : KotlinRuntime.KotlinBase {
    public var a: Swift.Int32 {
        get {
            fatalError()
        }
    }
}
/**
* this is a sample comment for class without package
* in order to support documentation for primary constructor - we will have to start parsing comment content:
* https://kotlinlang.org/docs/kotlin-doc.html#constructor
*/
public class Foo : KotlinRuntime.KotlinBase {
    /**
    * this is a sample comment for INSIDE_CLASS without package
    */
    public class INSIDE_CLASS : KotlinRuntime.KotlinBase {
        /**
        * this is a sample comment for val on INSIDE_CLASS without package
        */
        public var my_value_inner: Swift.UInt32 {
            get {
                fatalError()
            }
        }
        /**
        * this is a sample comment for var on INSIDE_CLASS without package
        */
        public var my_variable_inner: Swift.Int64 {
            get {
                fatalError()
            }
            set {
                fatalError()
            }
        }
        /**
        * this is a sample comment for INSIDE_CLASS without package
        */
        public override init() {
            fatalError()
        }
        /**
        * this is a sample comment for func on INSIDE_CLASS without package
        */
        public func my_func() -> Swift.Bool {
            fatalError()
        }
    }
    /**
    * this is a sample comment for val on class without package
    */
    public var my_value: Swift.UInt32 {
        get {
            fatalError()
        }
    }
    /**
    * this is a sample comment for var on class without package
    */
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
    /**
    * this is a sample comment for secondary constructor
    */
    public init(
        f: Swift.Float
    ) {
        fatalError()
    }
    /**
    * this is a sample comment for func on class without package
    */
    public func foo() -> Swift.Bool {
        fatalError()
    }
}
/**
demo comment for packageless object
*/
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
    /**
    demo comment for packaged object
    */
    public class OBJECT_WITH_PACKAGE : KotlinRuntime.KotlinBase {
        public class Bar : KotlinRuntime.KotlinBase {
            /**
            * demo comment for inner object
            */
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
        /**
        * this is a sample comment for INSIDE_CLASS with package
        */
        public class INSIDE_CLASS : KotlinRuntime.KotlinBase {
            /**
            * this is a sample comment for INSIDE_CLASS with package
            */
            public override init() {
                fatalError()
            }
        }
        /**
        * this is a sample comment for val on class with package
        */
        public var my_value: Swift.UInt32 {
            get {
                fatalError()
            }
        }
        /**
        * this is a sample comment for var on class with package
        */
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
        /**
        * this is a sample comment for func on class with package
        */
        public func foo() -> Swift.Bool {
            fatalError()
        }
    }
    /**
    *  demo comment for
    *  NAMESPACED_CLASS
    */
    public class NAMESPACED_CLASS : KotlinRuntime.KotlinBase {
        /**
        *  demo comment for
        *  NAMESPACED_CLASS
        */
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
    /**
    * this will calculate the return type of `foo` on `CLASS_WITH_SAME_NAME`.
    * Return type of CLASS_WITH_SAME_NAME differs, so we can detect which one was used on Swift side.
    * We are expecting it to be the one that does not have a module - so it will be Swift.Int32.
    */
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

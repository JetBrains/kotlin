import KotlinBridges
import KotlinRuntime

public extension main.namespace.deeper {
    public class NAMESPACED_CLASS {
        public init() {
            fatalError()
        }
    }
    public class Foo {
        public func foo() -> Swift.Bool {
            fatalError()
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
        public class INSIDE_CLASS {
            public func foo() -> Swift.Bool {
                fatalError()
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
            public class DEEPER_INSIDE_CLASS {
                public func foo() -> Swift.Bool {
                    fatalError()
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
            }
            public init() {
                fatalError()
            }
        }
        public init() {
            fatalError()
        }
    }
}

public extension main.namespace {
    /**
    *  demo comment for
    *  NAMESPACED_CLASS
    */
    public class NAMESPACED_CLASS {
        /**
        *  demo comment for
        *  NAMESPACED_CLASS
        */
        public init() {
            fatalError()
        }
    }
    public class Foo {
        /**
        * this is a sample comment for func on class with package
        */
        public func foo() -> Swift.Bool {
            fatalError()
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
        /**
        * this is a sample comment for INSIDE_CLASS with package
        */
        public class INSIDE_CLASS {
            /**
            * this is a sample comment for INSIDE_CLASS with package
            */
            public init() {
                fatalError()
            }
        }
        public init() {
            fatalError()
        }
    }
}

public enum namespace {
    public enum deeper {
    }
}

/**
* this is a sample comment for class without public constructor
*/
public class ClassWithNonPublicConstructor {
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
public class Foo {
    /**
    * this is a sample comment for func on class without package
    */
    public func foo() -> Swift.Bool {
        fatalError()
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
    /**
    * this is a sample comment for INSIDE_CLASS without package
    */
    public class INSIDE_CLASS {
        /**
        * this is a sample comment for func on INSIDE_CLASS without package
        */
        public func my_func() -> Swift.Bool {
            fatalError()
        }
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
        public init() {
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
}

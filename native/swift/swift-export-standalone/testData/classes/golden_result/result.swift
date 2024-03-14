import KotlinBridges
import KotlinRuntime

public enum namespace {
    public enum deeper {
        public class NAMESPACED_CLASS {
            public init() {
                fatalError()
            }
        }
        public class Foo {
            public init() {
                fatalError()
            }
            public class INSIDE_CLASS {
                public init() {
                    fatalError()
                }
                public class DEEPER_INSIDE_CLASS {
                    public init() {
                        fatalError()
                    }
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
                }
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
            }
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
        }
    }
    public class NAMESPACED_CLASS {
        public init() {
            fatalError()
        }
    }
    public class Foo {
        public init() {
            fatalError()
        }
        public class INSIDE_CLASS {
            public init() {
                fatalError()
            }
        }
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
    }
}

public class ClassWithNonPublicConstructor {
}

public class Foo {
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
    public class INSIDE_CLASS {
        public init() {
            fatalError()
        }
        public func my_func() -> Swift.Bool {
            fatalError()
        }
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
    }
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
}

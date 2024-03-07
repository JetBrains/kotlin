import KotlinBridges
import KotlinRuntime

public enum namespace {
    public enum deeper {
        public class NAMESPACED_CLASS : KotlinRuntime.KotlinBase {
        }
        public class Foo : KotlinRuntime.KotlinBase {
            public class INSIDE_CLASS : KotlinRuntime.KotlinBase {
                public class DEEPER_INSIDE_CLASS : KotlinRuntime.KotlinBase {
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
    public class NAMESPACED_CLASS : KotlinRuntime.KotlinBase {
    }
    public class Foo : KotlinRuntime.KotlinBase {
        public class INSIDE_CLASS : KotlinRuntime.KotlinBase {
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
        public func createNamespacedClass() -> namespace.NAMESPACED_CLASS {
            fatalError()
        }
        public func createDeeperNamespacedClass() -> namespace.deeper.NAMESPACED_CLASS {
            fatalError()
        }
    }
}

public class Foo : KotlinRuntime.KotlinBase {
    public class INSIDE_CLASS : KotlinRuntime.KotlinBase {
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
    public func createInstance() -> Foo {
        fatalError()
    }
}

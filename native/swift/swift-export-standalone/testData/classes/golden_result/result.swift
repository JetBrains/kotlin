import KotlinBridges
import KotlinRuntime

public enum namespace {
    public enum deeper {
        public class NAMESPACED_CLASS {
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
        }
    }
    public class NAMESPACED_CLASS {
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
}

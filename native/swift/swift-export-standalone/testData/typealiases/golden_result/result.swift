import KotlinBridges
import KotlinRuntime

public typealias RegularInteger = Swift.Int32
public extension main.typealiases.inner {
    public typealias LargeInteger = Swift.Int64
    public typealias Foo = main.typealiases.Foo
    public class Bar {
        public init() {
            fatalError()
        }
    }
}
public extension main.typealiases {
    public typealias SmallInteger = Swift.Int16
    public typealias Bar = main.typealiases.inner.Bar
    public class Foo {
        public init() {
            fatalError()
        }
    }
}
public enum typealiases {
    public enum inner {
    }
}

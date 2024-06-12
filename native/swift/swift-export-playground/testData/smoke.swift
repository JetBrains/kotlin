import KotlinRuntime

public typealias CDE = Playground.C.D.E
public class A : KotlinRuntime.KotlinBase {
    public var boolProp: Swift.Bool {
        get {
            stub()
        }
    }
    public var floatProp: Swift.Float {
        get {
            stub()
        }
    }
    public var intProp: Swift.Int32 {
        get {
            stub()
        }
    }
    public var refProp: Playground.B {
        get {
            stub()
        }
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        stub()
    }
    public init(
        boolProp: Swift.Bool,
        intProp: Swift.Int32,
        floatProp: Swift.Float,
        refProp: Playground.B
    ) {
        stub()
    }
}
public class B : KotlinRuntime.KotlinBase {
    public override init() {
        stub()
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        stub()
    }
}
public class C : KotlinRuntime.KotlinBase {
    public class D : KotlinRuntime.KotlinBase {
        public class E : KotlinRuntime.KotlinBase {
            public override init() {
                stub()
            }
            public override init(
                __externalRCRef: Swift.UInt
            ) {
                stub()
            }
        }
        public override init() {
            stub()
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            stub()
        }
    }
    public override init() {
        stub()
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        stub()
    }
    public func method() -> Playground.C.D.E {
        stub()
    }
}
public func foo() -> Swift.Void {
    stub()
}
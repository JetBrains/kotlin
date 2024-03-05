declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        const forth: foo.Forth;
        interface ExportedInterface {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.ExportedInterface": unique symbol;
            };
        }
        function producer(value: number): foo.NonExportedType;
        function consumer(value: foo.NonExportedType): number;
        function childProducer(value: number): foo.NotExportedChildClass;
        function childConsumer(value: foo.NotExportedChildClass): number;
        function genericChildProducer<T extends foo.NonExportedGenericType<number>>(value: T): foo.NotExportedChildGenericClass<T>;
        function genericChildConsumer<T extends foo.NonExportedGenericType<number>>(value: foo.NotExportedChildGenericClass<T>): T;
        class A implements foo.NonExportedParent.NonExportedSecond.NonExportedUsedChild {
            constructor(value: foo.NonExportedType);
            get value(): foo.NonExportedType;
            set value(value: foo.NonExportedType);
            increment<T extends foo.NonExportedType>(t: T): foo.NonExportedType;
            getNonExportedUserChild(): foo.NonExportedParent.NonExportedSecond.NonExportedUsedChild;
            readonly __doNotUseOrImplementIt: foo.NonExportedParent.NonExportedSecond.NonExportedUsedChild["__doNotUseOrImplementIt"];
        }
        class B implements foo.NonExportedType {
            constructor(v: number);
            readonly __doNotUseOrImplementIt: foo.NonExportedType["__doNotUseOrImplementIt"];
        }
        class C implements foo.NonExportedInterface {
            constructor();
            readonly __doNotUseOrImplementIt: foo.NonExportedInterface["__doNotUseOrImplementIt"];
        }
        class D implements foo.NonExportedInterface, foo.ExportedInterface {
            constructor();
            readonly __doNotUseOrImplementIt: foo.NonExportedInterface["__doNotUseOrImplementIt"] & foo.ExportedInterface["__doNotUseOrImplementIt"];
        }
        class E implements foo.NonExportedType, foo.ExportedInterface {
            constructor();
            readonly __doNotUseOrImplementIt: foo.NonExportedType["__doNotUseOrImplementIt"] & foo.ExportedInterface["__doNotUseOrImplementIt"];
        }
        class F extends foo.A implements foo.NonExportedInterface {
            constructor();
            readonly __doNotUseOrImplementIt: foo.A["__doNotUseOrImplementIt"] & foo.NonExportedInterface["__doNotUseOrImplementIt"];
        }
        class G implements foo.NonExportedGenericInterface<foo.NonExportedType> {
            constructor();
            readonly __doNotUseOrImplementIt: foo.NonExportedGenericInterface<foo.NonExportedType>["__doNotUseOrImplementIt"];
        }
        class H implements foo.NonExportedGenericType<foo.NonExportedType> {
            constructor();
            readonly __doNotUseOrImplementIt: foo.NonExportedGenericType<foo.NonExportedType>["__doNotUseOrImplementIt"];
        }
        class I implements foo.NotExportedChildClass {
            constructor();
            readonly __doNotUseOrImplementIt: foo.NotExportedChildClass["__doNotUseOrImplementIt"];
        }
        class J implements foo.NotExportedChildGenericClass<foo.NonExportedType> {
            constructor();
            readonly __doNotUseOrImplementIt: foo.NotExportedChildGenericClass<foo.NonExportedType>["__doNotUseOrImplementIt"];
        }
        function baz(a: number): Promise<number>;
        function bar(): Error;
        function pep<T extends foo.NonExportedInterface & foo.NonExportedGenericInterface<number>>(x: T): void;
        const console: Console;
        const error: WebAssembly.CompileError;
        interface IA {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.IA": unique symbol;
            };
        }
        class Third extends /* foo.Second */ foo.First {
            constructor();
        }
        class Sixth extends /* foo.Fifth */ foo.Third implements foo.Forth, foo.IC {
            constructor();
            readonly __doNotUseOrImplementIt: foo.Forth["__doNotUseOrImplementIt"] & foo.IC["__doNotUseOrImplementIt"];
        }
        class First {
            constructor();
        }
        function acceptForthLike<T extends foo.Forth>(forth: T): void;
        function acceptMoreGenericForthLike<T extends foo.IB & foo.IC & foo.Third>(forth: T): void;
        interface Service<Self extends foo.Service<Self, TEvent>, TEvent extends foo.Event<Self>> {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.Service": unique symbol;
            };
        }
        interface Event<TService extends foo.Service<TService, any /*UnknownType **/>> {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.Event": unique symbol;
            };
        }
        class SomeServiceRequest implements foo.Service<any/* foo.SomeService */, foo.Event<any/* foo.SomeService */>/* foo.SomeEvent */> {
            constructor();
            readonly __doNotUseOrImplementIt: foo.Service<any/* foo.SomeService */, foo.Event<any/* foo.SomeService */>/* foo.SomeEvent */>["__doNotUseOrImplementIt"];
        }
        interface NonExportedParent {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.NonExportedParent": unique symbol;
            };
        }
        namespace NonExportedParent {
            interface NonExportedSecond {
                readonly __doNotUseOrImplementIt: {
                    readonly "foo.NonExportedParent.NonExportedSecond": unique symbol;
                };
            }
            namespace NonExportedSecond {
                interface NonExportedUsedChild {
                    readonly __doNotUseOrImplementIt: {
                        readonly "foo.NonExportedParent.NonExportedSecond.NonExportedUsedChild": unique symbol;
                    };
                }
            }
        }
        interface NonExportedInterface {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.NonExportedInterface": unique symbol;
            };
        }
        interface NonExportedGenericInterface<T> {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.NonExportedGenericInterface": unique symbol;
            };
        }
        interface NonExportedType {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.NonExportedType": unique symbol;
            };
        }
        interface NonExportedGenericType<T> {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.NonExportedGenericType": unique symbol;
            };
        }
        interface NotExportedChildClass extends foo.NonExportedInterface, foo.NonExportedType {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.NotExportedChildClass": unique symbol;
            } & foo.NonExportedInterface["__doNotUseOrImplementIt"] & foo.NonExportedType["__doNotUseOrImplementIt"];
        }
        interface NotExportedChildGenericClass<T> extends foo.NonExportedInterface, foo.NonExportedGenericInterface<T>, foo.NonExportedGenericType<T> {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.NotExportedChildGenericClass": unique symbol;
            } & foo.NonExportedInterface["__doNotUseOrImplementIt"] & foo.NonExportedGenericInterface<T>["__doNotUseOrImplementIt"] & foo.NonExportedGenericType<T>["__doNotUseOrImplementIt"];
        }
        interface IB extends foo.IA {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.IB": unique symbol;
            } & foo.IA["__doNotUseOrImplementIt"];
        }
        interface IC extends foo.IB {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.IC": unique symbol;
            } & foo.IB["__doNotUseOrImplementIt"];
        }
        interface Forth extends foo.Third, foo.IB, foo.IC {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.Forth": unique symbol;
            } & foo.IB["__doNotUseOrImplementIt"] & foo.IC["__doNotUseOrImplementIt"];
        }
    }
}

declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);


    namespace foo {
        const console: Console;
        const error: CompileError;
        const forth: any/* foo.Forth */;
        function producer(value: number): any/* foo.NonExportedType */;
        function consumer(value: any/* foo.NonExportedType */): number;
        function childProducer(value: number): any/* foo.NotExportedChildClass */;
        function childConsumer(value: any/* foo.NotExportedChildClass */): number;
        function genericChildProducer<T extends unknown/* foo.NonExportedGenericType<number> */>(value: T): any/* foo.NotExportedChildGenericClass<T> */;
        function genericChildConsumer<T extends unknown/* foo.NonExportedGenericType<number> */>(value: any/* foo.NotExportedChildGenericClass<T> */): T;
        function baz(a: number): Promise<number>;
        function bazVoid(a: number): Promise<void>;
        function bar(): Error;
        function pep<T extends unknown/* foo.NonExportedInterface */ & unknown/* foo.NonExportedGenericInterface<number> */>(x: T): void;
        function acceptForthLike<T extends unknown/* foo.Forth */>(forth: T): void;
        function acceptMoreGenericForthLike<T extends unknown/* foo.IB */ & unknown/* foo.IC */ & foo.Third>(forth: T): void;
        interface ExportedInterface {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.ExportedInterface": unique symbol;
            };
        }
        class A /* extends foo.NonExportedParent.NonExportedSecond.NonExportedUsedChild */ {
            constructor(value: any/* foo.NonExportedType */);
            increment<T extends unknown/* foo.NonExportedType */>(t: T): any/* foo.NonExportedType */;
            getNonExportedUserChild(): any/* foo.NonExportedParent.NonExportedSecond.NonExportedUsedChild */;
            get value(): any/* foo.NonExportedType */;
            set value(value: any/* foo.NonExportedType */);
        }
        namespace A {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => A;
            }
        }
        class B /* extends foo.NonExportedType */ {
            constructor(v: number);
        }
        namespace B {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => B;
            }
        }
        class C /* implements foo.NonExportedInterface */ {
            constructor();
        }
        namespace C {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => C;
            }
        }
        class D implements foo.ExportedInterface/*, foo.NonExportedInterface */ {
            constructor();
            readonly __doNotUseOrImplementIt: foo.ExportedInterface["__doNotUseOrImplementIt"];
        }
        namespace D {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => D;
            }
        }
        class E /* extends foo.NonExportedType */ implements foo.ExportedInterface {
            constructor();
            readonly __doNotUseOrImplementIt: foo.ExportedInterface["__doNotUseOrImplementIt"];
        }
        namespace E {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => E;
            }
        }
        class F extends foo.A.$metadata$.constructor /* implements foo.NonExportedInterface */ {
            constructor();
        }
        namespace F {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => F;
            }
        }
        class G /* implements foo.NonExportedGenericInterface<foo.NonExportedType> */ {
            constructor();
        }
        namespace G {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => G;
            }
        }
        class H /* extends foo.NonExportedGenericType<foo.NonExportedType> */ {
            constructor();
        }
        namespace H {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => H;
            }
        }
        class I /* extends foo.NotExportedChildClass */ {
            constructor();
        }
        namespace I {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => I;
            }
        }
        class J /* extends foo.NotExportedChildGenericClass<foo.NonExportedType> */ {
            constructor();
        }
        namespace J {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => J;
            }
        }
        interface IA {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.IA": unique symbol;
            };
        }
        class Third /* extends foo.Second */ {
            constructor();
        }
        namespace Third {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Third;
            }
        }
        class Sixth /* extends foo.Fifth */ /* implements foo.IC */ {
            constructor();
        }
        namespace Sixth {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Sixth;
            }
        }
        class First {
            constructor();
        }
        namespace First {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => First;
            }
        }
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
        class SomeServiceRequest implements foo.Service<any/* foo.SomeService */, any/* foo.SomeEvent */> {
            constructor();
            readonly __doNotUseOrImplementIt: foo.Service<any/* foo.SomeService */, any/* foo.SomeEvent */>["__doNotUseOrImplementIt"];
        }
        namespace SomeServiceRequest {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => SomeServiceRequest;
            }
        }
        class FinalClass {
            private constructor();
        }
        namespace FinalClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => FinalClass;
            }
        }
        abstract class SealedClass {
            private constructor();
        }
        namespace SealedClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => SealedClass;
            }
        }
    }
}

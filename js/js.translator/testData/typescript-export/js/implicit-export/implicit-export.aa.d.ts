declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);


    namespace foo {
        const console: Console;
        const error: CompileError;
        function producer(value: number): any/* foo.NonExportedType */;
        function consumer(value: any/* foo.NonExportedType */): number;
        function baz(a: number): Promise<number>;
        function bazVoid(a: number): Promise<void>;
        function bar(): Error;
        function functionWithTypeAliasInside(x: any/* foo.NonExportedGenericInterface<foo.NonExportedType> */): any/* foo.NonExportedGenericInterface<foo.NonExportedType> */;
        interface ExportedInterface {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.ExportedInterface": unique symbol;
            };
        }
        class A {
            constructor(value: any/* foo.NonExportedType */);
            increment<T extends unknown/* foo.NonExportedType */>(t: T): any/* foo.NonExportedType */;
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
        class TheNewException extends Error {
            constructor();
        }
        namespace TheNewException {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => TheNewException;
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
    }
}

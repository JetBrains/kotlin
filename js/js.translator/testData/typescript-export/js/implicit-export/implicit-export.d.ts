declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        interface ExportedInterface {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.ExportedInterface": unique symbol;
            };
        }
        function producer(value: number): any/* foo.NonExportedType */;
        function consumer(value: any/* foo.NonExportedType */): number;
        class A {
            constructor(value: any/* foo.NonExportedType */);
            get value(): any/* foo.NonExportedType */;
            set value(value: any/* foo.NonExportedType */);
            increment<T extends unknown/* foo.NonExportedType */>(t: T): any/* foo.NonExportedType */;
        }
        class B /* extends foo.NonExportedType */ {
            constructor(v: number);
        }
        class C /* implements foo.NonExportedInterface */ {
            constructor();
        }
        class D implements foo.ExportedInterface/*, foo.NonExportedInterface */ {
            constructor();
            readonly __doNotUseOrImplementIt: foo.ExportedInterface["__doNotUseOrImplementIt"];
        }
        class E /* extends foo.NonExportedType */ implements foo.ExportedInterface {
            constructor();
            readonly __doNotUseOrImplementIt: foo.ExportedInterface["__doNotUseOrImplementIt"];
        }
        class F extends foo.A /* implements foo.NonExportedInterface */ {
            constructor();
        }
        class G /* implements foo.NonExportedGenericInterface<foo.NonExportedType> */ {
            constructor();
        }
        class H /* extends foo.NonExportedGenericType<foo.NonExportedType> */ {
            constructor();
        }
        function baz(a: number): Promise<number>;
        function bar(): Error;
        const console: Console;
        const error: WebAssembly.CompileError;
        function functionWithTypeAliasInside(x: any/* foo.NonExportedGenericInterface<foo.NonExportedType> */): any/* foo.NonExportedGenericInterface<foo.NonExportedType> */;
        class TheNewException extends Error {
            constructor();
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
        class SomeServiceRequest implements foo.Service<any/* foo.SomeService */, foo.Event<any/* foo.SomeService */>/* foo.SomeEvent */> {
            constructor();
            readonly __doNotUseOrImplementIt: foo.Service<any/* foo.SomeService */, foo.Event<any/* foo.SomeService */>/* foo.SomeEvent */>["__doNotUseOrImplementIt"];
        }
    }
}

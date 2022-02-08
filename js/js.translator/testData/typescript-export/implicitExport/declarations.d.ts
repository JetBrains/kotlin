declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    const __doNotImplementIt: unique symbol
    type __doNotImplementIt = typeof __doNotImplementIt
    namespace foo {
        interface ExportedInterface {
            readonly __doNotUseIt: __doNotImplementIt;
        }
        function producer(value: number): any/* foo.NonExportedType */;
        function consumer(value: any/* foo.NonExportedType */): number;
        class A {
            constructor(value: any/* foo.NonExportedType */);
            get value(): any/* foo.NonExportedType */;
            set value(value: any/* foo.NonExportedType */);
            increment<T>(t: T): any/* foo.NonExportedType */;
        }
        class B /* extends foo.NonExportedType */ {
            constructor(v: number);
        }
        class C /* implements foo.NonExportedInterface */ {
            constructor();
        }
        class D implements foo.ExportedInterface/*, foo.NonExportedInterface */ {
            constructor();
            readonly __doNotUseIt: __doNotImplementIt;
        }
        class E /* extends foo.NonExportedType */ implements foo.ExportedInterface {
            constructor();
            readonly __doNotUseIt: __doNotImplementIt;
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
    }
}

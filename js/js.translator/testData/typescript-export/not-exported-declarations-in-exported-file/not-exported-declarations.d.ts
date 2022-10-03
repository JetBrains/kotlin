declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        interface ExportedInterface {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.ExportedInterface": unique symbol;
            };
        }
        class OnlyFooParamExported implements foo.ExportedInterface {
            constructor(foo: string);
            get foo(): string;
            readonly __doNotUseOrImplementIt: foo.ExportedInterface["__doNotUseOrImplementIt"];
        }
    }
}

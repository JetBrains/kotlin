declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        class Test {
            constructor();
            static bar(): string;
            static get foo(): string;
            static get baz(): string;
            static get mutable(): string;
            static set mutable(value: string);
        }
    }
}

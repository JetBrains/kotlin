declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    namespace foo {
        class Test {
            constructor();
            static bar(): string;
            static get foo(): string;
            static get baz(): string;
            static get mutable(): string;
            static set mutable(value: string);
        }
        namespace Test {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Test;
            }
        }
    }
}

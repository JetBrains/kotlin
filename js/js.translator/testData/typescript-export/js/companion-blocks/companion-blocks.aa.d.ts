declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        class MyClass {
            constructor();
            static bar(): string;
            static get foo(): string;
            static get mutable(): string;
            static set mutable(value: string);
            static get baz(): string;
        }
        namespace MyClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => MyClass;
            }
        }
    }
}

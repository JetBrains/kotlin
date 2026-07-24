declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        class WithInternal {
            constructor();
            static get publicVal(): string;
            static publicFun(): string;
        }
        namespace WithInternal {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => WithInternal;
            }
        }
    }
}

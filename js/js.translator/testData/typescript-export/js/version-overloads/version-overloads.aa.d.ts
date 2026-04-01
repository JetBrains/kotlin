declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    namespace foo {
        function foo2(a: number, B: string | undefined, b1: string | undefined, c: number | undefined, f: () => void): void;
        class X {
            constructor();
            foo(a: number, B?: string, b1?: string, c?: number): void;
        }
        namespace X {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => X;
            }
        }
    }
}

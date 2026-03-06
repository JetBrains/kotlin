declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    namespace foo {
        function foo2(a: number, B: string | undefined, b1: string | undefined, c: number | undefined, f: () => void): void;
        /* ErrorDeclaration: Class declarations are not implemented yet */
    }
}

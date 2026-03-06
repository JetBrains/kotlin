declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    namespace foo {
        const prop: number;
        function box(): string;
        function justSomeDefaultExport(): string;
        /* ErrorDeclaration: Class declarations are not implemented yet */
    }
}

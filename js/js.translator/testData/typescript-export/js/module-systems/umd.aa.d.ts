type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);

export declare namespace foo {
    /* ErrorDeclaration: Top level property declarations are not implemented yet */
    function box(): string;
    function justSomeDefaultExport(): string;
    /* ErrorDeclaration: Class declarations are not implemented yet */
}
export as namespace JS_TESTS;

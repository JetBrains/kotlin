declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    /* ErrorDeclaration: Top level function declarations are not implemented yet */
    /* ErrorDeclaration: Class declarations are not implemented yet */
    namespace a.b {
        /* ErrorDeclaration: Top level function declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
    }
    namespace foo.bar.baz {
        /* ErrorDeclaration: Top level function declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
    }
}

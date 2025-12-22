declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    namespace foo {
        function shortNameBasedDestructuring(): string;
        function fullNameBasedDestructuring(): string;
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
    }
}

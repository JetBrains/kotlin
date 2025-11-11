declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    /* ErrorDeclaration: Top level property declarations are not implemented yet */
    function publicFun(): number;
    /* ErrorDeclaration: Class declarations are not implemented yet */
    /* ErrorDeclaration: Class declarations are not implemented yet */
    /* ErrorDeclaration: Class declarations are not implemented yet */
    /* ErrorDeclaration: Class declarations are not implemented yet */
}

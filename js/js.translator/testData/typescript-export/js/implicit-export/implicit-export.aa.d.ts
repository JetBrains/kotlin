declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace kotlin {
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
    }

    namespace foo {
        /* ErrorDeclaration: Top level property declarations are not implemented yet */
        /* ErrorDeclaration: Top level property declarations are not implemented yet */
        function producer(value: number): any/* foo.NonExportedType */;
        function consumer(value: any/* foo.NonExportedType */): number;
        function baz(a: number): Promise<number>;
        function bazVoid(a: number): Promise<void>;
        function bar(): Error;
        function functionWithTypeAliasInside(x: any/* foo.NonExportedGenericInterface<foo.NonExportedType> */): any/* foo.NonExportedGenericInterface<foo.NonExportedType> */;
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
    }
}

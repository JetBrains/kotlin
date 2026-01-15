declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace kotlin {
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
    }

    namespace foo {
        function callingExportedParentMethod(foo: foo.IFoo<any /*UnknownType **/>): string;
        function justCallFoo(foo: foo.IFoo<any /*UnknownType **/>): string;
        function justCallAsyncFoo(foo: foo.IFoo<any /*UnknownType **/>): Promise<string>;
        function justCallParentAsyncMethod(foo: foo.IFoo<any /*UnknownType **/>): Promise<string>;
        function justCallSuspendWithDefaultImplementation(foo: foo.IFoo<any /*UnknownType **/>): Promise<string>;
        function callingWithDefaultsWithoutParameter(foo: foo.IFoo<any /*UnknownType **/>): string;
        function callingWithDefaultsAndDefaultImplementationWithParameter(foo: foo.IFoo<any /*UnknownType **/>): string;
        function callingWithDefaultsAndDefaultImplementationWithoutParameter(foo: foo.IFoo<any /*UnknownType **/>): string;
        function callingWithDefaultsWithParameter(foo: foo.IFoo<any /*UnknownType **/>): string;
        function callingWithBridge(foo: foo.IFoo<string>): string;
        function checkIsFooInterface(foo: any): boolean;
        function checkIsExportedParentInterface(foo: any): boolean;
        function callingWithDefaultImplementation(foo: foo.IFoo<any /*UnknownType **/>): string;
        function callingAnotherWithDefaultImplementation(foo: foo.IFoo<any /*UnknownType **/>): string;
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
    }
}

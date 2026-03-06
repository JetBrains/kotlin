declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    function f(x1: foo.bar.baz.C1, x2: a.b.C2, x3: C3): string;
    /* ErrorDeclaration: Class declarations are not implemented yet */
    namespace a.b {
        function f(x1: foo.bar.baz.C1, x2: a.b.C2, x3: C3): string;
        /* ErrorDeclaration: Class declarations are not implemented yet */
    }
    namespace foo.bar.baz {
        function f(x1: foo.bar.baz.C1, x2: a.b.C2, x3: C3): string;
        /* ErrorDeclaration: Class declarations are not implemented yet */
    }
}

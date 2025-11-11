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
        function createPair(): kotlin.Pair<number, string>;
        function createTriple(): kotlin.Triple<foo.Foo, Array<kotlin.Pair<number, string>>, string>;
        function acceptPair<K, V>(somePair: kotlin.Pair<K, V>): V;
        function acceptTriple<A, B, C>(someTriple: kotlin.Triple<A, B, C>): kotlin.Pair<A, C>;
        /* ErrorDeclaration: Class declarations are not implemented yet */
    }
}

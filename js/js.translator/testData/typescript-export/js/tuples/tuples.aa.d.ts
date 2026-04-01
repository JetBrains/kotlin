declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);


    namespace foo {
        const pair: kotlin.Pair<string, number>;
        const triple: kotlin.Triple<string, number, kotlin.Pair<string, number>>;
        function createPair(): kotlin.Pair<number, string>;
        function createTriple(): kotlin.Triple<foo.Foo, Array<kotlin.Pair<number, string>>, string>;
        function acceptPair<K, V>(somePair: kotlin.Pair<K, V>): V;
        function acceptTriple<A, B, C>(someTriple: kotlin.Triple<A, B, C>): kotlin.Pair<A, C>;
        class Foo {
            constructor();
        }
        namespace Foo {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Foo;
            }
        }
    }
}

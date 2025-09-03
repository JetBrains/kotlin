declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace kotlin {
        class Pair<A, B> /* implements kotlin.io.Serializable */ {
            constructor(first: A, second: B);
            get first(): A;
            get second(): B;
            toString(): string;
            copy(first?: A, second?: B): kotlin.Pair<A, B>;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace Pair {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <A, B>() => Pair<A, B>;
            }
        }
        class Triple<A, B, C> /* implements kotlin.io.Serializable */ {
            constructor(first: A, second: B, third: C);
            get first(): A;
            get second(): B;
            get third(): C;
            toString(): string;
            copy(first?: A, second?: B, third?: C): kotlin.Triple<A, B, C>;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace Triple {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <A, B, C>() => Triple<A, B, C>;
            }
        }
    }
    namespace foo {
        const pair: kotlin.Pair<string, number>;
        const triple: kotlin.Triple<string, number, kotlin.Pair<string, number>>;
        class Foo {
            constructor();
        }
        namespace Foo {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Foo;
            }
        }
        function createPair(): kotlin.Pair<number, string>;
        function createTriple(): kotlin.Triple<foo.Foo, Array<kotlin.Pair<number, string>>, string>;
        function acceptPair<K, V>(somePair: kotlin.Pair<K, V>): V;
        function acceptTriple<A, B, C>(someTriple: kotlin.Triple<A, B, C>): kotlin.Pair<A, C>;
    }
}
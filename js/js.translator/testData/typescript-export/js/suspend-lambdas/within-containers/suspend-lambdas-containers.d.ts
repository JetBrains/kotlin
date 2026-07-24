declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace kotlin.collections {
        interface KtList<out E> /* extends kotlin.collections.Collection<E> */ {
            asJsReadonlyArrayView(): ReadonlyArray<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtList": unique symbol;
            };
        }
        namespace KtList {
            function fromJsArray<E>(array: ReadonlyArray<E>): kotlin.collections.KtList<E>;
        }
        interface KtMap<K, out V> {
            asJsReadonlyMapView(): ReadonlyMap<K, V>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtMap": unique symbol;
            };
        }
        namespace KtMap {
            function fromJsMap<K, V>(map: ReadonlyMap<K, V>): kotlin.collections.KtMap<K, V>;
        }
    }
    namespace foo {
        class Box<T> {
            constructor(value: T);
            get value(): T;
        }
        namespace Box {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T>() => Box<T>;
            }
        }
        function produceBoxOfSuspendLambda(): foo.Box<() => Promise<string>>;
        function callBoxedSuspendLambda(box: foo.Box<() => Promise<string>>): Promise<string>;
        function produceListOfSuspendLambdas(): kotlin.collections.KtList<(p0: number) => Promise<number>>;
        function reduceListOfSuspendLambdas(lambdas: kotlin.collections.KtList<(p0: number) => Promise<number>>, start: number): Promise<number>;
        function produceMapOfSuspendLambdas(): kotlin.collections.KtMap<string, () => Promise<string>>;
        function callMapOfSuspendLambdas(lambdas: kotlin.collections.KtMap<string, () => Promise<string>>, key: string): Promise<Nullable<string>>;
    }
}

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
    function acceptList(x: kotlin.collections.KtList<any>): void;
    function returnList(): kotlin.collections.KtList<any>;
    function acceptMapStarKey(x: kotlin.collections.KtMap<any, string>): void;
    function acceptMapStarValue(x: kotlin.collections.KtMap<string, any>): void;
    function acceptMapStarAll(x: kotlin.collections.KtMap<any, any>): void;
    function acceptNestedStar(x: kotlin.collections.KtList<kotlin.collections.KtList<any>>): void;
    function acceptNullableStar(x: Nullable<kotlin.collections.KtList<any>>): void;
    function returnNullableStar(): Nullable<kotlin.collections.KtList<any>>;
    function acceptMapWithStarList(x: kotlin.collections.KtMap<string, kotlin.collections.KtList<any>>): void;
    function box(): string;
    class StarProjectionClass {
        constructor(items: kotlin.collections.KtList<any>);
        processEntries(x: kotlin.collections.KtMap<any, any>): kotlin.collections.KtList<any>;
        getNestedMap(): kotlin.collections.KtMap<string, kotlin.collections.KtList<any>>;
        get items(): kotlin.collections.KtList<any>;
        get mapping(): kotlin.collections.KtMap<any, any>;
    }
    namespace StarProjectionClass {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => StarProjectionClass;
        }
    }
}



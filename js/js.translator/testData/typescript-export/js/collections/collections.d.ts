declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace kotlin.collections {
        interface KtList<E> /* extends kotlin.collections.Collection<E> */ {
            asJsReadonlyArrayView(): ReadonlyArray<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtList": unique symbol;
            };
        }
        namespace KtList {
            function fromJsArray<E>(array: ReadonlyArray<E>): kotlin.collections.KtList<E>;
        }
        interface KtMutableList<E> extends kotlin.collections.KtList<E>/*, kotlin.collections.MutableCollection<E> */ {
            asJsArrayView(): Array<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtMutableList": unique symbol;
            } & kotlin.collections.KtList<E>["__doNotUseOrImplementIt"];
        }
        namespace KtMutableList {
            function fromJsArray<E>(array: ReadonlyArray<E>): kotlin.collections.KtMutableList<E>;
        }
        interface KtMap<K, V> {
            asJsReadonlyMapView(): ReadonlyMap<K, V>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtMap": unique symbol;
            };
        }
        namespace KtMap {
            function fromJsMap<K, V>(map: ReadonlyMap<K, V>): kotlin.collections.KtMap<K, V>;
        }
        interface KtSet<E> /* extends kotlin.collections.Collection<E> */ {
            asJsReadonlySetView(): ReadonlySet<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtSet": unique symbol;
            };
        }
        namespace KtSet {
            function fromJsSet<E>(set: ReadonlySet<E>): kotlin.collections.KtSet<E>;
        }
        interface KtMutableSet<E> extends kotlin.collections.KtSet<E>/*, kotlin.collections.MutableCollection<E> */ {
            asJsSetView(): Set<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtMutableSet": unique symbol;
            } & kotlin.collections.KtSet<E>["__doNotUseOrImplementIt"];
        }
        namespace KtMutableSet {
            function fromJsSet<E>(set: ReadonlySet<E>): kotlin.collections.KtMutableSet<E>;
        }
        interface KtMutableMap<K, V> extends kotlin.collections.KtMap<K, V> {
            asJsMapView(): Map<K, V>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtMutableMap": unique symbol;
            } & kotlin.collections.KtMap<K, V>["__doNotUseOrImplementIt"];
        }
        namespace KtMutableMap {
            function fromJsMap<K, V>(map: ReadonlyMap<K, V>): kotlin.collections.KtMutableMap<K, V>;
        }
    }
    function provideList(): kotlin.collections.KtList<number>;
    function provideMutableList(): kotlin.collections.KtMutableList<number>;
    function provideSet(): kotlin.collections.KtSet<number>;
    function provideMutableSet(): kotlin.collections.KtMutableSet<number>;
    function provideMap(): kotlin.collections.KtMap<string, number>;
    function provideMutableMap(): kotlin.collections.KtMutableMap<string, number>;
    function consumeList(list: kotlin.collections.KtList<number>): boolean;
    function consumeMutableList(list: kotlin.collections.KtMutableList<number>): boolean;
    function consumeSet(list: kotlin.collections.KtSet<number>): boolean;
    function consumeMutableSet(list: kotlin.collections.KtMutableSet<number>): boolean;
    function consumeMap(map: kotlin.collections.KtMap<string, number>): boolean;
    function consumeMutableMap(map: kotlin.collections.KtMutableMap<string, number>): boolean;
    function provideListAsync(): Promise<kotlin.collections.KtList<number>>;
}

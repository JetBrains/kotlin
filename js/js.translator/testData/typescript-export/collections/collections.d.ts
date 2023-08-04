declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace kotlin.collections {
        interface List<E> /* extends kotlin.collections.Collection<E> */ {
            asJsArrayView(): ReadonlyArray<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.List": unique symbol;
            };
        }
        interface MutableList<E> extends kotlin.collections.List<E>/*, kotlin.collections.MutableCollection<E> */ {
            asJsArrayView(): ReadonlyArray<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.MutableList": unique symbol;
            } & kotlin.collections.List<E>["__doNotUseOrImplementIt"];
        }
        interface Set<E> /* extends kotlin.collections.Collection<E> */ {
            asJsSetView(): ReadonlySet<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.Set": unique symbol;
            };
        }
        interface MutableSet<E> extends kotlin.collections.Set<E>/*, kotlin.collections.MutableCollection<E> */ {
            asJsSetView(): ReadonlySet<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.MutableSet": unique symbol;
            } & kotlin.collections.Set<E>["__doNotUseOrImplementIt"];
        }
        interface Map<K, V> {
            asJsMapView(): ReadonlyMap<K, V>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.Map": unique symbol;
            };
        }
        interface MutableMap<K, V> extends kotlin.collections.Map<K, V> {
            asJsMapView(): ReadonlyMap<K, V>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.MutableMap": unique symbol;
            } & kotlin.collections.Map<K, V>["__doNotUseOrImplementIt"];
        }
    }
    function provideList(): kotlin.collections.List<number>;
    function provideMutableList(): kotlin.collections.MutableList<number>;
    function provideSet(): kotlin.collections.Set<number>;
    function provideMutableSet(): kotlin.collections.MutableSet<number>;
    function provideMap(): kotlin.collections.Map<string, number>;
    function provideMutableMap(): kotlin.collections.MutableMap<string, number>;
    function consumeList(list: kotlin.collections.List<number>): boolean;
    function consumeMutableList(list: kotlin.collections.MutableList<number>): boolean;
    function consumeSet(list: kotlin.collections.Set<number>): boolean;
    function consumeMutableSet(list: kotlin.collections.MutableSet<number>): boolean;
    function consumeMap(map: kotlin.collections.Map<string, number>): boolean;
    function consumeMutableMap(map: kotlin.collections.MutableMap<string, number>): boolean;
}

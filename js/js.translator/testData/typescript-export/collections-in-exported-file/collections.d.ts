declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace kotlin.collections {
        interface KotlinList<E> /* extends kotlin.collections.Collection<E> */ {
            asJsArrayView(): ReadonlyArray<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KotlinList": unique symbol;
            };
        }
        interface KotlinMap<K, V> {
            asJsMapView(): ReadonlyMap<K, V>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KotlinMap": unique symbol;
            };
        }
        interface KotlinMutableList<E> extends kotlin.collections.KotlinList<E>/*, kotlin.collections.MutableCollection<E> */ {
            asJsArrayMutableView(): Array<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KotlinMutableList": unique symbol;
            } & kotlin.collections.KotlinList<E>["__doNotUseOrImplementIt"];
        }
        interface KotlinSet<E> /* extends kotlin.collections.Collection<E> */ {
            asJsSetView(): ReadonlySet<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KotlinSet": unique symbol;
            };
        }
        interface KotlinMutableSet<E> extends kotlin.collections.KotlinSet<E>/*, kotlin.collections.MutableCollection<E> */ {
            asJsSetMutableView(): Set<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KotlinMutableSet": unique symbol;
            } & kotlin.collections.KotlinSet<E>["__doNotUseOrImplementIt"];
        }
        interface KotlinMutableMap<K, V> extends kotlin.collections.KotlinMap<K, V> {
            asJsMapMutableView(): Map<K, V>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KotlinMutableMap": unique symbol;
            } & kotlin.collections.KotlinMap<K, V>["__doNotUseOrImplementIt"];
        }
    }
    function provideList(): kotlin.collections.KotlinList<number>;
    function provideMutableList(): kotlin.collections.KotlinMutableList<number>;
    function provideSet(): kotlin.collections.KotlinSet<number>;
    function provideMutableSet(): kotlin.collections.KotlinMutableSet<number>;
    function provideMap(): kotlin.collections.KotlinMap<string, number>;
    function provideMutableMap(): kotlin.collections.KotlinMutableMap<string, number>;
    function consumeList(list: kotlin.collections.KotlinList<number>): boolean;
    function consumeMutableList(list: kotlin.collections.KotlinMutableList<number>): boolean;
    function consumeSet(list: kotlin.collections.KotlinSet<number>): boolean;
    function consumeMutableSet(list: kotlin.collections.KotlinMutableSet<number>): boolean;
    function consumeMap(map: kotlin.collections.KotlinMap<string, number>): boolean;
    function consumeMutableMap(map: kotlin.collections.KotlinMutableMap<string, number>): boolean;
}

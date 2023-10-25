declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace kotlin.collections {
        interface KtList<E> /* extends kotlin.collections.Collection<E> */ {
            asJsArrayView(): ReadonlyArray<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtList": unique symbol;
            };
        }
        namespace KtList {
            abstract class Factory extends _objects_.kotlin$collections$KtList$Factory {
                private constructor();
            }
        }
        interface KtMutableList<E> extends kotlin.collections.KtList<E>/*, kotlin.collections.MutableCollection<E> */ {
            asJsArrayMutableView(): Array<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtMutableList": unique symbol;
            } & kotlin.collections.KtList<E>["__doNotUseOrImplementIt"];
        }
        namespace KtMutableList {
            abstract class Factory extends _objects_.kotlin$collections$KtMutableList$Factory {
                private constructor();
            }
        }
        interface KtMap<K, V> {
            asJsMapView(): ReadonlyMap<K, V>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtMap": unique symbol;
            };
        }
        namespace KtMap {
            abstract class Factory extends _objects_.kotlin$collections$KtMap$Factory {
                private constructor();
            }
        }
        interface KtSet<E> /* extends kotlin.collections.Collection<E> */ {
            asJsSetView(): ReadonlySet<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtSet": unique symbol;
            };
        }
        namespace KtSet {
            abstract class Factory extends _objects_.kotlin$collections$KtSet$Factory {
                private constructor();
            }
        }
        interface KtMutableSet<E> extends kotlin.collections.KtSet<E>/*, kotlin.collections.MutableCollection<E> */ {
            asJsSetMutableView(): Set<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtMutableSet": unique symbol;
            } & kotlin.collections.KtSet<E>["__doNotUseOrImplementIt"];
        }
        namespace KtMutableSet {
            abstract class Factory extends _objects_.kotlin$collections$KtMutableSet$Factory {
                private constructor();
            }
        }
        interface KtMutableMap<K, V> extends kotlin.collections.KtMap<K, V> {
            asJsMapMutableView(): Map<K, V>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtMutableMap": unique symbol;
            } & kotlin.collections.KtMap<K, V>["__doNotUseOrImplementIt"];
        }
        namespace KtMutableMap {
            abstract class Factory extends _objects_.kotlin$collections$KtMutableMap$Factory {
                private constructor();
            }
        }
    }
    namespace _objects_ {
        const kotlin$collections$KtList$Factory: {
            fromJsArray<E>(array: ReadonlyArray<E>): kotlin.collections.KtList<E>;
        } & {
            new(): any;
        };
        const kotlin$collections$KtMutableList$Factory: {
            fromJsArray<E>(array: ReadonlyArray<E>): kotlin.collections.KtMutableList<E>;
        } & {
            new(): any;
        };
        const kotlin$collections$KtMap$Factory: {
            fromJsMap<K, V>(map: ReadonlyMap<K, V>): kotlin.collections.KtMap<K, V>;
        } & {
            new(): any;
        };
        const kotlin$collections$KtSet$Factory: {
            fromJsSet<E>(set: ReadonlySet<E>): kotlin.collections.KtSet<E>;
        } & {
            new(): any;
        };
        const kotlin$collections$KtMutableSet$Factory: {
            fromJsSet<E>(set: ReadonlySet<E>): kotlin.collections.KtMutableSet<E>;
        } & {
            new(): any;
        };
        const kotlin$collections$KtMutableMap$Factory: {
            fromJsMap<K, V>(map: ReadonlyMap<K, V>): kotlin.collections.KtMutableMap<K, V>;
        } & {
            new(): any;
        };
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
}

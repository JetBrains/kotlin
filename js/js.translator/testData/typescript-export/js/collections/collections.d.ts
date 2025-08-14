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
        abstract class KtList<E> extends KtSingleton<KtList.$metadata$.constructor>() {
            private constructor();
        }
        namespace KtList {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor {
                    fromJsArray<E>(array: ReadonlyArray<E>): kotlin.collections.KtList<E>;
                    private constructor();
                }
            }
        }
        interface KtMap<K, V> {
            asJsReadonlyMapView(): ReadonlyMap<K, V>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtMap": unique symbol;
            };
        }
        abstract class KtMap<K, V> extends KtSingleton<KtMap.$metadata$.constructor>() {
            private constructor();
        }
        namespace KtMap {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor {
                    fromJsMap<K, V>(map: ReadonlyMap<K, V>): kotlin.collections.KtMap<K, V>;
                    private constructor();
                }
            }
        }
        interface KtMutableList<E> extends kotlin.collections.KtList<E>/*, kotlin.collections.MutableCollection<E> */ {
            asJsArrayView(): Array<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtMutableList": unique symbol;
            } & kotlin.collections.KtList<E>["__doNotUseOrImplementIt"];
        }
        abstract class KtMutableList<E> extends KtSingleton<KtMutableList.$metadata$.constructor>() {
            private constructor();
        }
        namespace KtMutableList {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor {
                    fromJsArray<E>(array: ReadonlyArray<E>): kotlin.collections.KtMutableList<E>;
                    private constructor();
                }
            }
        }
        interface KtSet<E> /* extends kotlin.collections.Collection<E> */ {
            asJsReadonlySetView(): ReadonlySet<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtSet": unique symbol;
            };
        }
        abstract class KtSet<E> extends KtSingleton<KtSet.$metadata$.constructor>() {
            private constructor();
        }
        namespace KtSet {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor {
                    fromJsSet<E>(set: ReadonlySet<E>): kotlin.collections.KtSet<E>;
                    private constructor();
                }
            }
        }
        interface KtMutableSet<E> extends kotlin.collections.KtSet<E>/*, kotlin.collections.MutableCollection<E> */ {
            asJsSetView(): Set<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtMutableSet": unique symbol;
            } & kotlin.collections.KtSet<E>["__doNotUseOrImplementIt"];
        }
        abstract class KtMutableSet<E> extends KtSingleton<KtMutableSet.$metadata$.constructor>() {
            private constructor();
        }
        namespace KtMutableSet {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor {
                    fromJsSet<E>(set: ReadonlySet<E>): kotlin.collections.KtMutableSet<E>;
                    private constructor();
                }
            }
        }
        interface KtMutableMap<K, V> extends kotlin.collections.KtMap<K, V> {
            asJsMapView(): Map<K, V>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtMutableMap": unique symbol;
            } & kotlin.collections.KtMap<K, V>["__doNotUseOrImplementIt"];
        }
        abstract class KtMutableMap<K, V> extends KtSingleton<KtMutableMap.$metadata$.constructor>() {
            private constructor();
        }
        namespace KtMutableMap {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor {
                    fromJsMap<K, V>(map: ReadonlyMap<K, V>): kotlin.collections.KtMutableMap<K, V>;
                    private constructor();
                }
            }
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

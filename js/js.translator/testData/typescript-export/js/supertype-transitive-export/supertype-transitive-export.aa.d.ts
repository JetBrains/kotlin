declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace kotlin.collections {
        interface KtMutableSet<E> extends kotlin.collections.KtSet<E>/*, kotlin.collections.MutableCollection<E> */ {
            asJsSetView(): Set<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtMutableSet": unique symbol;
            } & kotlin.collections.KtSet<any>["__doNotUseOrImplementIt"];
        }
        namespace KtMutableSet {
            function fromJsSet<E>(set: ReadonlySet<E>): kotlin.collections.KtMutableSet<E>;
        }
        interface KtMutableList<E> extends kotlin.collections.KtList<E>/*, kotlin.collections.MutableCollection<E> */ {
            asJsArrayView(): Array<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtMutableList": unique symbol;
            } & kotlin.collections.KtList<any>["__doNotUseOrImplementIt"];
        }
        namespace KtMutableList {
            function fromJsArray<E>(array: ReadonlyArray<E>): kotlin.collections.KtMutableList<E>;
        }
        interface KtSet<out E> /* extends kotlin.collections.Collection<E> */ {
            asJsReadonlySetView(): ReadonlySet<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtSet": unique symbol;
            };
        }
        namespace KtSet {
            function fromJsSet<E>(set: ReadonlySet<E>): kotlin.collections.KtSet<E>;
        }
        interface KtList<out E> /* extends kotlin.collections.Collection<E> */ {
            asJsReadonlyArrayView(): ReadonlyArray<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtList": unique symbol;
            };
        }
        namespace KtList {
            function fromJsArray<E>(array: ReadonlyArray<E>): kotlin.collections.KtList<E>;
        }
    }
    const exportedProperty: kotlin.collections.KtMutableSet<string>/* NonExportedSet */;
    function foo(ml: kotlin.collections.KtMutableList<number>): void;
}

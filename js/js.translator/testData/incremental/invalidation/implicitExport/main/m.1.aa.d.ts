type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare namespace kotlin.collections {
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
export declare function consumeCollection(list: kotlin.collections.KtList<string>): Nullable<string>;
export declare function box(step: number): string;

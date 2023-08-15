type Nullable<T> = T | null | undefined
export declare interface KtList<E> /* extends Collection<E> */ {
    asJsArrayView(): ReadonlyArray<E>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KtList": unique symbol;
    };
}
export declare interface KtSet<E> /* extends Collection<E> */ {
    asJsSetView(): ReadonlySet<E>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KtSet": unique symbol;
    };
}
export declare interface KtMap<K, V> {
    asJsMapView(): ReadonlyMap<K, V>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KtMap": unique symbol;
    };
}
export declare interface KtMutableSet<E> extends KtSet<E>/*, MutableCollection<E> */ {
    asJsSetMutableView(): Set<E>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KtMutableSet": unique symbol;
    } & KtSet<E>["__doNotUseOrImplementIt"];
}
export declare interface KtMutableList<E> extends KtList<E>/*, MutableCollection<E> */ {
    asJsArrayMutableView(): Array<E>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KtMutableList": unique symbol;
    } & KtList<E>["__doNotUseOrImplementIt"];
}
export declare interface KtMutableMap<K, V> extends KtMap<K, V> {
    asJsMapMutableView(): Map<K, V>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KtMutableMap": unique symbol;
    } & KtMap<K, V>["__doNotUseOrImplementIt"];
}
export declare class MyClass {
    constructor(stepId: number);
    get stepId(): number;
    qux(): number;
}
export declare function box(stepId: number): string;
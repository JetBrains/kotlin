type Nullable<T> = T | null | undefined
export declare interface KotlinList<E> /* extends Collection<E> */ {
    asJsArrayView(): ReadonlyArray<E>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KotlinList": unique symbol;
    };
}
export declare interface KotlinSet<E> /* extends Collection<E> */ {
    asJsSetView(): ReadonlySet<E>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KotlinSet": unique symbol;
    };
}
export declare interface KotlinMap<K, V> {
    asJsMapView(): ReadonlyMap<K, V>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KotlinMap": unique symbol;
    };
}
export declare interface KotlinMutableSet<E> extends KotlinSet<E>/*, MutableCollection<E> */ {
    asJsSetMutableView(): Set<E>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KotlinMutableSet": unique symbol;
    } & KotlinSet<E>["__doNotUseOrImplementIt"];
}
export declare interface KotlinMutableList<E> extends KotlinList<E>/*, MutableCollection<E> */ {
    asJsArrayMutableView(): Array<E>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KotlinMutableList": unique symbol;
    } & KotlinList<E>["__doNotUseOrImplementIt"];
}
export declare interface KotlinMutableMap<K, V> extends KotlinMap<K, V> {
    asJsMapMutableView(): Map<K, V>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KotlinMutableMap": unique symbol;
    } & KotlinMap<K, V>["__doNotUseOrImplementIt"];
}
export declare function bar(): number;
export declare function box(stepId: number): string;
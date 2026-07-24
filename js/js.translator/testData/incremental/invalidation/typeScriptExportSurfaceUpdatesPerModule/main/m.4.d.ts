type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare interface KtSet<out E> /* extends Collection<E> */ {
    asJsReadonlySetView(): ReadonlySet<E>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KtSet": unique symbol;
    };
}
export declare namespace KtSet {
    function fromJsSet<E>(set: ReadonlySet<E>): KtSet<E>;
}
export declare interface KtList<out E> /* extends Collection<E> */ {
    asJsReadonlyArrayView(): ReadonlyArray<E>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KtList": unique symbol;
    };
}
export declare namespace KtList {
    function fromJsArray<E>(array: ReadonlyArray<E>): KtList<E>;
}
export declare interface KtMap<K, out V> {
    asJsReadonlyMapView(): ReadonlyMap<K, V>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KtMap": unique symbol;
    };
}
export declare namespace KtMap {
    function fromJsMap<K, V>(map: ReadonlyMap<K, V>): KtMap<K, V>;
}
export declare interface KtMutableSet<E> extends KtSet<E>/*, MutableCollection<E> */ {
    asJsSetView(): Set<E>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KtMutableSet": unique symbol;
    } & KtSet<any>["__doNotUseOrImplementIt"];
}
export declare namespace KtMutableSet {
    function fromJsSet<E>(set: ReadonlySet<E>): KtMutableSet<E>;
}
export declare interface KtMutableList<E> extends KtList<E>/*, MutableCollection<E> */ {
    asJsArrayView(): Array<E>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KtMutableList": unique symbol;
    } & KtList<any>["__doNotUseOrImplementIt"];
}
export declare namespace KtMutableList {
    function fromJsArray<E>(array: ReadonlyArray<E>): KtMutableList<E>;
}
export declare interface KtMutableMap<K, V> extends KtMap<K, V> {
    asJsMapView(): Map<K, V>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KtMutableMap": unique symbol;
    } & KtMap<any, any>["__doNotUseOrImplementIt"];
}
export declare namespace KtMutableMap {
    function fromJsMap<K, V>(map: ReadonlyMap<K, V>): KtMutableMap<K, V>;
}
export declare class Pair<out A, out B> /* implements Serializable */ {
    constructor(first: A, second: B);
    get first(): A;
    get second(): B;
    toString(): string;
    copy(first?: A, second?: B): Pair<A, B>;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace Pair {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new <A, B>() => Pair<A, B>;
    }
}
export declare function box(): string;

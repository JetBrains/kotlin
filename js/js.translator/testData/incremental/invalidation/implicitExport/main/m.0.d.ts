type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare namespace kotlin.collections {
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
    interface KtMap<K, out V> {
        asJsReadonlyMapView(): ReadonlyMap<K, V>;
        readonly __doNotUseOrImplementIt: {
            readonly "kotlin.collections.KtMap": unique symbol;
        };
    }
    namespace KtMap {
        function fromJsMap<K, V>(map: ReadonlyMap<K, V>): kotlin.collections.KtMap<K, V>;
    }
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
    interface KtMutableMap<K, V> extends kotlin.collections.KtMap<K, V> {
        asJsMapView(): Map<K, V>;
        readonly __doNotUseOrImplementIt: {
            readonly "kotlin.collections.KtMutableMap": unique symbol;
        } & kotlin.collections.KtMap<any, any>["__doNotUseOrImplementIt"];
    }
    namespace KtMutableMap {
        function fromJsMap<K, V>(map: ReadonlyMap<K, V>): kotlin.collections.KtMutableMap<K, V>;
    }
}
export declare namespace kotlin {
    class Pair<out A, out B> /* implements kotlin.io.Serializable */ {
        constructor(first: A, second: B);
        get first(): A;
        get second(): B;
        toString(): string;
        copy(first?: A, second?: B): kotlin.Pair<A, B>;
        hashCode(): number;
        equals(other: Nullable<any>): boolean;
    }
    namespace Pair {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new <A, B>() => Pair<A, B>;
        }
    }
}
export declare function consumeCollection(map: kotlin.collections.KtMap<string, string>): Nullable<string>;
export declare function box(step: number): string;

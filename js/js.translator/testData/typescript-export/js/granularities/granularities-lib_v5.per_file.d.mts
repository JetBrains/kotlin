// FILE: granularities-kotlin-kotlin-stdlib/kotlin/collections/Collections.export_v5.d.mts
type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare interface KtList<out E> /* extends Collection<E> */ {
    asJsReadonlyArrayView(): ReadonlyArray<E>;
    readonly __doNotUseOrImplementIt: {
        readonly "kotlin.collections.KtList": unique symbol;
    };
}
export declare namespace KtList {
    function fromJsArray<E>(array: ReadonlyArray<E>): KtList<E>;
}

// FILE: granularities-kotlin_lib1/lib1a.export_v5.d.mts
type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function foo(): KtList<number>;

// FILE: granularities-kotlin_lib1/lib1b.export_v5.d.mts
type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function bar(): KtList<number>;

// FILE: granularities-kotlin_lib2/lib2.export_v5.d.mts
type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function baz(): KtList<number>;

// FILE: granularities-kotlin_main/main.export_v5.d.mts
type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function box(): string;


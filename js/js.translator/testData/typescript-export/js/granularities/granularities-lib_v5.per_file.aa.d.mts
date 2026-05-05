// FILE: kotlin-kotlin-stdlib/kotlin/collections/Collections.export.d.mts
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

// FILE: kotlin_lib1/lib1a.export.d.mts
type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function foo(): KtList<number>;

// FILE: kotlin_lib1/lib1b.export.d.mts
type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function bar(): KtList<number>;

// FILE: kotlin_lib2/lib2.export.d.mts
type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function baz(): KtList<number>;

// FILE: kotlin_main/main.export.d.mts
type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function box(): string;


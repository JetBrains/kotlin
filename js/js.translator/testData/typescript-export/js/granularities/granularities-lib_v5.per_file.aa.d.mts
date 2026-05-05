// FILE: kotlin_lib1/lib1a.export.d.mts
type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function foo(): any/* KtList<number> */;

// FILE: kotlin_lib1/lib1b.export.d.mts
type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function bar(): any/* KtList<number> */;

// FILE: kotlin_lib2/lib2.export.d.mts
type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function baz(): any/* KtList<number> */;

// FILE: kotlin_main/main.export.d.mts
type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function box(): string;


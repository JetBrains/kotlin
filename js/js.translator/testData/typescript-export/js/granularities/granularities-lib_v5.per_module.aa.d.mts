// FILE: kotlin_lib1.d.mts
type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function foo(): any/* KtList<number> */;
export declare function bar(): any/* KtList<number> */;

// FILE: kotlin_lib2.d.mts
type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function baz(): any/* KtList<number> */;

// FILE: kotlin_main.d.mts
type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function box(): string;


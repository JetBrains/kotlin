// FILE: kotlin_lib1.d.mts
type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function foo(): any/* List<number> */;
export declare function bar(): any/* List<number> */;

// FILE: kotlin_lib2.d.mts
type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function baz(): any/* List<number> */;

// FILE: kotlin_main.d.mts
type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function box(): string;


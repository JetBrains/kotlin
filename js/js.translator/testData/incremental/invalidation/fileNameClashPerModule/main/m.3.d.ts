type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function test1(): string;
export declare function test2(): string;
export declare function box(): string;
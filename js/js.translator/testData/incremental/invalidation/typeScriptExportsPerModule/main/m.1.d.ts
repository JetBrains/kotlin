type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function foo(): number;
export declare function box(stepId: number, isWasm: boolean): string;
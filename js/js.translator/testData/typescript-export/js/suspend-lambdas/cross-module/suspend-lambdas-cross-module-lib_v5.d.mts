type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare const libSuspendLambda: {
    get(): (p0: number) => Promise<number>;
};
export declare function produceLibSuspendLambda(): (p0: number) => Promise<number>;
export declare const mainSuspendLambda: {
    get(): () => Promise<string>;
};
export declare function reuseLibSuspendLambda(): (p0: number) => Promise<number>;
export declare function callMainLambda(callback: () => Promise<string>): Promise<string>;
export declare function callIntLambda(callback: (p0: number) => Promise<number>, x: number): Promise<number>;
export declare function box(): string;

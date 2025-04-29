type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare class MyClass {
    constructor(stepId: number);
    get stepId(): number;
    baz(): number;
}
/** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
export declare namespace MyClass.$metadata$ {
    const constructor: abstract new () => MyClass;
}
export declare function box(stepId: number, isWasm: boolean): string;
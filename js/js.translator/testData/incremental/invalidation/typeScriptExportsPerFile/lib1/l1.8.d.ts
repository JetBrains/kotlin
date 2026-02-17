type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare function foo(): number;
export declare class RenamedExportedClass {
    constructor(value: number);
    get value(): number;
    getValue(): number;
}
export declare namespace RenamedExportedClass {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => RenamedExportedClass;
    }
}
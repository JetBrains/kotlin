import type { ExportedClass } from "../lib1/l1.export.mjs";

type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare class MyClass {
    constructor(stepId: number);
    get stepId(): number;
    bar(): number;
}
export declare namespace MyClass {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => MyClass;
    }
}
export declare function useExportedClass(ec: Nullable<ExportedClass>): Nullable<number>;
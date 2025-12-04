type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);

export declare namespace foo {
    const prop: number;
    function box(): string;
    function justSomeDefaultExport(): string;
    class C {
        constructor(x: number);
        doubleX(): number;
        get x(): number;
    }
    namespace C {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => C;
        }
    }
}
export as namespace JS_TESTS;

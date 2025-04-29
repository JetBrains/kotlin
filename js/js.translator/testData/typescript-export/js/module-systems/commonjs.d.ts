type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare namespace foo {
    const prop: number;
    class C {
        constructor(x: number);
        get x(): number;
        doubleX(): number;
    }
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace C.$metadata$ {
        const constructor: abstract new () => C;
    }
    function box(): string;
}
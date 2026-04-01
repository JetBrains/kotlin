type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);


export declare namespace foo {
    const prop: number;
    function box(): string;
    function asyncList(): Promise<any/* kotlin.collections.List<number> */>;
    function arrayOfLists(): Array<any/* kotlin.collections.List<number> */>;
    function acceptArrayOfPairs(array: Array<kotlin.Pair<string, string>>): void;
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

type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare namespace kotlin {
    /* ErrorDeclaration: Class declarations are not implemented yet */
    /* ErrorDeclaration: Class declarations are not implemented yet */
}

export declare namespace foo {
    const prop: number;
    function box(): string;
    function asyncList(): Promise<any/* kotlin.collections.List<number> */>;
    function arrayOfLists(): Array<any/* kotlin.collections.List<number> */>;
    function acceptArrayOfPairs(array: Array<kotlin.Pair<string, string>>): void;
    function justSomeDefaultExport(): string;
    /* ErrorDeclaration: Class declarations are not implemented yet */
}

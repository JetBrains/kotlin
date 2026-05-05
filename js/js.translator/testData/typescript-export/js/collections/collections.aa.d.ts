declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    function provideList(): any/* kotlin.collections.KtList<number> */;
    function provideMutableList(): any/* kotlin.collections.KtMutableList<number> */;
    function provideSet(): any/* kotlin.collections.KtSet<number> */;
    function provideMutableSet(): any/* kotlin.collections.KtMutableSet<number> */;
    function provideMap(): any/* kotlin.collections.KtMap<string, number> */;
    function provideMutableMap(): any/* kotlin.collections.KtMutableMap<string, number> */;
    function consumeList(list: any/* kotlin.collections.KtList<number> */): boolean;
    function consumeMutableList(list: any/* kotlin.collections.KtMutableList<number> */): boolean;
    function consumeSet(list: any/* kotlin.collections.KtSet<number> */): boolean;
    function consumeMutableSet(list: any/* kotlin.collections.KtMutableSet<number> */): boolean;
    function consumeMap(map: any/* kotlin.collections.KtMap<string, number> */): boolean;
    function consumeMutableMap(map: any/* kotlin.collections.KtMutableMap<string, number> */): boolean;
    function provideListAsync(): Promise<any/* kotlin.collections.KtList<number> */>;
}



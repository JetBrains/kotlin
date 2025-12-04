declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);


    function provideList(): any/* kotlin.collections.List<number> */;
    function provideMutableList(): any/* kotlin.collections.MutableList<number> */;
    function provideSet(): any/* kotlin.collections.Set<number> */;
    function provideMutableSet(): any/* kotlin.collections.MutableSet<number> */;
    function provideMap(): any/* kotlin.collections.Map<string, number> */;
    function provideMutableMap(): any/* kotlin.collections.MutableMap<string, number> */;
    function consumeList(list: any/* kotlin.collections.List<number> */): boolean;
    function consumeMutableList(list: any/* kotlin.collections.MutableList<number> */): boolean;
    function consumeSet(list: any/* kotlin.collections.Set<number> */): boolean;
    function consumeMutableSet(list: any/* kotlin.collections.MutableSet<number> */): boolean;
    function consumeMap(map: any/* kotlin.collections.Map<string, number> */): boolean;
    function consumeMutableMap(map: any/* kotlin.collections.MutableMap<string, number> */): boolean;
    function provideListAsync(): Promise<any/* kotlin.collections.List<number> */>;
}

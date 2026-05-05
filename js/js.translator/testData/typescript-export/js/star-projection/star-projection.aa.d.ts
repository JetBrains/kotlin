declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    function acceptList(x: any/* kotlin.collections.KtList<any> */): void;
    function returnList(): any/* kotlin.collections.KtList<any> */;
    function acceptMapStarKey(x: any/* kotlin.collections.KtMap<any, string> */): void;
    function acceptMapStarValue(x: any/* kotlin.collections.KtMap<string, any> */): void;
    function acceptMapStarAll(x: any/* kotlin.collections.KtMap<any, any> */): void;
    function acceptNestedStar(x: any/* kotlin.collections.KtList<kotlin.collections.KtList<any>> */): void;
    function acceptNullableStar(x: Nullable<any>/* Nullable<kotlin.collections.KtList<any>> */): void;
    function returnNullableStar(): Nullable<any>/* Nullable<kotlin.collections.KtList<any>> */;
    function acceptMapWithStarList(x: any/* kotlin.collections.KtMap<string, kotlin.collections.KtList<any>> */): void;
    function box(): string;
    class StarProjectionClass {
        constructor(items: any/* kotlin.collections.KtList<any> */);
        processEntries(x: any/* kotlin.collections.KtMap<any, any> */): any/* kotlin.collections.KtList<any> */;
        getNestedMap(): any/* kotlin.collections.KtMap<string, kotlin.collections.KtList<any>> */;
        get items(): any/* kotlin.collections.KtList<any> */;
        get mapping(): any/* kotlin.collections.KtMap<any, any> */;
    }
    namespace StarProjectionClass {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => StarProjectionClass;
        }
    }
}



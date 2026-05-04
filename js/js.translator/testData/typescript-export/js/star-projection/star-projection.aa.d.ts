declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    function acceptList(x: any/* kotlin.collections.List<any> */): void;
    function returnList(): any/* kotlin.collections.List<any> */;
    function acceptMapStarKey(x: any/* kotlin.collections.Map<any, string> */): void;
    function acceptMapStarValue(x: any/* kotlin.collections.Map<string, any> */): void;
    function acceptMapStarAll(x: any/* kotlin.collections.Map<any, any> */): void;
    function acceptNestedStar(x: any/* kotlin.collections.List<kotlin.collections.List<any>> */): void;
    function acceptNullableStar(x: Nullable<any>/* Nullable<kotlin.collections.List<any>> */): void;
    function returnNullableStar(): Nullable<any>/* Nullable<kotlin.collections.List<any>> */;
    function acceptMapWithStarList(x: any/* kotlin.collections.Map<string, kotlin.collections.List<any>> */): void;
    function box(): string;
    class StarProjectionClass {
        constructor(items: any/* kotlin.collections.List<any> */);
        processEntries(x: any/* kotlin.collections.Map<any, any> */): any/* kotlin.collections.List<any> */;
        getNestedMap(): any/* kotlin.collections.Map<string, kotlin.collections.List<any>> */;
        get items(): any/* kotlin.collections.List<any> */;
        get mapping(): any/* kotlin.collections.Map<any, any> */;
    }
    namespace StarProjectionClass {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => StarProjectionClass;
        }
    }
}



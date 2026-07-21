declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        const _any: unknown;
        const _nullable_any: unknown;
        const _array_any: Array<unknown>;
        function consumeAny(value: unknown): unknown;
        function consumeNullableAny(value: unknown): unknown;
        function produceDynamic(): unknown;
        function consumeDynamic(value: unknown): unknown;
        function consumeNullableDynamic(value: unknown): unknown;
        class WithDynamicMembers {
            constructor();
            anyMethod(value: unknown): unknown;
            dynamicMethod(value: unknown): unknown;
            get anyProperty(): unknown;
            get dynamicProperty(): unknown;
        }
        namespace WithDynamicMembers {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => WithDynamicMembers;
            }
        }
    }
}



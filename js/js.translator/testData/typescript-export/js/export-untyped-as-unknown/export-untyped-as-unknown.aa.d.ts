declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        const _any: NonNullable<unknown>;
        const _nullable_any: unknown;
        const _array_any: Array<NonNullable<unknown>>;
        function consumeAny(value: NonNullable<unknown>): NonNullable<unknown>;
        function consumeNullableAny(value: unknown): unknown;
        function produceDynamic(): unknown;
        function consumeDynamic(value: unknown): unknown;
        function consumeNullableDynamic(value: unknown): unknown;
        function consumeImplicitlyExported(value: NonNullable<unknown>/* foo.ImplicitlyExported */): void;
        function produceImplicitlyExported(): NonNullable<unknown>/* foo.ImplicitlyExported */;
        class WithDynamicMembers {
            constructor();
            anyMethod(value: NonNullable<unknown>): NonNullable<unknown>;
            dynamicMethod(value: unknown): unknown;
            get anyProperty(): NonNullable<unknown>;
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



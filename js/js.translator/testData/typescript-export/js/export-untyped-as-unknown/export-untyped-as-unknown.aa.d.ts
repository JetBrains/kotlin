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
        function transformUntyped(block: (p0: NonNullable<unknown>) => unknown): (p0: NonNullable<unknown>) => unknown;
        function produceStarProjectedBox(): foo.BoundedBox<any, NonNullable<unknown>, unknown>;
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
        interface ExportedBound {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.ExportedBound": unique symbol;
            };
        }
        class BoundedBox<T extends foo.ExportedBound, U, V> {
            constructor(value: T, payload: U);
            get value(): T;
            get payload(): U;
        }
        namespace BoundedBox {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T extends foo.ExportedBound, U, V>() => BoundedBox<T, U, V>;
            }
        }
    }
}

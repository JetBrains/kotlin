declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    namespace foo {
        abstract class Uninhabited {
            private constructor();
            static values(): [];
            static valueOf(value: string): never;
            get name(): never;
            get ordinal(): never;
        }
        namespace Uninhabited {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Uninhabited;
            }
        }
        abstract class TestEnumClass {
            private constructor();
            static get A(): foo.TestEnumClass & {
                get name(): "A";
                get ordinal(): 0;
            };
            static get B(): foo.TestEnumClass & {
                get name(): "B";
                get ordinal(): 1;
            };
            static get CustomNamedEntry(): foo.TestEnumClass & {
                get name(): "C";
                get ordinal(): 2;
            };
            static values(): [typeof foo.TestEnumClass.A, typeof foo.TestEnumClass.B, typeof foo.TestEnumClass.CustomNamedEntry];
            static valueOf(value: string): foo.TestEnumClass;
            get name(): "A" | "B" | "C";
            get ordinal(): 0 | 1 | 2;
            bar(value: string): string;
            bay(): string;
            get constructorParameter(): string;
            get foo(): number;
        }
        namespace TestEnumClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => TestEnumClass;
            }
            class Nested {
                constructor();
                get prop(): string;
            }
            namespace Nested {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => Nested;
                }
            }
        }
        class OuterClass {
            constructor();
        }
        namespace OuterClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => OuterClass;
            }
            abstract class NestedEnum {
                private constructor();
                static get A(): foo.OuterClass.NestedEnum & {
                    get name(): "A";
                    get ordinal(): 0;
                };
                static get B(): foo.OuterClass.NestedEnum & {
                    get name(): "B";
                    get ordinal(): 1;
                };
                static values(): [typeof foo.OuterClass.NestedEnum.A, typeof foo.OuterClass.NestedEnum.B];
                static valueOf(value: string): foo.OuterClass.NestedEnum;
                get name(): "A" | "B";
                get ordinal(): 0 | 1;
            }
            namespace NestedEnum {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => NestedEnum;
                }
            }
        }
    }
}

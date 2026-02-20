declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    namespace foo {
        /** @deprecated message 2 */
        const bar: string;
        /** @deprecated message 1 */
        function funktion(): void;
        /** @deprecated message 3 */
        class TestClass {
            constructor();
        }
        namespace TestClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => TestClass;
            }
        }
        class AnotherClass {
            /** @deprecated message 4 */
            constructor(value: string);
            /** @deprecated message 5 */
            static fromNothing(): foo.AnotherClass;
            static fromInt(value: number): foo.AnotherClass;
            /** @deprecated message 6 */
            foo(): void;
            baz(): void;
            get value(): string;
            /** @deprecated deprecated read-only property */
            get readOnlyProperty(): string;
            /** @deprecated deprecated read-write property */
            get readWriteProperty(): string;
            set readWriteProperty(value: string);
            get deprecatedGetter(): string;
            set deprecatedGetter(value: string);
            get deprecatedSetter(): string;
            set deprecatedSetter(value: string);
            /** @deprecated deprecated property */
            get mixedDeprecated(): string;
            set mixedDeprecated(value: string);
        }
        namespace AnotherClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => AnotherClass;
            }
        }
        interface TestInterface {
            /** @deprecated message 8 */
            foo(): void;
            bar(): void;
            /** @deprecated message 9 */
            readonly baz: string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.TestInterface": unique symbol;
            };
        }
        abstract class TestObject extends KtSingleton<TestObject.$metadata$.constructor>() {
            private constructor();
        }
        namespace TestObject {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor {
                    /** @deprecated message 10 */
                    foo(): void;
                    bar(): void;
                    /** @deprecated message 11 */
                    get baz(): string;
                    private constructor();
                }
            }
        }
        /** @deprecated Whole enum */
        abstract class TestEnum {
            private constructor();
            /** @deprecated Only first entry */
            static get A(): foo.TestEnum & {
                get name(): "A";
                get ordinal(): 0;
            };
            static get B(): foo.TestEnum & {
                get name(): "B";
                get ordinal(): 1;
            };
            static values(): [typeof foo.TestEnum.A, typeof foo.TestEnum.B];
            static valueOf(value: string): foo.TestEnum;
            get name(): "A" | "B";
            get ordinal(): 0 | 1;
        }
        namespace TestEnum {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => TestEnum;
            }
        }
    }
}

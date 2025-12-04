declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    namespace foo {
        const bar: string;
        function funktion(): void;
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
            constructor(value: string);
            static fromNothing(): foo.AnotherClass;
            static fromInt(value: number): foo.AnotherClass;
            foo(): void;
            baz(): void;
            get value(): string;
            get bar(): string;
        }
        namespace AnotherClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => AnotherClass;
            }
        }
        interface TestInterface {
            foo(): void;
            bar(): void;
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
                    foo(): void;
                    bar(): void;
                    get baz(): string;
                    private constructor();
                }
            }
        }
        abstract class TestEnum {
            private constructor();
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

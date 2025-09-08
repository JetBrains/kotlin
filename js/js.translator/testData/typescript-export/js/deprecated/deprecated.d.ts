declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        /** @deprecated message 2 */
        const bar: string;
        /** @deprecated message 1 */
        function foo(): void;
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
            get value(): string;
            /** @deprecated message 5 */
            static fromNothing(): foo.AnotherClass;
            static fromInt(value: number): foo.AnotherClass;
            /** @deprecated message 6 */
            foo(): void;
            baz(): void;
            /** @deprecated message 7 */
            get bar(): string;
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
    }
}

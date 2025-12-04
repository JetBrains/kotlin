declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    namespace foo {
        interface Object {
            readonly constructor?: any;
        }
        class JsNameTest {
            private constructor();
            runTest(): string;
            acceptObject(impl: foo.Object): string;
            testName1(): string;
            testName2(): string;
            setWithSetter1(value: string): void;
            getWithSetter1(): string;
            setWithSetter2(value: string): void;
            getWithSetter2(): string;
            get value(): number;
        }
        namespace JsNameTest {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => JsNameTest;
            }
            abstract class NotCompanion extends KtSingleton<NotCompanion.$metadata$.constructor>() {
                private constructor();
            }
            namespace NotCompanion {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    abstract class constructor {
                        create(): foo.JsNameTest;
                        createChild(value: number): foo.JsNameTest.NestedJsName;
                        private constructor();
                    }
                }
            }
            class NestedJsName {
                constructor(__value: number);
                get value(): number;
            }
            namespace NestedJsName {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => NestedJsName;
                }
            }
        }
        interface TestInterface {
            runTest(): string;
            acceptObject(impl: foo.Object): string;
            testName1(): string;
            testName2(): string;
            setWithSetter1(value: string): void;
            getWithSetter1(): string;
            setWithSetter2(value: string): void;
            getWithSetter2(): string;
            readonly value: number;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.TestInterface": unique symbol;
            };
        }
        namespace TestInterface {
            abstract class NotCompanion extends KtSingleton<NotCompanion.$metadata$.constructor>() {
                private constructor();
            }
            namespace NotCompanion {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    abstract class constructor {
                        create(): foo.JsNameTest;
                        createChild(value: number): foo.TestInterface.NestedJsName;
                        private constructor();
                    }
                }
            }
            class NestedJsName {
                constructor(__value: number);
                get value(): number;
            }
            namespace NestedJsName {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => NestedJsName;
                }
            }
        }
    }
}

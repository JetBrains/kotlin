declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        abstract class TestSealed {
            private constructor(name: string);
            get name(): string;
            protected static fromNumber(n: number): foo.TestSealed;
            protected get protectedVal(): number;
            protected protectedFun(): number;
        }
        namespace TestSealed {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => TestSealed;
            }
            class AA extends foo.TestSealed.$metadata$.constructor {
                constructor();
                bar(): string;
            }
            namespace AA {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => AA;
                }
            }
            class BB extends foo.TestSealed.$metadata$.constructor {
                constructor();
                baz(): string;
            }
            namespace BB {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => BB;
                }
            }
            class protectedClass {
                constructor();
            }
            namespace protectedClass {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => protectedClass;
                }
            }
            abstract class protectedNestedObject extends KtSingleton<protectedNestedObject.$metadata$.constructor>() {
                private constructor();
            }
            namespace protectedNestedObject {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    abstract class constructor {
                        private constructor();
                    }
                }
            }
            abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
                private constructor();
            }
            namespace Companion {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    abstract class constructor {
                        get companionObjectProp(): number;
                        private constructor();
                    }
                }
            }
        }
    }
}

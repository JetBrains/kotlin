declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        abstract class TestSealed {
            protected constructor(name: string);
            get name(): string;
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
        }
    }
}
declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        abstract class TestSealed {
            protected constructor(name: string);
            get name(): string;
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace TestSealed.$metadata$ {
            const constructor: abstract new () => TestSealed;
        }
        namespace TestSealed {
            class AA extends foo.TestSealed.$metadata$.constructor {
                constructor();
                bar(): string;
            }
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace AA.$metadata$ {
                const constructor: abstract new () => AA;
            }
            class BB extends foo.TestSealed.$metadata$.constructor {
                constructor();
                baz(): string;
            }
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace BB.$metadata$ {
                const constructor: abstract new () => BB;
            }
        }
    }
}
declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        abstract class TestAbstract {
            constructor(name: string);
            get name(): string;
        }
        namespace TestAbstract {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => TestAbstract;
            }
            class AA extends foo.TestAbstract.$metadata$.constructor {
                constructor();
                bar(): string;
            }
            namespace AA {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => AA;
                }
            }
            class BB extends foo.TestAbstract.$metadata$.constructor {
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
        abstract class Money<T extends foo.Money<T>> {
            protected constructor();
            abstract get amount(): number;
            isZero(): boolean;
        }
        namespace Money {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T extends foo.Money<T>>() => Money<T>;
            }
        }
        class Euro extends foo.Money.$metadata$.constructor<foo.Euro> {
            constructor(amount: number);
            get amount(): number;
        }
        namespace Euro {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Euro;
            }
        }
    }
}

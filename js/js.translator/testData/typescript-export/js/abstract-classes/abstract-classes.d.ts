declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        abstract class TestAbstract {
            constructor(name: string);
            get name(): string;
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace TestAbstract.$metadata$ {
            const constructor: abstract new () => TestAbstract;
        }
        namespace TestAbstract {
            class AA extends foo.TestAbstract.$metadata$.constructor {
                constructor();
                bar(): string;
            }
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace AA.$metadata$ {
                const constructor: abstract new () => AA;
            }
            class BB extends foo.TestAbstract.$metadata$.constructor {
                constructor();
                baz(): string;
            }
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace BB.$metadata$ {
                const constructor: abstract new () => BB;
            }
        }
        abstract class Money<T extends foo.Money<T>> {
            protected constructor();
            abstract get amount(): number;
            isZero(): boolean;
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace Money.$metadata$ {
            const constructor: abstract new <T extends foo.Money<T>>() => Money<T>;
        }
        class Euro extends foo.Money.$metadata$.constructor<foo.Euro> {
            constructor(amount: number);
            get amount(): number;
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace Euro.$metadata$ {
            const constructor: abstract new () => Euro;
        }
    }
}

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
            isZero(): boolean;
            abstract get amount(): number;
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
        abstract class AbstractClassWithProtected {
            constructor();
            protected abstract protectedAbstractFun(): number;
            protected abstract get protectedAbstractVal(): number;
        }
        namespace AbstractClassWithProtected {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => AbstractClassWithProtected;
            }
            class N extends foo.AbstractClassWithProtected.$metadata$.constructor {
                constructor();
            }
            namespace N {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => N;
                }
            }
        }
        abstract class AbstractInheritorOfAbstractClass extends foo.AbstractClassWithProtected.$metadata$.constructor {
            constructor();
            protected protectedAbstractFun(): number;
        }
        namespace AbstractInheritorOfAbstractClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => AbstractInheritorOfAbstractClass;
            }
        }
        class InheritorOfAbstractClass extends foo.AbstractInheritorOfAbstractClass.$metadata$.constructor {
            constructor();
        }
        namespace InheritorOfAbstractClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => InheritorOfAbstractClass;
            }
        }
    }
}

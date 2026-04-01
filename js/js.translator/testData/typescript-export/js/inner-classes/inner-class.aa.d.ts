declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    namespace foo {
        class TestInner {
            constructor(a: string);
            get a(): string;
            get Inner(): {
                new(a: string): foo.TestInner.Inner;
                fromNumber(a: number): foo.TestInner.Inner;
            };
            get OpenInnerWithPublicConstructor(): {
                new(a: string): foo.TestInner.OpenInnerWithPublicConstructor;
                fromNumber(a: number): foo.TestInner.OpenInnerWithPublicConstructor;
            };
            get OpenInnerWithProtectedConstructor(): {
            };
            get SubclassOfAbstractInnerClass(): {
                new(a: string): foo.TestInner.SubclassOfAbstractInnerClass;
            };
            get SubclassOfOpenInnerClass(): {
                new(a: string): foo.TestInner.SubclassOfOpenInnerClass;
            };
        }
        namespace TestInner {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => TestInner;
            }
            class Inner {
                private constructor();
                get a(): string;
                get concat(): string;
                get SecondLayerInner(): {
                    new(a: string): foo.TestInner.Inner.SecondLayerInner;
                };
            }
            namespace Inner {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => Inner;
                }
                class SecondLayerInner {
                    private constructor();
                    get a(): string;
                    get concat(): string;
                }
                namespace SecondLayerInner {
                    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                    namespace $metadata$ {
                        const constructor: abstract new () => SecondLayerInner;
                    }
                }
            }
            class OpenInnerWithPublicConstructor {
                protected constructor($outer: foo.TestInner, a: string);
                get a(): string;
                get concat(): string;
            }
            namespace OpenInnerWithPublicConstructor {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => OpenInnerWithPublicConstructor;
                }
            }
            class OpenInnerWithProtectedConstructor {
                protected constructor($outer: foo.TestInner, a: string);
                get a(): string;
                get concat(): string;
            }
            namespace OpenInnerWithProtectedConstructor {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => OpenInnerWithProtectedConstructor;
                }
            }
            abstract class AbstractInnerWithProtectedConstructor {
                protected constructor($outer: foo.TestInner, a: string);
                get a(): string;
                abstract get concat(): string;
            }
            namespace AbstractInnerWithProtectedConstructor {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => AbstractInnerWithProtectedConstructor;
                }
            }
            class SubclassOfAbstractInnerClass extends foo.TestInner.AbstractInnerWithProtectedConstructor.$metadata$.constructor {
                private constructor();
                get concat(): string;
            }
            namespace SubclassOfAbstractInnerClass {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => SubclassOfAbstractInnerClass;
                }
            }
            class SubclassOfOpenInnerClass extends foo.TestInner.OpenInnerWithProtectedConstructor.$metadata$.constructor {
                private constructor();
            }
            namespace SubclassOfOpenInnerClass {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => SubclassOfOpenInnerClass;
                }
            }
            abstract class AbstractInnerWithSecondaryConstructor {
                protected constructor($outer: foo.TestInner, a: string);
                get a(): string;
            }
            namespace AbstractInnerWithSecondaryConstructor {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => AbstractInnerWithSecondaryConstructor;
                }
            }
        }
    }
}

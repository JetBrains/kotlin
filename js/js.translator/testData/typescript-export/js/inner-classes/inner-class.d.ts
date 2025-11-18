declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        class TestInner {
            constructor(a: string);
            get a(): string;
            get Inner(): {
                new(a: string): TestInner.Inner;
            } & typeof TestInner.Inner;
            get OpenInnerWithPublicConstructor(): {
                new(a: string): TestInner.OpenInnerWithPublicConstructor;
            } & typeof TestInner.OpenInnerWithPublicConstructor;
            get OpenInnerWithProtectedConstructor(): {
            } & typeof TestInner.OpenInnerWithProtectedConstructor;
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
                static fromNumber(a: number): foo.TestInner.Inner;
                get SecondLayerInner(): {
                    new(a: string): TestInner.Inner.SecondLayerInner;
                } & typeof TestInner.Inner.SecondLayerInner;
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
                static fromNumber(a: number): foo.TestInner.OpenInnerWithPublicConstructor;
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
                protected static fromNumber(a: number): foo.TestInner.OpenInnerWithProtectedConstructor;
                get concat(): string;
            }
            namespace OpenInnerWithProtectedConstructor {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => OpenInnerWithProtectedConstructor;
                }
            }
        }
    }
}

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
        }
        namespace TestInner {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => TestInner;
            }
            class Inner {
                protected constructor($outer: foo.TestInner, a: string);
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
                    protected constructor($outer: foo.TestInner.Inner, a: string);
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
        }
    }
}

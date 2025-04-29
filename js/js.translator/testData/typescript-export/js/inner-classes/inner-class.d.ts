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
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace TestInner.$metadata$ {
            const constructor: abstract new () => TestInner;
        }
        namespace TestInner {
            class Inner {
                protected constructor($outer: foo.TestInner, a: string);
                get a(): string;
                get concat(): string;
                static fromNumber(a: number): foo.TestInner.Inner;
                get SecondLayerInner(): {
                    new(a: string): TestInner.Inner.SecondLayerInner;
                } & typeof TestInner.Inner.SecondLayerInner;
            }
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace Inner.$metadata$ {
                const constructor: abstract new () => Inner;
            }
            namespace Inner {
                class SecondLayerInner {
                    protected constructor($outer: foo.TestInner.Inner, a: string);
                    get a(): string;
                    get concat(): string;
                }
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace SecondLayerInner.$metadata$ {
                    const constructor: abstract new () => SecondLayerInner;
                }
            }
        }
    }
}

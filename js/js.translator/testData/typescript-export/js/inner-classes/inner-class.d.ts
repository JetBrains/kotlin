declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        class TestInner {
            constructor(a: string);
            get a(): string;
            get Inner(): {
                new(a: string): TestInner.Inner;
            } & typeof TestInner.Inner;
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
            namespace Inner {
                class SecondLayerInner {
                    protected constructor($outer: foo.TestInner.Inner, a: string);
                    get a(): string;
                    get concat(): string;
                }
            }
        }
    }
}

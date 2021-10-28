declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        class TestInner {
            constructor(a: string);
            readonly a: string;
            readonly Inner: {
                new(a: string): TestInner.Inner;
            } & typeof TestInner.Inner;
        }
        namespace TestInner {
            class Inner {
                protected constructor($outer: foo.TestInner, a: string);
                readonly a: string;
                readonly concat: string;
                static fromNumber(a: number): foo.TestInner.Inner;
                readonly SecondLayerInner: {
                    new(a: string): TestInner.Inner.SecondLayerInner;
                } & typeof TestInner.Inner.SecondLayerInner;
            }
            namespace Inner {
                class SecondLayerInner {
                    protected constructor($outer: foo.TestInner.Inner, a: string);
                    readonly a: string;
                    readonly concat: string;
                }
            }
        }
    }
}
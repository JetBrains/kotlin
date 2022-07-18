declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        class JsNameTest {
            private constructor();
            get value(): number;
            runTest(): string;
            acceptObject(impl: Object): string;
            static get NotCompanion(): {
                create(): foo.JsNameTest;
                createChild(value: number): foo.JsNameTest.NestedJsName;
            };
        }
        namespace JsNameTest {
            class NestedJsName {
                constructor(__value: number);
                get value(): number;
            }
        }
    }
}
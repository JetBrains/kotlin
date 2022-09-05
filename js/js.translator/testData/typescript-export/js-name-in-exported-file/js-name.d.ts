declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        interface Object {
            readonly constructor?: any;
        }
    }
    namespace foo {
        class JsNameTest {
            private constructor();
            get value(): number;
            runTest(): string;
            acceptObject(impl: foo.Object): string;
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
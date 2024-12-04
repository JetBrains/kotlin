declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        abstract class TestSealed {
            protected constructor(name: string);
            get name(): string;
        }
        namespace TestSealed {
            class AA extends foo.TestSealed {
                constructor();
                bar(): string;
            }
            class BB extends foo.TestSealed {
                constructor();
                baz(): string;
            }
        }
    }
}
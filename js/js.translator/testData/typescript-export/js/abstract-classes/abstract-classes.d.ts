declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        abstract class TestAbstract {
            constructor(name: string);
            get name(): string;
        }
        namespace TestAbstract {
            class AA extends foo.TestAbstract {
                constructor();
                bar(): string;
            }
            class BB extends foo.TestAbstract {
                constructor();
                baz(): string;
            }
        }
    }
}
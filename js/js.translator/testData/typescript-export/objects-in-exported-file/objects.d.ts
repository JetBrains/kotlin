declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        const O0: {
        };
        const O: {
            get x(): number;
            foo(): number;
        };
        function takesO(o: typeof foo.O): number;
        abstract class Parent {
            private constructor();
        }
        namespace Parent {
            abstract class Nested1 extends _objects_.foo$Parent$Nested1 {
                private constructor();
            }
            namespace Nested1 {
                class Nested2 {
                    constructor();
                }
                namespace Nested2 {
                    abstract class Companion {
                        private constructor();
                    }
                    namespace Companion {
                        class Nested3 {
                            constructor();
                        }
                    }
                }
            }
        }
        function getParent(): typeof foo.Parent;
        function createNested1(): typeof foo.Parent.Nested1;
        function createNested2(): foo.Parent.Nested1.Nested2;
        function createNested3(): foo.Parent.Nested1.Nested2.Companion.Nested3;
    }
    namespace _objects_ {
        const foo$Parent$Nested1: {
            get value(): string;
        } & {
            new(): any;
        };
    }
}
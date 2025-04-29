declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        interface Interface1 {
            foo(): string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.Interface1": unique symbol;
            };
        }
        interface Interface2 {
            bar(): string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.Interface2": unique symbol;
            };
        }
        abstract class O0 extends KtSingleton<O0.$metadata$.constructor>() {
            private constructor();
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace O0.$metadata$ {
            abstract class constructor {
                private constructor();
            }
        }
        abstract class O extends KtSingleton<O.$metadata$.constructor>() {
            private constructor();
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace O.$metadata$ {
            abstract class constructor {
                get x(): number;
                foo(): number;
                private constructor();
            }
        }
        function takesO(o: typeof foo.O): number;
        abstract class WithSimpleObjectInside extends KtSingleton<WithSimpleObjectInside.$metadata$.constructor>() {
            private constructor();
        }
        namespace WithSimpleObjectInside {
            abstract class SimpleObject extends KtSingleton<SimpleObject.$metadata$.constructor>() {
                private constructor();
            }
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace SimpleObject.$metadata$ {
                abstract class constructor {
                    get value(): string;
                    private constructor();
                }
            }
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace WithSimpleObjectInside.$metadata$ {
            abstract class constructor {
                get value(): string;
                private constructor();
            }
        }
        abstract class Parent extends KtSingleton<Parent.$metadata$.constructor>() {
            private constructor();
        }
        namespace Parent {
            abstract class Nested1 extends KtSingleton<Nested1.$metadata$.constructor>() {
                private constructor();
            }
            namespace Nested1 {
                class Nested2 {
                    constructor();
                }
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace Nested2.$metadata$ {
                    const constructor: abstract new () => Nested2;
                }
                namespace Nested2 {
                    abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
                        private constructor();
                    }
                    namespace Companion {
                        class Nested3 {
                            constructor();
                        }
                        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                        namespace Nested3.$metadata$ {
                            const constructor: abstract new () => Nested3;
                        }
                    }
                    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                    namespace Companion.$metadata$ {
                        abstract class constructor {
                            private constructor();
                        }
                    }
                }
            }
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace Nested1.$metadata$ {
                abstract class constructor {
                    get value(): string;
                    private constructor();
                }
            }
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace Parent.$metadata$ {
            abstract class constructor {
                private constructor();
            }
        }
        function getParent(): typeof foo.Parent;
        function createNested1(): typeof foo.Parent.Nested1;
        function createNested2(): foo.Parent.Nested1.Nested2;
        function createNested3(): foo.Parent.Nested1.Nested2.Companion.Nested3;
        abstract class BaseWithCompanion {
            constructor();
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace BaseWithCompanion.$metadata$ {
            const constructor: abstract new () => BaseWithCompanion;
        }
        namespace BaseWithCompanion {
            abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
                private constructor();
            }
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace Companion.$metadata$ {
                abstract class constructor {
                    get any(): string;
                    private constructor();
                }
            }
        }
        class ChildWithCompanion extends foo.BaseWithCompanion.$metadata$.constructor {
            constructor();
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace ChildWithCompanion.$metadata$ {
            const constructor: abstract new () => ChildWithCompanion;
        }
        namespace ChildWithCompanion {
            abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
                private constructor();
            }
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace Companion.$metadata$ {
                abstract class constructor {
                    private constructor();
                }
            }
        }
        abstract class SimpleObjectWithInterface1 extends KtSingleton<SimpleObjectWithInterface1.$metadata$.constructor>() {
            private constructor();
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace SimpleObjectWithInterface1.$metadata$ {
            abstract class constructor implements foo.Interface1 {
                foo(): string;
                readonly __doNotUseOrImplementIt: foo.Interface1["__doNotUseOrImplementIt"];
                private constructor();
            }
        }
        abstract class SimpleObjectWithBothInterfaces extends KtSingleton<SimpleObjectWithBothInterfaces.$metadata$.constructor>() {
            private constructor();
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace SimpleObjectWithBothInterfaces.$metadata$ {
            abstract class constructor implements foo.Interface1, foo.Interface2 {
                foo(): string;
                bar(): string;
                readonly __doNotUseOrImplementIt: foo.Interface1["__doNotUseOrImplementIt"] & foo.Interface2["__doNotUseOrImplementIt"];
                private constructor();
            }
        }
        abstract class SimpleObjectInheritingAbstract extends KtSingleton<SimpleObjectInheritingAbstract.$metadata$.constructor>() {
            private constructor();
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace SimpleObjectInheritingAbstract.$metadata$ {
            abstract class constructor extends foo.BaseWithCompanion.$metadata$.constructor {
                private constructor();
            }
        }
        abstract class SimpleObjectInheritingAbstractAndInterface1 extends KtSingleton<SimpleObjectInheritingAbstractAndInterface1.$metadata$.constructor>() {
            private constructor();
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace SimpleObjectInheritingAbstractAndInterface1.$metadata$ {
            abstract class constructor extends foo.BaseWithCompanion.$metadata$.constructor implements foo.Interface1 {
                foo(): string;
                readonly __doNotUseOrImplementIt: foo.Interface1["__doNotUseOrImplementIt"];
                private constructor();
            }
        }
        abstract class SimpleObjectInheritingAbstractAndBothInterfaces extends KtSingleton<SimpleObjectInheritingAbstractAndBothInterfaces.$metadata$.constructor>() {
            private constructor();
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace SimpleObjectInheritingAbstractAndBothInterfaces.$metadata$ {
            abstract class constructor extends foo.BaseWithCompanion.$metadata$.constructor implements foo.Interface1, foo.Interface2 {
                foo(): string;
                bar(): string;
                readonly __doNotUseOrImplementIt: foo.Interface1["__doNotUseOrImplementIt"] & foo.Interface2["__doNotUseOrImplementIt"];
                private constructor();
            }
        }
        abstract class SimpleObjectWithInterface1AndNested extends KtSingleton<SimpleObjectWithInterface1AndNested.$metadata$.constructor>() {
            private constructor();
        }
        namespace SimpleObjectWithInterface1AndNested {
            class Nested {
                constructor();
            }
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace Nested.$metadata$ {
                const constructor: abstract new () => Nested;
            }
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace SimpleObjectWithInterface1AndNested.$metadata$ {
            abstract class constructor implements foo.Interface1 {
                foo(): string;
                readonly __doNotUseOrImplementIt: foo.Interface1["__doNotUseOrImplementIt"];
                private constructor();
            }
        }
        abstract class SimpleObjectWithBothInterfacesAndNested extends KtSingleton<SimpleObjectWithBothInterfacesAndNested.$metadata$.constructor>() {
            private constructor();
        }
        namespace SimpleObjectWithBothInterfacesAndNested {
            class Nested {
                constructor();
            }
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace Nested.$metadata$ {
                const constructor: abstract new () => Nested;
            }
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace SimpleObjectWithBothInterfacesAndNested.$metadata$ {
            abstract class constructor implements foo.Interface1, foo.Interface2 {
                foo(): string;
                bar(): string;
                readonly __doNotUseOrImplementIt: foo.Interface1["__doNotUseOrImplementIt"] & foo.Interface2["__doNotUseOrImplementIt"];
                private constructor();
            }
        }
        abstract class SimpleObjectInheritingAbstractAndNested extends KtSingleton<SimpleObjectInheritingAbstractAndNested.$metadata$.constructor>() {
            private constructor();
        }
        namespace SimpleObjectInheritingAbstractAndNested {
            class Nested {
                constructor();
            }
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace Nested.$metadata$ {
                const constructor: abstract new () => Nested;
            }
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace SimpleObjectInheritingAbstractAndNested.$metadata$ {
            abstract class constructor extends foo.BaseWithCompanion.$metadata$.constructor {
                private constructor();
            }
        }
        abstract class SimpleObjectInheritingAbstractAndInterface1AndNested extends KtSingleton<SimpleObjectInheritingAbstractAndInterface1AndNested.$metadata$.constructor>() {
            private constructor();
        }
        namespace SimpleObjectInheritingAbstractAndInterface1AndNested {
            class Nested {
                constructor();
            }
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace Nested.$metadata$ {
                const constructor: abstract new () => Nested;
            }
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace SimpleObjectInheritingAbstractAndInterface1AndNested.$metadata$ {
            abstract class constructor extends foo.BaseWithCompanion.$metadata$.constructor implements foo.Interface1 {
                foo(): string;
                readonly __doNotUseOrImplementIt: foo.Interface1["__doNotUseOrImplementIt"];
                private constructor();
            }
        }
        abstract class SimpleObjectInheritingAbstractAndBothInterfacesAndNested extends KtSingleton<SimpleObjectInheritingAbstractAndBothInterfacesAndNested.$metadata$.constructor>() {
            private constructor();
        }
        namespace SimpleObjectInheritingAbstractAndBothInterfacesAndNested {
            class Nested {
                constructor();
            }
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace Nested.$metadata$ {
                const constructor: abstract new () => Nested;
            }
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace SimpleObjectInheritingAbstractAndBothInterfacesAndNested.$metadata$ {
            abstract class constructor extends foo.BaseWithCompanion.$metadata$.constructor implements foo.Interface1, foo.Interface2 {
                foo(): string;
                bar(): string;
                readonly __doNotUseOrImplementIt: foo.Interface1["__doNotUseOrImplementIt"] & foo.Interface2["__doNotUseOrImplementIt"];
                private constructor();
            }
        }
        abstract class Money<T extends foo.Money<T, Array<T>>, I extends Array<T>> {
            protected constructor();
            abstract get amount(): number;
            isZero(): boolean;
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace Money.$metadata$ {
            const constructor: abstract new <T extends foo.Money<T, Array<T>>, I extends Array<T>>() => Money<T, I>;
        }
        abstract class Zero extends KtSingleton<Zero.$metadata$.constructor>() {
            private constructor();
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace Zero.$metadata$ {
            abstract class constructor extends foo.Money.$metadata$.constructor<constructor, Array<constructor>> {
                get amount(): number;
                private constructor();
            }
        }
    }
}

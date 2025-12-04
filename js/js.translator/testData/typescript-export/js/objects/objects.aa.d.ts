declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    namespace foo {
        function takesO(o: typeof foo.O): number;
        function getParent(): typeof foo.Parent;
        function createNested1(): typeof foo.Parent.Nested1;
        function createNested2(): foo.Parent.Nested1.Nested2;
        function createNested3(): foo.Parent.Nested1.Nested2.Companion.Nested3;
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
        namespace O0 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor {
                    private constructor();
                }
            }
        }
        abstract class O extends KtSingleton<O.$metadata$.constructor>() {
            private constructor();
        }
        namespace O {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor {
                    foo(): number;
                    get x(): number;
                    private constructor();
                }
            }
        }
        abstract class WithSimpleObjectInside extends KtSingleton<WithSimpleObjectInside.$metadata$.constructor>() {
            private constructor();
        }
        namespace WithSimpleObjectInside {
            abstract class SimpleObject extends KtSingleton<SimpleObject.$metadata$.constructor>() {
                private constructor();
            }
            namespace SimpleObject {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    abstract class constructor {
                        get value(): string;
                        private constructor();
                    }
                }
            }
        }
        namespace WithSimpleObjectInside {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor {
                    get value(): string;
                    private constructor();
                }
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
                namespace Nested2 {
                    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                    namespace $metadata$ {
                        const constructor: abstract new () => Nested2;
                    }
                    abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
                        private constructor();
                    }
                    namespace Companion {
                        class Nested3 {
                            constructor();
                        }
                        namespace Nested3 {
                            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                            namespace $metadata$ {
                                const constructor: abstract new () => Nested3;
                            }
                        }
                    }
                    namespace Companion {
                        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                        namespace $metadata$ {
                            abstract class constructor {
                                private constructor();
                            }
                        }
                    }
                }
            }
            namespace Nested1 {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    abstract class constructor {
                        get value(): string;
                        private constructor();
                    }
                }
            }
        }
        namespace Parent {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor {
                    private constructor();
                }
            }
        }
        abstract class BaseWithCompanion {
            constructor();
        }
        namespace BaseWithCompanion {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => BaseWithCompanion;
            }
            abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
                private constructor();
            }
            namespace Companion {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    abstract class constructor {
                        get any(): string;
                        private constructor();
                    }
                }
            }
        }
        class ChildWithCompanion extends foo.BaseWithCompanion.$metadata$.constructor {
            constructor();
        }
        namespace ChildWithCompanion {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ChildWithCompanion;
            }
            abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
                private constructor();
            }
            namespace Companion {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    abstract class constructor {
                        private constructor();
                    }
                }
            }
        }
        abstract class SimpleObjectWithInterface1 extends KtSingleton<SimpleObjectWithInterface1.$metadata$.constructor>() {
            private constructor();
        }
        namespace SimpleObjectWithInterface1 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor implements foo.Interface1 {
                    foo(): string;
                    readonly __doNotUseOrImplementIt: foo.Interface1["__doNotUseOrImplementIt"];
                    private constructor();
                }
            }
        }
        abstract class SimpleObjectWithBothInterfaces extends KtSingleton<SimpleObjectWithBothInterfaces.$metadata$.constructor>() {
            private constructor();
        }
        namespace SimpleObjectWithBothInterfaces {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor implements foo.Interface1, foo.Interface2 {
                    foo(): string;
                    bar(): string;
                    readonly __doNotUseOrImplementIt: foo.Interface1["__doNotUseOrImplementIt"] & foo.Interface2["__doNotUseOrImplementIt"];
                    private constructor();
                }
            }
        }
        abstract class SimpleObjectInheritingAbstract extends KtSingleton<SimpleObjectInheritingAbstract.$metadata$.constructor>() {
            private constructor();
        }
        namespace SimpleObjectInheritingAbstract {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor extends foo.BaseWithCompanion.$metadata$.constructor {
                    private constructor();
                }
            }
        }
        abstract class SimpleObjectInheritingAbstractAndInterface1 extends KtSingleton<SimpleObjectInheritingAbstractAndInterface1.$metadata$.constructor>() {
            private constructor();
        }
        namespace SimpleObjectInheritingAbstractAndInterface1 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor extends foo.BaseWithCompanion.$metadata$.constructor implements foo.Interface1 {
                    foo(): string;
                    readonly __doNotUseOrImplementIt: foo.Interface1["__doNotUseOrImplementIt"];
                    private constructor();
                }
            }
        }
        abstract class SimpleObjectInheritingAbstractAndBothInterfaces extends KtSingleton<SimpleObjectInheritingAbstractAndBothInterfaces.$metadata$.constructor>() {
            private constructor();
        }
        namespace SimpleObjectInheritingAbstractAndBothInterfaces {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor extends foo.BaseWithCompanion.$metadata$.constructor implements foo.Interface1, foo.Interface2 {
                    foo(): string;
                    bar(): string;
                    readonly __doNotUseOrImplementIt: foo.Interface1["__doNotUseOrImplementIt"] & foo.Interface2["__doNotUseOrImplementIt"];
                    private constructor();
                }
            }
        }
        abstract class SimpleObjectWithInterface1AndNested extends KtSingleton<SimpleObjectWithInterface1AndNested.$metadata$.constructor>() {
            private constructor();
        }
        namespace SimpleObjectWithInterface1AndNested {
            class Nested {
                constructor();
            }
            namespace Nested {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => Nested;
                }
            }
        }
        namespace SimpleObjectWithInterface1AndNested {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor implements foo.Interface1 {
                    foo(): string;
                    readonly __doNotUseOrImplementIt: foo.Interface1["__doNotUseOrImplementIt"];
                    private constructor();
                }
            }
        }
        abstract class SimpleObjectWithBothInterfacesAndNested extends KtSingleton<SimpleObjectWithBothInterfacesAndNested.$metadata$.constructor>() {
            private constructor();
        }
        namespace SimpleObjectWithBothInterfacesAndNested {
            class Nested {
                constructor();
            }
            namespace Nested {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => Nested;
                }
            }
        }
        namespace SimpleObjectWithBothInterfacesAndNested {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor implements foo.Interface1, foo.Interface2 {
                    foo(): string;
                    bar(): string;
                    readonly __doNotUseOrImplementIt: foo.Interface1["__doNotUseOrImplementIt"] & foo.Interface2["__doNotUseOrImplementIt"];
                    private constructor();
                }
            }
        }
        abstract class SimpleObjectInheritingAbstractAndNested extends KtSingleton<SimpleObjectInheritingAbstractAndNested.$metadata$.constructor>() {
            private constructor();
        }
        namespace SimpleObjectInheritingAbstractAndNested {
            class Nested {
                constructor();
            }
            namespace Nested {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => Nested;
                }
            }
        }
        namespace SimpleObjectInheritingAbstractAndNested {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor extends foo.BaseWithCompanion.$metadata$.constructor {
                    private constructor();
                }
            }
        }
        abstract class SimpleObjectInheritingAbstractAndInterface1AndNested extends KtSingleton<SimpleObjectInheritingAbstractAndInterface1AndNested.$metadata$.constructor>() {
            private constructor();
        }
        namespace SimpleObjectInheritingAbstractAndInterface1AndNested {
            class Nested {
                constructor();
            }
            namespace Nested {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => Nested;
                }
            }
        }
        namespace SimpleObjectInheritingAbstractAndInterface1AndNested {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor extends foo.BaseWithCompanion.$metadata$.constructor implements foo.Interface1 {
                    foo(): string;
                    readonly __doNotUseOrImplementIt: foo.Interface1["__doNotUseOrImplementIt"];
                    private constructor();
                }
            }
        }
        abstract class SimpleObjectInheritingAbstractAndBothInterfacesAndNested extends KtSingleton<SimpleObjectInheritingAbstractAndBothInterfacesAndNested.$metadata$.constructor>() {
            private constructor();
        }
        namespace SimpleObjectInheritingAbstractAndBothInterfacesAndNested {
            class Nested {
                constructor();
            }
            namespace Nested {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => Nested;
                }
            }
        }
        namespace SimpleObjectInheritingAbstractAndBothInterfacesAndNested {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor extends foo.BaseWithCompanion.$metadata$.constructor implements foo.Interface1, foo.Interface2 {
                    foo(): string;
                    bar(): string;
                    readonly __doNotUseOrImplementIt: foo.Interface1["__doNotUseOrImplementIt"] & foo.Interface2["__doNotUseOrImplementIt"];
                    private constructor();
                }
            }
        }
        abstract class Money<T extends foo.Money<T, Array<T>>, I extends Array<T>> {
            protected constructor();
            isZero(): boolean;
            abstract get amount(): number;
        }
        namespace Money {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T extends foo.Money<T, Array<T>>, I extends Array<T>>() => Money<T, I>;
            }
        }
        abstract class Zero extends KtSingleton<Zero.$metadata$.constructor>() {
            private constructor();
        }
        namespace Zero {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor extends foo.Money.$metadata$.constructor<constructor, Array<constructor>> {
                    get amount(): number;
                    private constructor();
                }
            }
        }
        abstract class AbstractClassWithProtected {
            constructor();
            protected abstract protectedAbstractFun(): number;
            protected abstract get protectedAbstractVal(): number;
        }
        namespace AbstractClassWithProtected {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => AbstractClassWithProtected;
            }
        }
        abstract class ObjectWithProtected extends KtSingleton<ObjectWithProtected.$metadata$.constructor>() {
            private constructor();
        }
        namespace ObjectWithProtected {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor extends foo.AbstractClassWithProtected.$metadata$.constructor {
                    private constructor();
                }
            }
        }
    }
}

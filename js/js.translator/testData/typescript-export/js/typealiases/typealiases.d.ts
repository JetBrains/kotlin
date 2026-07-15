declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace kotlin.collections {
        interface KtList<out E> /* extends kotlin.collections.Collection<E> */ {
            asJsReadonlyArrayView(): ReadonlyArray<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtList": unique symbol;
            };
        }
        namespace KtList {
            function fromJsArray<E>(array: ReadonlyArray<E>): kotlin.collections.KtList<E>;
        }
    }
    namespace foo {
        interface SomeExternalInterface {
        }
    }
    namespace foo {
        class SomeClass {
            constructor(value: string);
            get value(): string;
        }
        namespace SomeClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => SomeClass;
            }
        }
        class GenericClass<T> {
            constructor(value: T);
            get value(): T;
            get Inner(): {
                new<S>(): GenericClass.Inner<S, T>;
            };
        }
        namespace GenericClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T>() => GenericClass<T>;
            }
            class Inner<S, T$GenericClass> {
                private constructor();
            }
            namespace Inner {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new <S, T$GenericClass>() => Inner<S, T$GenericClass>;
                }
            }
        }
        class GenericClassWithVariance<out T> {
            constructor(value: T);
            get value(): T;
        }
        namespace GenericClassWithVariance {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T>() => GenericClassWithVariance<T>;
            }
        }
        class TwoGenericParamsClass<A, B> {
            constructor(first: A, second: B);
            get first(): A;
            get second(): B;
        }
        namespace TwoGenericParamsClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <A, B>() => TwoGenericParamsClass<A, B>;
            }
        }
        interface SomeInterface {
            readonly prop: string;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.SomeInterface": unique symbol;
            };
        }
        abstract class SomeEnum {
            private constructor();
            static get A(): foo.SomeEnum & {
                get name(): "A";
                get ordinal(): 0;
            };
            static get B(): foo.SomeEnum & {
                get name(): "B";
                get ordinal(): 1;
            };
            static get C(): foo.SomeEnum & {
                get name(): "C";
                get ordinal(): 2;
            };
            static values(): [typeof foo.SomeEnum.A, typeof foo.SomeEnum.B, typeof foo.SomeEnum.C];
            static valueOf(value: string): foo.SomeEnum;
            get name(): "A" | "B" | "C";
            get ordinal(): 0 | 1 | 2;
        }
        namespace SomeEnum {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => SomeEnum;
            }
        }
        abstract class SomeObject extends KtSingleton<SomeObject.$metadata$.constructor>() {
            private constructor();
        }
        namespace SomeObject {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor {
                    get value(): string;
                    private constructor();
                }
            }
        }
        interface Comparable<in T> {
            compareTo(other: T): number;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.Comparable": unique symbol;
            };
        }
        class ClassWithConstraint<T extends foo.SomeClass> {
            constructor(value: T);
            get value(): T;
            get InnerWithConstraints(): {
                new<S extends foo.SomeEnum>(): ClassWithConstraint.InnerWithConstraints<S, T>;
            };
        }
        namespace ClassWithConstraint {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T extends foo.SomeClass>() => ClassWithConstraint<T>;
            }
            class InnerWithConstraints<S extends foo.SomeEnum, T$ClassWithConstraint extends foo.SomeClass> {
                private constructor();
            }
            namespace InnerWithConstraints {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new <S extends foo.SomeEnum, T$ClassWithConstraint extends foo.SomeClass>() => InnerWithConstraints<S, T$ClassWithConstraint>;
                }
            }
        }
        class RecursiveBoundClass<T extends foo.Comparable<T>> {
            constructor(value: T);
            get value(): T;
        }
        namespace RecursiveBoundClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T extends foo.Comparable<T>>() => RecursiveBoundClass<T>;
            }
        }
        class ClassWithNestedTypealiases {
            constructor();
        }
        namespace ClassWithNestedTypealiases {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ClassWithNestedTypealiases;
            }
        }
        class GenericClassWithNestedTypealiases<T> {
            constructor();
        }
        namespace GenericClassWithNestedTypealiases {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T>() => GenericClassWithNestedTypealiases<T>;
            }
        }
        interface InterfaceWithNestedTypealiases {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.InterfaceWithNestedTypealiases": unique symbol;
            };
        }
        abstract class ObjectWithNestedTypealiases extends KtSingleton<ObjectWithNestedTypealiases.$metadata$.constructor>() {
            private constructor();
        }
        namespace ObjectWithNestedTypealiases {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor {
                    private constructor();
                }
            }
        }
        class ClassWithCompanionTypealiases {
            constructor();
        }
        namespace ClassWithCompanionTypealiases {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ClassWithCompanionTypealiases;
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
        class ClassWithNamedCompanionTypealiases {
            constructor();
        }
        namespace ClassWithNamedCompanionTypealiases {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ClassWithNamedCompanionTypealiases;
            }
            abstract class Named extends KtSingleton<Named.$metadata$.constructor>() {
                private constructor();
            }
            namespace Named {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    abstract class constructor {
                        private constructor();
                    }
                }
            }
        }
        abstract class EnumWithNestedTypealiases {
            private constructor();
            static get X(): foo.EnumWithNestedTypealiases & {
                get name(): "X";
                get ordinal(): 0;
            };
            static get Y(): foo.EnumWithNestedTypealiases & {
                get name(): "Y";
                get ordinal(): 1;
            };
            static get Z(): foo.EnumWithNestedTypealiases & {
                get name(): "Z";
                get ordinal(): 2;
            };
            static values(): [typeof foo.EnumWithNestedTypealiases.X, typeof foo.EnumWithNestedTypealiases.Y, typeof foo.EnumWithNestedTypealiases.Z];
            static valueOf(value: string): foo.EnumWithNestedTypealiases;
            get name(): "X" | "Y" | "Z";
            get ordinal(): 0 | 1 | 2;
        }
        namespace EnumWithNestedTypealiases {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => EnumWithNestedTypealiases;
            }
        }
        class OpenClassWithNestedTypealiases {
            constructor();
        }
        namespace OpenClassWithNestedTypealiases {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => OpenClassWithNestedTypealiases;
            }
        }
        abstract class AbstractClassWithNestedTypealiases {
            constructor();
        }
        namespace AbstractClassWithNestedTypealiases {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => AbstractClassWithNestedTypealiases;
            }
        }
        class OuterWithNested {
            constructor();
        }
        namespace OuterWithNested {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => OuterWithNested;
            }
            class InnerNested {
                constructor();
            }
            namespace InnerNested {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => InnerNested;
                }
            }
        }
        abstract class SealedClassWithNestedTypealiases {
            private constructor();
        }
        namespace SealedClassWithNestedTypealiases {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => SealedClassWithNestedTypealiases;
            }
            class Sub extends foo.SealedClassWithNestedTypealiases.$metadata$.constructor {
                constructor();
            }
            namespace Sub {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => Sub;
                }
            }
        }
        interface SealedInterfaceWithNestedTypealiases {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.SealedInterfaceWithNestedTypealiases": unique symbol;
            };
        }
        namespace SealedInterfaceWithNestedTypealiases {
            class Impl implements foo.SealedInterfaceWithNestedTypealiases {
                constructor();
                readonly __doNotUseOrImplementIt: foo.SealedInterfaceWithNestedTypealiases["__doNotUseOrImplementIt"];
            }
            namespace Impl {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    const constructor: abstract new () => Impl;
                }
            }
        }
        class ClassWithIgnoredNestedTypealias {
            constructor();
        }
        namespace ClassWithIgnoredNestedTypealias {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ClassWithIgnoredNestedTypealias;
            }
        }
    }
    namespace foo {
        function consumeMyInt(value: number): number;
        function consumeCallback(cb: Nullable<() => void>): Nullable<void>;
        function consumeGenericAlias(value: foo.GenericClass<string>): string;
        class ClassUsingAlias {
            constructor(id: number, name: string);
            get id(): number;
            get name(): string;
        }
        namespace ClassUsingAlias {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ClassUsingAlias;
            }
        }
        function acceptInnerAlias(value: foo.GenericClass.Inner<string, number>): void;
    }
}

declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace kotlin {
        class Pair<out A, out B> /* implements kotlin.io.Serializable */ {
            constructor(first: A, second: B);
            toString(): string;
            copy(first?: A, second?: B): kotlin.Pair<A, B>;
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            get first(): A;
            get second(): B;
        }
        namespace Pair {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <A, B>() => Pair<A, B>;
            }
        }
    }
    namespace foo {
        function consumeMyInt(value: foo.MyInt): foo.MyInt;
        function consumeCallback(cb: Nullable<foo.SimpleCallback>): Nullable<void>;
        function consumeGenericAlias(value: foo.AliasGenericClass<string>): string;
        function acceptInnerAlias(value: foo.AliasWithInnerClass<number, string>): void;
        class ClassUsingAlias {
            constructor(id: foo.MyInt, name: foo.MyString);
            get id(): foo.MyInt;
            get name(): foo.MyString;
        }
        namespace ClassUsingAlias {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ClassUsingAlias;
            }
        }
        type AliasWithVarianceInAliasedType<T> = foo.GenericClass<T>;
        type AliasWithVarianceInsideOfAliasedType<T> = foo.GenericClassWithVariance<T>;
        type AliasWithInnerClass<T, S> = foo.GenericClass.Inner<S, T>;
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
                new<S>(): foo.GenericClass.Inner<S, T>;
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
        interface SomeExternalInterface {
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
                new<S extends foo.SomeEnum>(): foo.ClassWithConstraint.InnerWithConstraints<S, T>;
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
            type NestedInt = number;
            type NestedString = string;
            type NestedClassAlias = foo.SomeClass;
            type NestedGenericAlias<T> = foo.GenericClass<T>;
            type NestedConcreteGenericAlias = foo.GenericClass<string>;
            type NestedCallback = () => void;
            type NestedNullable = Nullable<number>;
            type Self = foo.ClassWithNestedTypealiases;
        }
        class GenericClassWithNestedTypealiases<T> {
            constructor();
        }
        namespace GenericClassWithNestedTypealiases {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T>() => GenericClassWithNestedTypealiases<T>;
            }
            type NestedAlias = number;
            type NestedGenericAlias<T, R> = foo.TwoGenericParamsClass<T, R>;
            type Self<T> = foo.GenericClassWithNestedTypealiases<T>;
        }
        interface InterfaceWithNestedTypealiases {
            readonly __doNotUseOrImplementIt: {
                readonly "foo.InterfaceWithNestedTypealiases": unique symbol;
            };
        }
        namespace InterfaceWithNestedTypealiases {
            type InterfaceNestedInt = number;
            type InterfaceNestedClassAlias = foo.SomeClass;
            type InterfaceNestedGenericAlias<T> = foo.GenericClass<T>;
            type InterfaceNestedCallback = (p0: number) => string;
            type Self = foo.InterfaceWithNestedTypealiases;
        }
        abstract class ObjectWithNestedTypealiases extends KtSingleton<ObjectWithNestedTypealiases.$metadata$.constructor>() {
            private constructor();
        }
        namespace ObjectWithNestedTypealiases {
            type ObjectNestedInt = number;
            type ObjectNestedClassAlias = foo.SomeClass;
            type ObjectNestedGenericAlias<T> = foo.GenericClass<T>;
            type Self = typeof foo.ObjectWithNestedTypealiases;
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
                type CompanionNestedInt = number;
                type CompanionNestedString = string;
                type CompanionNestedGenericAlias<T> = foo.GenericClass<T>;
                type Self = typeof foo.ClassWithCompanionTypealiases.Companion;
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
                type NamedCompanionNestedAlias = string;
                type NamedCompanionGenericAlias<T> = foo.GenericClass<T>;
                type Self = typeof foo.ClassWithNamedCompanionTypealiases.Named;
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
            type EnumNestedInt = number;
            type EnumNestedClassAlias = foo.SomeClass;
            type Self = foo.EnumWithNestedTypealiases;
        }
        class OpenClassWithNestedTypealiases {
            constructor();
        }
        namespace OpenClassWithNestedTypealiases {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => OpenClassWithNestedTypealiases;
            }
            type OpenClassNestedAlias = string;
            type OpenClassNestedGenericAlias<T> = foo.GenericClass<T>;
        }
        abstract class AbstractClassWithNestedTypealiases {
            constructor();
        }
        namespace AbstractClassWithNestedTypealiases {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => AbstractClassWithNestedTypealiases;
            }
            type AbstractNestedAlias = number;
            type AbstractNestedClassAlias = foo.SomeClass;
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
                type DeeplyNestedAlias = number;
                type DeeplyNestedClassAlias = foo.SomeClass;
            }
            type OuterNestedAlias = string;
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
            type SealedNestedAlias = number;
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
            type SealedInterfaceNestedAlias = string;
        }
        class ClassWithIgnoredNestedTypealias {
            constructor();
        }
        namespace ClassWithIgnoredNestedTypealias {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ClassWithIgnoredNestedTypealias;
            }
            type VisibleAlias = number;
        }
        type MyInt = number;
        type MyString = string;
        type MyBoolean = boolean;
        type MyDouble = number;
        type MyByte = number;
        type MyShort = number;
        type MyFloat = number;
        type MyChar = any/* kotlin.Char */;
        type MyAny = any;
        type NullableInt = Nullable<number>;
        type NullableString = Nullable<string>;
        type NullableAny = Nullable<any>;
        type MyUByte = any/* kotlin.UByte */;
        type MyUShort = any/* kotlin.UShort */;
        type MyUInt = any/* kotlin.UInt */;
        type MyIntArray = Int32Array;
        type MyBooleanArray = any /*BooleanArray*/;
        type MyStringArray = Array<string>;
        type MyNullableIntArray = Nullable<Int32Array>;
        type NestedArray = Array<Array<string>>;
        type AliasSomeClass = foo.SomeClass;
        type AliasSomeInterface = foo.SomeInterface;
        type AliasSomeExternalInterface = foo.SomeExternalInterface;
        type AliasSomeEnum = foo.SomeEnum;
        type NullableSomeClass = Nullable<foo.SomeClass>;
        type ConcreteGenericClass = foo.GenericClass<string>;
        type ConcreteGenericClassInt = foo.GenericClass<number>;
        type ConcreteTwoGenericParamsClass = foo.TwoGenericParamsClass<string, number>;
        type AliasGenericClass<T> = foo.GenericClass<T>;
        type AliasTwoGenericParamsClass<A, B> = foo.TwoGenericParamsClass<A, B>;
        type FlippedTwoGenericParamsClass<A, B> = foo.TwoGenericParamsClass<B, A>;
        type PartiallySpecializedGenericClass<T> = foo.TwoGenericParamsClass<string, T>;
        type AliasClassWithConstraint<T extends foo.SomeClass> = foo.ClassWithConstraint<T>;
        type AliasClassWithMultipleConstraints<T extends foo.Comparable<T> & foo.SomeClass & foo.SomeEnum> = kotlin.Pair<foo.RecursiveBoundClass<T>, foo.ClassWithConstraint.InnerWithConstraints<T, T>>;
        type AliasRecursiveBound<T extends foo.Comparable<T>> = foo.RecursiveBoundClass<T>;
        type AliasOfConstrainedAlias<T extends foo.SomeClass> = foo.AliasClassWithConstraint<T>;
        type NullableGenericClass<T> = Nullable<foo.GenericClass<T>>;
        type GenericClassNullableParam<T> = foo.GenericClass<Nullable<T>>;
        type SimpleCallback = () => void;
        type IntToString = (p0: number) => string;
        type BinaryOperation = (p0: number, p1: number) => number;
        type GenericTransformer<T, R> = (p0: T) => R;
        type NullableCallback = Nullable<() => void>;
        type CallbackWithNullableParam = (p0: Nullable<number>) => Nullable<string>;
        type HigherOrderFunction = (p0: (p0: number) => string) => number;
        type CurriedFunction = (p0: number) => (p0: string) => boolean;
        type SuspendCallback = () => Promise<void>;
        type SuspendTransformer<T, R> = (p0: T) => Promise<R>;
        type AliasOfMyInt = foo.MyInt;
        type AliasOfAliasSomeClass = foo.AliasSomeClass;
        type AliasOfGenericAlias<T> = foo.AliasGenericClass<T>;
        type MyThrowable = Error;
        type NullableThrowable = Nullable<Error>;
        type MyNothing = never;
        type MyUnit = void;
    }
}



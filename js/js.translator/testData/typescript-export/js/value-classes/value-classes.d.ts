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
        interface KtMap<K, out V> {
            asJsReadonlyMapView(): ReadonlyMap<K, V>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtMap": unique symbol;
            };
        }
        namespace KtMap {
            function fromJsMap<K, V>(map: ReadonlyMap<K, V>): kotlin.collections.KtMap<K, V>;
        }
        interface KtSet<out E> /* extends kotlin.collections.Collection<E> */ {
            asJsReadonlySetView(): ReadonlySet<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtSet": unique symbol;
            };
        }
        namespace KtSet {
            function fromJsSet<E>(set: ReadonlySet<E>): kotlin.collections.KtSet<E>;
        }
        interface KtMutableList<E> extends kotlin.collections.KtList<E>/*, kotlin.collections.MutableCollection<E> */ {
            asJsArrayView(): Array<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtMutableList": unique symbol;
            } & kotlin.collections.KtList<any>["__doNotUseOrImplementIt"];
        }
        namespace KtMutableList {
            function fromJsArray<E>(array: ReadonlyArray<E>): kotlin.collections.KtMutableList<E>;
        }
    }
    namespace kotlin {
        class Pair<out A, out B> /* implements kotlin.io.Serializable */ {
            constructor(first: A, second: B);
            get first(): A;
            get second(): B;
            toString(): string;
            copy(first?: A, second?: B): kotlin.Pair<A, B>;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace Pair {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <A, B>() => Pair<A, B>;
            }
        }
        class Triple<out A, out B, out C> /* implements kotlin.io.Serializable */ {
            constructor(first: A, second: B, third: C);
            get first(): A;
            get second(): B;
            get third(): C;
            toString(): string;
            copy(first?: A, second?: B, third?: C): kotlin.Triple<A, B, C>;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace Triple {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <A, B, C>() => Triple<A, B, C>;
            }
        }
    }
    namespace foo {
        interface ExternalInterface {
            readonly intValue: number;
            readonly stringValue: string;
            readonly wrappedStringValue: string;
            readonly nullableValue: Nullable<string>;
            readonly nullableNullableValue?: Nullable<foo.NullableValueClass>;
            readonly genericValue: Array<string>;
            readonly genericOfGeneric: foo.GenericValueClass<foo.GenericValueClass<string>>;
            acceptIntValue(value: number): number;
            acceptStringValue(value: string): string;
            readonly arrayOfIntValue: Array<foo.IntValueClass>;
            readonly promiseOfStringValue: Promise<foo.StringValueClass>;
            acceptLambda(cb: (p0: foo.IntValueClass) => void): void;
        }
    }
    namespace foo {
        class IntValueClass {
            constructor(value: number);
            get value(): number;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace IntValueClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => IntValueClass;
            }
        }
        class StringValueClass {
            constructor(name: string);
            get name(): string;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace StringValueClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => StringValueClass;
            }
        }
        class DoubleValueClass {
            constructor(amount: number);
            get amount(): number;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace DoubleValueClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => DoubleValueClass;
            }
        }
        class BooleanValueClass {
            constructor(flag: boolean);
            get flag(): boolean;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace BooleanValueClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => BooleanValueClass;
            }
        }
        class NullableValueClass {
            constructor(data: Nullable<string>);
            get data(): Nullable<string>;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace NullableValueClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => NullableValueClass;
            }
        }
        class GenericValueClass<T> {
            constructor(item: T);
            get item(): T;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace GenericValueClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T>() => GenericValueClass<T>;
            }
        }
        class ValueClassWithMethods {
            constructor(number: number);
            get number(): number;
            double(): number;
            add(other: number): number;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace ValueClassWithMethods {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ValueClassWithMethods;
            }
        }
        class ValueClassWithCompanion {
            constructor(value: string);
            get value(): string;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace ValueClassWithCompanion {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ValueClassWithCompanion;
            }
            abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
                private constructor();
            }
            namespace Companion {
                /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
                namespace $metadata$ {
                    abstract class constructor {
                        get DEFAULT(): string;
                        create(s: string): foo.ValueClassWithCompanion;
                        private constructor();
                    }
                }
            }
        }
        function acceptValueClass(v: foo.IntValueClass): number;
        function createValueClass(x: number): foo.IntValueClass;
        function combineValueClasses(a: foo.IntValueClass, b: foo.IntValueClass): number;
        function useGenericValueClass<T>(g: foo.GenericValueClass<T>): T;
        class NestedValueClass {
            constructor(inner: foo.IntValueClass);
            get inner(): foo.IntValueClass;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace NestedValueClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => NestedValueClass;
            }
        }
        class ClassWithValueProperty {
            constructor(data: foo.StringValueClass);
            get data(): foo.StringValueClass;
        }
        namespace ClassWithValueProperty {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ClassWithValueProperty;
            }
        }
        class ClassWithValueMethods {
            constructor();
            produceValue(): foo.IntValueClass;
            consumeValue(v: foo.IntValueClass): number;
        }
        namespace ClassWithValueMethods {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ClassWithValueMethods;
            }
        }
        function createValueArray(): Array<foo.IntValueClass>;
        function acceptNullableValueClass(v: Nullable<foo.IntValueClass>): Nullable<number>;
        function compareValueClasses(a: foo.IntValueClass, b: foo.IntValueClass): boolean;
        interface HasValue {
            readonly value: number;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.HasValue": unique symbol;
            };
        }
        class ValueClassWithInterface implements foo.HasValue {
            constructor(value: number);
            get value(): number;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
            readonly __doNotUseOrImplementIt: foo.HasValue["__doNotUseOrImplementIt"];
        }
        namespace ValueClassWithInterface {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ValueClassWithInterface;
            }
        }
        class ValueClassWithConstructors {
            constructor(data: string);
            get data(): string;
            static createFromNumber(number: number): foo.ValueClassWithConstructors;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace ValueClassWithConstructors {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ValueClassWithConstructors;
            }
        }
        function createValueClassList(): kotlin.collections.KtList<foo.IntValueClass>;
        function createValueClassSet(): kotlin.collections.KtSet<foo.StringValueClass>;
        function createValueClassMap(): kotlin.collections.KtMap<foo.IntValueClass, foo.StringValueClass>;
        function acceptValueClassList(list: kotlin.collections.KtList<foo.IntValueClass>): number;
        function acceptValueClassArray(arr: Array<foo.IntValueClass>): number;
        function mixedCollection(): kotlin.collections.KtList<any>;
        class ClassWithValueCollections {
            constructor();
            get list(): kotlin.collections.KtList<foo.IntValueClass>;
            get array(): Array<foo.StringValueClass>;
            get mutableList(): kotlin.collections.KtMutableList<foo.IntValueClass>;
            set mutableList(value: kotlin.collections.KtMutableList<foo.IntValueClass>);
            addToList(v: foo.IntValueClass): void;
            getListSize(): number;
        }
        namespace ClassWithValueCollections {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ClassWithValueCollections;
            }
        }
        function nestedValueClassCollection(): kotlin.collections.KtList<kotlin.collections.KtList<foo.IntValueClass>>;
        class ValueClassWithCollection {
            constructor(items: kotlin.collections.KtList<number>);
            get items(): kotlin.collections.KtList<number>;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace ValueClassWithCollection {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ValueClassWithCollection;
            }
        }
        function createValueClassWithCollection(): foo.ValueClassWithCollection;
        function useValueClassAsMapKey(map: kotlin.collections.KtMap<foo.IntValueClass, string>): Nullable<string>;
        function useValueClassAsMapValue(map: kotlin.collections.KtMap<string, foo.IntValueClass>): Nullable<number>;
        function createPairWithValueClass(): kotlin.Pair<foo.IntValueClass, foo.StringValueClass>;
        function createTripleWithValueClass(): kotlin.Triple<foo.IntValueClass, foo.StringValueClass, foo.BooleanValueClass>;
        function acceptPairWithValueClass(pair: kotlin.Pair<foo.IntValueClass, foo.IntValueClass>): number;
        class WrappedStringValueClass {
            constructor(s: foo.StringValueClass);
            get s(): foo.StringValueClass;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace WrappedStringValueClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => WrappedStringValueClass;
            }
        }
    }
}

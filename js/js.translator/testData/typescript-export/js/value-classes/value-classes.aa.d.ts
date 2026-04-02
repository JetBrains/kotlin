declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);


    namespace foo {
        function acceptValueClass(v: foo.IntValueClass): number;
        function createValueClass(x: number): foo.IntValueClass;
        function combineValueClasses(a: foo.IntValueClass, b: foo.IntValueClass): number;
        function useGenericValueClass<T>(g: foo.GenericValueClass<T>): T;
        function createValueArray(): Array<foo.IntValueClass>;
        function acceptNullableValueClass(v: Nullable<foo.IntValueClass>): Nullable<number>;
        function compareValueClasses(a: foo.IntValueClass, b: foo.IntValueClass): boolean;
        function createValueClassList(): any/* kotlin.collections.List<foo.IntValueClass> */;
        function createValueClassSet(): any/* kotlin.collections.Set<foo.StringValueClass> */;
        function createValueClassMap(): any/* kotlin.collections.Map<foo.IntValueClass, foo.StringValueClass> */;
        function acceptValueClassList(list: any/* kotlin.collections.List<foo.IntValueClass> */): number;
        function acceptValueClassArray(arr: Array<foo.IntValueClass>): number;
        function mixedCollection(): any/* kotlin.collections.List<any> */;
        function nestedValueClassCollection(): any/* kotlin.collections.List<kotlin.collections.List<foo.IntValueClass>> */;
        function createValueClassWithCollection(): foo.ValueClassWithCollection;
        function useValueClassAsMapKey(map: any/* kotlin.collections.Map<foo.IntValueClass, string> */): Nullable<string>;
        function useValueClassAsMapValue(map: any/* kotlin.collections.Map<string, foo.IntValueClass> */): Nullable<number>;
        function createPairWithValueClass(): kotlin.Pair<foo.IntValueClass, foo.StringValueClass>;
        function createTripleWithValueClass(): kotlin.Triple<foo.IntValueClass, foo.StringValueClass, foo.BooleanValueClass>;
        function acceptPairWithValueClass(pair: kotlin.Pair<foo.IntValueClass, foo.IntValueClass>): number;
        class IntValueClass {
            constructor(value: number);
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
            get value(): number;
        }
        namespace IntValueClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => IntValueClass;
            }
        }
        class StringValueClass {
            constructor(name: string);
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
            get name(): string;
        }
        namespace StringValueClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => StringValueClass;
            }
        }
        class DoubleValueClass {
            constructor(amount: number);
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
            get amount(): number;
        }
        namespace DoubleValueClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => DoubleValueClass;
            }
        }
        class BooleanValueClass {
            constructor(flag: boolean);
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
            get flag(): boolean;
        }
        namespace BooleanValueClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => BooleanValueClass;
            }
        }
        class NullableValueClass {
            constructor(data: Nullable<string>);
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
            get data(): Nullable<string>;
        }
        namespace NullableValueClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => NullableValueClass;
            }
        }
        class GenericValueClass<T> {
            constructor(item: T);
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
            get item(): T;
        }
        namespace GenericValueClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new <T>() => GenericValueClass<T>;
            }
        }
        class ValueClassWithMethods {
            constructor(number: number);
            double(): number;
            add(other: number): number;
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
            get number(): number;
        }
        namespace ValueClassWithMethods {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ValueClassWithMethods;
            }
        }
        class ValueClassWithCompanion {
            constructor(value: string);
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
            get value(): string;
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
                        create(s: string): foo.ValueClassWithCompanion;
                        get DEFAULT(): string;
                        private constructor();
                    }
                }
            }
        }
        class NestedValueClass {
            constructor(inner: foo.IntValueClass);
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
            get inner(): foo.IntValueClass;
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
        interface HasValue {
            readonly value: number;
            readonly __doNotUseOrImplementIt: {
                readonly "foo.HasValue": unique symbol;
            };
        }
        class ValueClassWithInterface implements foo.HasValue {
            constructor(value: number);
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
            get value(): number;
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
            static createFromNumber(number: number): foo.ValueClassWithConstructors;
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
            get data(): string;
        }
        namespace ValueClassWithConstructors {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ValueClassWithConstructors;
            }
        }
        class ClassWithValueCollections {
            constructor();
            addToList(v: foo.IntValueClass): void;
            getListSize(): number;
            get list(): any/* kotlin.collections.List<foo.IntValueClass> */;
            get array(): Array<foo.StringValueClass>;
            get mutableList(): any/* kotlin.collections.MutableList<foo.IntValueClass> */;
            set mutableList(value: any/* kotlin.collections.MutableList<foo.IntValueClass> */);
        }
        namespace ClassWithValueCollections {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ClassWithValueCollections;
            }
        }
        class ValueClassWithCollection {
            constructor(items: any/* kotlin.collections.List<number> */);
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
            get items(): any/* kotlin.collections.List<number> */;
        }
        namespace ValueClassWithCollection {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ValueClassWithCollection;
            }
        }
        class WrappedStringValueClass {
            constructor(s: foo.StringValueClass);
            equals(other: Nullable<any>): boolean;
            hashCode(): number;
            toString(): string;
            get s(): foo.StringValueClass;
        }
        namespace WrappedStringValueClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => WrappedStringValueClass;
            }
        }
        interface ExternalInterface {
            acceptIntValue(value: number): number;
            acceptStringValue(value: string): string;
            acceptLambda(cb: (p0: foo.IntValueClass) => void): void;
            readonly intValue: number;
            readonly stringValue: string;
            readonly wrappedStringValue: string;
            readonly nullableValue: Nullable<string>;
            readonly nullableNullableValue?: Nullable<foo.NullableValueClass>;
            readonly genericValue: Array<string>;
            readonly genericOfGeneric: foo.GenericValueClass<foo.GenericValueClass<string>>;
            readonly arrayOfIntValue: Array<foo.IntValueClass>;
            readonly promiseOfStringValue: Promise<foo.StringValueClass>;
        }
    }
}

declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    const __doNotImplementIt: unique symbol
    type __doNotImplementIt = typeof __doNotImplementIt
    namespace foo {
        const _val: number;
        let _var: number;
        const _valCustomWithField: number;
        let _varCustomWithField: number;
        function sum(x: number, y: number): number;
        function varargInt(x: Int32Array): number;
        function varargNullableInt(x: Array<Nullable<number>>): number;
        function varargWithOtherParameters(x: string, y: Array<string>, z: string): number;
        function varargWithComplexType(x: Array<(p0: Array<Int32Array>) => Array<Int32Array>>): number;
        function sumNullable(x: Nullable<number>, y: Nullable<number>): number;
        function defaultParameters(a: string, x?: number, y?: string): string;
        function generic1<T>(x: T): T;
        function generic2<T>(x: Nullable<T>): boolean;
        function genericWithConstraint<T extends string>(x: T): T;
        function genericWithMultipleConstraints<T extends foo.TestInterface & Error>(x: T): T;
        function generic3<A, B, C, D, E>(a: A, b: B, c: C, d: D): Nullable<E>;
        function inlineFun(x: number, callback: (p0: number) => void): void;
        const _const_val: number;
        const _valCustom: number;
        let _varCustom: number;
        class A {
            constructor();
        }
        class A1 {
            constructor(x: number);
            get x(): number;
        }
        class A2 {
            constructor(x: string, y: boolean);
            get x(): string;
            get y(): boolean;
            set y(value: boolean);
        }
        class A3 {
            constructor();
            get x(): number;
        }
        class A4 {
            constructor();
            get _valCustom(): number;
            get _valCustomWithField(): number;
            get _varCustom(): number;
            set _varCustom(value: number);
            get _varCustomWithField(): number;
            set _varCustomWithField(value: number);
        }
        class A5<T> {
            constructor(value: T);
            get value(): T;
            test(): T;
        }
        const O0: {
        };
        const O: {
            get x(): number;
            foo(): number;
        };
        function takesO(o: typeof foo.O): number;
        class KT_37829 {
            constructor();
            static get Companion(): {
                get x(): number;
            };
        }
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
        class TestDataClass {
            constructor(name: string);
            get name(): string;
            component1(): string;
            copy(name?: string): foo.TestDataClass;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
        namespace TestDataClass {
            class Nested {
                constructor();
                get prop(): string;
            }
        }
        abstract class TestEnumClass {
            private constructor();
            get constructorParameter(): string;
            static get A(): foo.TestEnumClass & {
                get name(): "A";
                get ordinal(): 0;
            };
            static get B(): foo.TestEnumClass & {
                get name(): "B";
                get ordinal(): 1;
            };
            get foo(): number;
            bar(value: string): string;
            bay(): string;
            static values(): Array<foo.TestEnumClass>;
            static valueOf(value: string): foo.TestEnumClass;
            get name(): "A" | "B";
            get ordinal(): 0 | 1;
        }
        namespace TestEnumClass {
            class Nested {
                constructor();
                get prop(): string;
            }
        }
        interface TestInterface {
            readonly value: string;
            getOwnerName(): string;
            readonly __doNotUseIt: __doNotImplementIt;
        }
        class TestInterfaceImpl implements foo.TestInterface {
            constructor(value: string);
            get value(): string;
            getOwnerName(): string;
            readonly __doNotUseIt: __doNotImplementIt;
        }
        function processInterface(test: foo.TestInterface): string;
        class OuterClass {
            constructor();
        }
        namespace OuterClass {
            abstract class NestedEnum {
                private constructor();
                static get A(): foo.OuterClass.NestedEnum & {
                    get name(): "A";
                    get ordinal(): 0;
                };
                static get B(): foo.OuterClass.NestedEnum & {
                    get name(): "B";
                    get ordinal(): 1;
                };
                static values(): Array<foo.OuterClass.NestedEnum>;
                static valueOf(value: string): foo.OuterClass.NestedEnum;
                get name(): "A" | "B";
                get ordinal(): 0 | 1;
            }
        }
        class KT38262 {
            constructor();
            then(): number;
            catch(): number;
        }
        class JsNameTest {
            private constructor();
            get value(): number;
            runTest(): string;
            static get Companion(): {
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
        class KT39423 {
            constructor(a: string, b?: Nullable<number>);
            get a(): string;
            get b(): Nullable<number>;
            component1(): string;
            component2(): Nullable<number>;
            copy(a?: string, b?: Nullable<number>): foo.KT39423;
            toString(): string;
            hashCode(): number;
            equals(other: Nullable<any>): boolean;
        }
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
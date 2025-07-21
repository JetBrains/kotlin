declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        interface SomeExternalInterface {
        }
    }
    namespace foo {
        function sum(x: number, y: number): Promise<number>;
        function varargInt(x: Int32Array): Promise<number>;
        function varargNullableInt(x: Array<Nullable<number>>): Promise<number>;
        function varargWithOtherParameters(x: string, y: Array<string>, z: string): Promise<number>;
        function varargWithComplexType(x: Array<(p0: Array<Int32Array>) => Array<Int32Array>>): Promise<number>;
        function sumNullable(x: Nullable<number>, y: Nullable<number>): Promise<number>;
        function defaultParameters(a: string, x?: number, y?: string): Promise<string>;
        function generic1<T>(x: T): Promise<T>;
        function generic2<T>(x: Nullable<T>): Promise<boolean>;
        function genericWithConstraint<T extends string>(x: T): Promise<T>;
        function genericWithMultipleConstraints<T extends unknown/* kotlin.Comparable<T> */ & foo.SomeExternalInterface & Error>(x: T): Promise<T>;
        function generic3<A, B, C, D, E>(a: A, b: B, c: C, d: D): Promise<Nullable<E>>;
        function inlineFun(x: number, callback: (p0: number) => void): Promise<void>;
        class Test {
            constructor();
            sum(x: number, y: number): Promise<number>;
            varargInt(x: Int32Array): Promise<number>;
            varargNullableInt(x: Array<Nullable<number>>): Promise<number>;
            varargWithOtherParameters(x: string, y: Array<string>, z: string): Promise<number>;
            varargWithComplexType(x: Array<(p0: Array<Int32Array>) => Array<Int32Array>>): Promise<number>;
            sumNullable(x: Nullable<number>, y: Nullable<number>): Promise<number>;
            defaultParameters(a: string, x?: number, y?: string): Promise<string>;
            generic1<T>(x: T): Promise<T>;
            generic2<T>(x: Nullable<T>): Promise<boolean>;
            genericWithConstraint<T extends string>(x: T): Promise<T>;
            genericWithMultipleConstraints<T extends unknown/* kotlin.Comparable<T> */ & foo.SomeExternalInterface & Error>(x: T): Promise<T>;
            generic3<A, B, C, D, E>(a: A, b: B, c: C, d: D): Promise<Nullable<E>>;
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace Test.$metadata$ {
            const constructor: abstract new () => Test;
        }
        class TestChild extends foo.Test.$metadata$.constructor {
            constructor();
            varargInt(x: Int32Array): Promise<number>;
            sumNullable(x: Nullable<number>, y: Nullable<number>): Promise<number>;
            generic3<A, B, C, D, E>(a: A, b: B, c: C, d: D): Promise<Nullable<E>>;
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace TestChild.$metadata$ {
            const constructor: abstract new () => TestChild;
        }
        function acceptTest(test: foo.Test): Promise<void>;
    }
}

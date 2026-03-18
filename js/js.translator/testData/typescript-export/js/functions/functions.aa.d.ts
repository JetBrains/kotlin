declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);


    namespace foo {
        function sum(x: number, y: number): number;
        function varargByte(x: Int8Array): number;
        function varargShort(x: Int16Array): number;
        function varargInt(x: Int32Array): number;
        function varargFloat(x: Float32Array): number;
        function varargDouble(x: Float64Array): number;
        function varargBoolean(x: any /*BooleanArray*/): number;
        function varargChar(x: any /*CharArray*/): number;
        function varargUByte(x: Array<any/* kotlin.UByte */>): number;
        function varargUShort(x: Array<any/* kotlin.UShort */>): number;
        function varargUInt(x: Array<any/* kotlin.UInt */>): number;
        function varargNullableInt(x: Array<Nullable<number>>): number;
        function varargWithOtherParameters(x: string, y: Array<string>, z: string): number;
        function varargWithComplexType(x: Array<(p0: Array<Int32Array>) => Array<Int32Array>>): number;
        function sumNullable(x: Nullable<number>, y: Nullable<number>): number;
        function defaultParameters(a: string, x?: number, y?: string): string;
        function generic1<T>(x: T): T;
        function generic2<T>(x: Nullable<T>): boolean;
        function genericWithConstraint<T extends string>(x: T): T;
        function genericWithMultipleConstraints<T extends unknown/* kotlin.Comparable<T> */ & foo.SomeExternalInterface & Error>(x: T): T;
        function generic3<A, B, C, D, E>(a: A, b: B, c: C, d: D): Nullable<E>;
        function inlineFun(x: number, callback: (p0: number) => void): void;
        function formatList(value: any/* kotlin.collections.MutableList<UnknownType *> */): string;
        function createList(): any/* kotlin.collections.MutableList<UnknownType *> */;
        function defaultParametersAtTheBegining(a: string | undefined, b: string): string;
        function nonDefaultParameterInBetween(a: string | undefined, b: string, c?: string): string;
        function concatWithContextParameters(scope1: foo.Scope1, scope2: foo.Scope2): string;
        function concatWithExtensionAndContextParameter(scope1: foo.Scope1, _this_: foo.Scope2): string;
        function getWithExtension(_this_: foo.Scope1): string;
        function allParameters<A, B, C, D, R>(a: A, b: B, c: C, d: D, block: (p0: A, p1: B, _this_: C, d: D) => R): R;
        interface SomeExternalInterface {
        }
        class Scope1 {
            constructor(a: string);
            getA(): string;
            get a(): string;
        }
        namespace Scope1 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Scope1;
            }
        }
        class Scope2 {
            constructor(a: string);
            getA(): string;
            get a(): string;
        }
        namespace Scope2 {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Scope2;
            }
        }
    }
}

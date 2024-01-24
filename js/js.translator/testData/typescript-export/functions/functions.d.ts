declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace kotlin.collections {
        interface KtList<E> /* extends kotlin.collections.Collection<E> */ {
            asJsReadonlyArrayView(): ReadonlyArray<E>;
            readonly __doNotUseOrImplementIt: {
                readonly "kotlin.collections.KtList": unique symbol;
            };
        }
    }
    namespace foo {
        interface SomeExternalInterface {
        }
    }
    namespace foo {
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
        function genericWithMultipleConstraints<T extends unknown/* kotlin.Comparable<T> */ & foo.SomeExternalInterface & Error>(x: T): T;
        function generic3<A, B, C, D, E>(a: A, b: B, c: C, d: D): Nullable<E>;
        function inlineFun(x: number, callback: (p0: number) => void): void;
        function formatList(value: kotlin.collections.KtList<any /*UnknownType **/>): string;
        function createList(): kotlin.collections.KtList<any /*UnknownType **/>;
        function defaultParametersAtTheBegining(a: string | undefined, b: string): string;
        function nonDefaultParameterInBetween(a: string | undefined, b: string, c?: string): string;
    }
}

declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        class Test {
            constructor();
            sum(x: number, y: number): number;
            varargInt(x: Int32Array): number;
            varargNullableInt(x: Array<Nullable<number>>): number;
            varargWithOtherParameters(x: string, y: Array<string>, z: string): number;
            varargWithComplexType(x: Array<(p0: Array<Int32Array>) => Array<Int32Array>>): number;
            sumNullable(x: Nullable<number>, y: Nullable<number>): number;
            defaultParameters(a: string, x?: number, y?: string): string;
            generic1<T>(x: T): T;
            generic2<T>(x: Nullable<T>): boolean;
            genericWithConstraint<T extends string>(x: T): T;
            genericWithMultipleConstraints<T extends RegExpMatchArray & Error>(x: T): T;
            generic3<A, B, C, D, E>(a: A, b: B, c: C, d: D): Nullable<E>;
            inlineFun(x: number, callback: (p0: number) => void): void;
        }
    }
}
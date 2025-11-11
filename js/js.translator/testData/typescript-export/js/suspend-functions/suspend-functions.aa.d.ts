declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

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
        function inlineFun(x: number, callback: (p0: number) => number): Promise<number>;
        function simpleSuspendFun(x: number): Promise<number>;
        function inlineChain(x: number): Promise<number>;
        function suspendExtensionFun(_this_: number): Promise<number>;
        function suspendFunWithContext(ctx: number): Promise<number>;
        function generateOneMoreChildOfTest(): foo.Test;
        function acceptTest(test: foo.Test): Promise<void>;
        function acceptExportedChild(child: foo.ExportedChild): Promise<void>;
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
    }
}

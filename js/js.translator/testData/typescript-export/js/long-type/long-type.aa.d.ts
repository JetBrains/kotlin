declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace kotlin {
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
    }

    namespace foo {
        /* ErrorDeclaration: Top level property declarations are not implemented yet */
        /* ErrorDeclaration: Top level property declarations are not implemented yet */
        /* ErrorDeclaration: Top level property declarations are not implemented yet */
        /* ErrorDeclaration: Top level property declarations are not implemented yet */
        /* ErrorDeclaration: Top level property declarations are not implemented yet */
        /* ErrorDeclaration: Top level property declarations are not implemented yet */
        /* ErrorDeclaration: Top level property declarations are not implemented yet */
        /* ErrorDeclaration: Top level property declarations are not implemented yet */
        /* ErrorDeclaration: Top level property declarations are not implemented yet */
        /* ErrorDeclaration: Top level property declarations are not implemented yet */
        function funWithLongParameters(a: bigint, b: bigint): bigint;
        function funWithLongDefaultParameters(a?: bigint, b?: bigint): bigint;
        function varargLong(x: BigInt64Array): number;
        function varargULong(x: Array<bigint>): number;
        function funWithTypeParameter<T extends bigint>(a: T, b: T): bigint;
        function funWithTypeParameterWithTwoUpperBounds<T extends unknown/* kotlin.Comparable<T> */ & bigint>(a: T, b: T): bigint;
        function funWithContextParameter(long: bigint): bigint;
        function inlineFun(a: bigint, b: bigint): bigint;
        function inlineFunWithTypeParameter<T extends bigint>(a: T, b: T): bigint;
        function inlineFunDefaultParameters(a?: bigint, b?: bigint): bigint;
        function extensionFun(_this_: bigint): bigint;
        function globalFun(a: bigint): bigint;
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
        /* ErrorDeclaration: Class declarations are not implemented yet */
    }
}

declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        const _long: bigint;
        const _long_array: BigInt64Array;
        const _array_long: Array<bigint>;
        const _n_long: Nullable<bigint>;
    }
}

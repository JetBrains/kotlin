declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    namespace foo {
        const _const_val: number;
        const _val: number;
        let _var: number;
        const _valCustom: number;
        const _valCustomWithField: number;
        let _varCustom: number;
        let _varCustomWithField: number;
    }
}

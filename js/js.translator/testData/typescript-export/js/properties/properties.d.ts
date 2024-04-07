declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        const _val: number;
        let _var: number;
        const _valCustomWithField: number;
        let _varCustomWithField: number;
        const _const_val: number;
        const _valCustom: number;
        let _varCustom: number;
    }
}
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
        const valWithExplicitBackingField: any;
        class A {
            constructor();
            get valWithExplicitBackingField(): any;
        }
        namespace A {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => A;
            }
        }
    }
}

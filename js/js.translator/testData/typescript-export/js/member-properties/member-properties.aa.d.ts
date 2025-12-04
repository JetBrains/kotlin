declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    namespace foo {
        class Test {
            constructor();
            get _val(): number;
            get _var(): number;
            set _var(value: number);
            get _valCustom(): number;
            get _valCustomWithField(): number;
            get _varCustom(): number;
            set _varCustom(value: number);
            get _varCustomWithField(): number;
            set _varCustomWithField(value: number);
        }
        namespace Test {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Test;
            }
        }
    }
}

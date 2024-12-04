declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
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
    }
}
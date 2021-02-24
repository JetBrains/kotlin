declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        const prop: number;
        class C {
            constructor(x: number);
            readonly x: number;
            doubleX(): number;
        }
        function box(): string;
    }
}

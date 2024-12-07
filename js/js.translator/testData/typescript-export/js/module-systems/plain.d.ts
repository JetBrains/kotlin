declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        const prop: number;
        class C {
            constructor(x: number);
            get x(): number;
            doubleX(): number;
        }
        function box(): string;
    }
}

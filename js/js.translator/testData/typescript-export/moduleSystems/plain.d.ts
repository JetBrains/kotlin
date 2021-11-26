declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    const __doNotImplementIt: unique symbol
    type __doNotImplementIt = typeof __doNotImplementIt
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

declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    interface publicInterface {
    }
    const publicVal: number;
    function publicFun(): number;
    class publicClass {
        constructor();
    }
    class Class {
        constructor();
        readonly publicVal: number;
        publicFun(): number;
    }
    namespace Class {
        class publicClass {
            constructor();
        }
    }
}

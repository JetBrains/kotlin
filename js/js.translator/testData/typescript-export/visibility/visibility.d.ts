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
        protected readonly protectedVal: number;
        protected protectedFun(): number;
        protected readonly protectedNestedObject: {
        };
        protected readonly Companion: {
            readonly companionObjectProp: number;
        };
        readonly publicVal: number;
        publicFun(): number;
    }
    namespace Class {
        class protectedClass {
            constructor();
        }
        class classWithProtectedConstructors {
            protected constructor();
            protected static createWithString(arg: string): Class.classWithProtectedConstructors;
        }
        class publicClass {
            constructor();
        }
    }
}

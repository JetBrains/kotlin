declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    const __doNotImplementIt: unique symbol
    type __doNotImplementIt = typeof __doNotImplementIt
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
        protected static readonly protectedNestedObject: {
        };
        protected static readonly Companion: {
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

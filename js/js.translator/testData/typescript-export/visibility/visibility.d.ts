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
        protected get protectedVal(): number;
        protected protectedFun(): number;
        get publicVal(): number;
        publicFun(): number;
        protected static get protectedNestedObject(): {
        };
        protected static get Companion(): {
            get companionObjectProp(): number;
        };
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
    abstract class EnumClass {
        private constructor();
        static get EC1(): EnumClass & {
            get name(): "EC1";
            get ordinal(): 0;
        };
        static get EC2(): EnumClass & {
            get name(): "EC2";
            get ordinal(): 1;
        };
        static values(): Array<EnumClass>;
        static valueOf(value: string): EnumClass;
        get name(): "EC1" | "EC2";
        get ordinal(): 0 | 1;
    }
}

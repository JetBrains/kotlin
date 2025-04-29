declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    interface publicInterface {
    }
    const publicVal: number;
    function publicFun(): number;
    class publicClass {
        constructor();
    }
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace publicClass.$metadata$ {
        const constructor: abstract new () => publicClass;
    }
    class Class {
        constructor();
        protected get protectedVal(): number;
        protected protectedFun(): number;
        get publicVal(): number;
        publicFun(): number;
    }
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace Class.$metadata$ {
        const constructor: abstract new () => Class;
    }
    namespace Class {
        class protectedClass {
            constructor();
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace protectedClass.$metadata$ {
            const constructor: abstract new () => protectedClass;
        }
        abstract class protectedNestedObject extends KtSingleton<protectedNestedObject.$metadata$.constructor>() {
            private constructor();
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace protectedNestedObject.$metadata$ {
            abstract class constructor {
                private constructor();
            }
        }
        abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
            private constructor();
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace Companion.$metadata$ {
            abstract class constructor {
                get companionObjectProp(): number;
                private constructor();
            }
        }
        class classWithProtectedConstructors {
            protected constructor();
            protected static createWithString(arg: string): Class.classWithProtectedConstructors;
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace classWithProtectedConstructors.$metadata$ {
            const constructor: abstract new () => classWithProtectedConstructors;
        }
        class publicClass {
            constructor();
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace publicClass.$metadata$ {
            const constructor: abstract new () => publicClass;
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
        get name(): "EC1" | "EC2";
        get ordinal(): 0 | 1;
        static values(): Array<EnumClass>;
        static valueOf(value: string): EnumClass;
    }
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace EnumClass.$metadata$ {
        const constructor: abstract new () => EnumClass;
    }
}

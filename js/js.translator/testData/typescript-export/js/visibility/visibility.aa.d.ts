declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);

    const publicVal: number;
    function publicFun(): number;
    class publicClass {
        constructor();
    }
    namespace publicClass {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => publicClass;
        }
    }
    interface publicInterface {
    }
    abstract class AbstractClassWithProtected {
        constructor();
        protected abstract protectedAbstractFun(): number;
        protected abstract get protectedAbstractVal(): number;
    }
    namespace AbstractClassWithProtected {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => AbstractClassWithProtected;
        }
    }
    class Class extends AbstractClassWithProtected.$metadata$.constructor {
        constructor();
        protected protectedFun(): number;
        publicFun(): number;
        protected protectedAbstractFun(): number;
        protected get protectedVal(): number;
        get publicVal(): number;
        protected get protectedAbstractVal(): number;
    }
    namespace Class {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => Class;
        }
        class protectedClass {
            constructor();
        }
        namespace protectedClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => protectedClass;
            }
        }
        abstract class protectedNestedObject extends KtSingleton<protectedNestedObject.$metadata$.constructor>() {
            private constructor();
        }
        namespace protectedNestedObject {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor {
                    private constructor();
                }
            }
        }
        abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
            private constructor();
        }
        namespace Companion {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                abstract class constructor {
                    get companionObjectProp(): number;
                    private constructor();
                }
            }
        }
        class classWithProtectedConstructors {
            private constructor();
        }
        namespace classWithProtectedConstructors {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => classWithProtectedConstructors;
            }
        }
        class publicClass {
            constructor();
        }
        namespace publicClass {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => publicClass;
            }
        }
    }
    class FinalClass extends AbstractClassWithProtected.$metadata$.constructor {
        private constructor();
        static fromString(s: string): FinalClass;
    }
    namespace FinalClass {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => FinalClass;
        }
    }
    class FinalClassWithPublicPrimaryProtectedSecondaryCtor {
        constructor(s: string);
    }
    namespace FinalClassWithPublicPrimaryProtectedSecondaryCtor {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => FinalClassWithPublicPrimaryProtectedSecondaryCtor;
        }
    }
    class FinalClassWithProtectedPrimaryPublicSecondaryCtor {
        private constructor();
        static fromInt(n: number): FinalClassWithProtectedPrimaryPublicSecondaryCtor;
    }
    namespace FinalClassWithProtectedPrimaryPublicSecondaryCtor {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => FinalClassWithProtectedPrimaryPublicSecondaryCtor;
        }
    }
    class FinalClassWithOnlySecondaryCtorsMixedVisibility {
        private constructor();
        static fromString(s: string): FinalClassWithOnlySecondaryCtorsMixedVisibility;
    }
    namespace FinalClassWithOnlySecondaryCtorsMixedVisibility {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => FinalClassWithOnlySecondaryCtorsMixedVisibility;
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
        static values(): [typeof EnumClass.EC1, typeof EnumClass.EC2];
        static valueOf(value: string): EnumClass;
        get name(): "EC1" | "EC2";
        get ordinal(): 0 | 1;
    }
    namespace EnumClass {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => EnumClass;
        }
    }
}

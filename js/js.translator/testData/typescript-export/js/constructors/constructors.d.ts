declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    class ClassWithDefaultCtor {
        constructor();
        get x(): string;
    }
    namespace ClassWithDefaultCtor {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => ClassWithDefaultCtor;
        }
    }
    class ClassWithPrimaryCtor {
        constructor(x: string);
        get x(): string;
    }
    namespace ClassWithPrimaryCtor {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => ClassWithPrimaryCtor;
        }
    }
    class ClassWithSecondaryCtor {
        private constructor();
        get x(): string;
        static create(y: string): ClassWithSecondaryCtor;
    }
    namespace ClassWithSecondaryCtor {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => ClassWithSecondaryCtor;
        }
    }
    class ClassWithMultipleSecondaryCtors {
        private constructor();
        get x(): string;
        static createFromString(y: string): ClassWithMultipleSecondaryCtors;
        static createFromInts(y: number, z: number): ClassWithMultipleSecondaryCtors;
    }
    namespace ClassWithMultipleSecondaryCtors {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => ClassWithMultipleSecondaryCtors;
        }
    }
    class OpenClassWithMixedConstructors {
        constructor(x: string);
        get x(): string;
        static createFromStrings(y: string, z: string): OpenClassWithMixedConstructors;
        static createFromInts(y: number, z: number): OpenClassWithMixedConstructors;
    }
    namespace OpenClassWithMixedConstructors {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => OpenClassWithMixedConstructors;
        }
    }
    class DerivedClassWithSecondaryCtor extends OpenClassWithMixedConstructors.$metadata$.constructor {
        private constructor();
        static delegateToPrimary(y: string): DerivedClassWithSecondaryCtor;
        static delegateToCreateFromInts(y: number, z: number): DerivedClassWithSecondaryCtor;
    }
    namespace DerivedClassWithSecondaryCtor {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => DerivedClassWithSecondaryCtor;
        }
    }
    class GenericClassWithSecondaryCtor<Self extends GenericClassWithSecondaryCtor<Self>> {
        private constructor();
        get x(): string;
        static createFromString<Self extends GenericClassWithSecondaryCtor<Self>>(y: string): GenericClassWithSecondaryCtor<Self>;
    }
    namespace GenericClassWithSecondaryCtor {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new <Self extends GenericClassWithSecondaryCtor<Self>>() => GenericClassWithSecondaryCtor<Self>;
        }
    }
    class KotlinGreeter {
        constructor(greeting?: string);
        get greeting(): string;
    }
    namespace KotlinGreeter {
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace $metadata$ {
            const constructor: abstract new () => KotlinGreeter;
        }
    }
}

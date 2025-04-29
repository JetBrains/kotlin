declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    class ClassWithDefaultCtor {
        constructor();
        get x(): string;
    }
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace ClassWithDefaultCtor.$metadata$ {
        const constructor: abstract new () => ClassWithDefaultCtor;
    }
    class ClassWithPrimaryCtor {
        constructor(x: string);
        get x(): string;
    }
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace ClassWithPrimaryCtor.$metadata$ {
        const constructor: abstract new () => ClassWithPrimaryCtor;
    }
    class ClassWithSecondaryCtor {
        private constructor();
        get x(): string;
        static create(y: string): ClassWithSecondaryCtor;
    }
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace ClassWithSecondaryCtor.$metadata$ {
        const constructor: abstract new () => ClassWithSecondaryCtor;
    }
    class ClassWithMultipleSecondaryCtors {
        private constructor();
        get x(): string;
        static createFromString(y: string): ClassWithMultipleSecondaryCtors;
        static createFromInts(y: number, z: number): ClassWithMultipleSecondaryCtors;
    }
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace ClassWithMultipleSecondaryCtors.$metadata$ {
        const constructor: abstract new () => ClassWithMultipleSecondaryCtors;
    }
    class OpenClassWithMixedConstructors {
        constructor(x: string);
        get x(): string;
        static createFromStrings(y: string, z: string): OpenClassWithMixedConstructors;
        static createFromInts(y: number, z: number): OpenClassWithMixedConstructors;
    }
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace OpenClassWithMixedConstructors.$metadata$ {
        const constructor: abstract new () => OpenClassWithMixedConstructors;
    }
    class DerivedClassWithSecondaryCtor extends OpenClassWithMixedConstructors.$metadata$.constructor {
        private constructor();
        static delegateToPrimary(y: string): DerivedClassWithSecondaryCtor;
        static delegateToCreateFromInts(y: number, z: number): DerivedClassWithSecondaryCtor;
    }
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace DerivedClassWithSecondaryCtor.$metadata$ {
        const constructor: abstract new () => DerivedClassWithSecondaryCtor;
    }
    class KotlinGreeter {
        constructor(greeting?: string);
        get greeting(): string;
    }
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace KotlinGreeter.$metadata$ {
        const constructor: abstract new () => KotlinGreeter;
    }
}

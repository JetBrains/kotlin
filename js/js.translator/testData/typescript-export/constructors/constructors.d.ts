declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    class ClassWithDefaultCtor {
        constructor();
        readonly x: string;
    }
    class ClassWithPrimaryCtor {
        constructor(x: string);
        readonly x: string;
    }
    class ClassWithSecondaryCtor {
        private constructor();
        readonly x: string;
        static create(y: string): ClassWithSecondaryCtor;
    }
    class ClassWithMultipleSecondaryCtors {
        private constructor();
        readonly x: string;
        static createFromString(y: string): ClassWithMultipleSecondaryCtors;
        static createFromInts(y: number, z: number): ClassWithMultipleSecondaryCtors;
    }
    class OpenClassWithMixedConstructors {
        constructor(x: string);
        readonly x: string;
        static createFromStrings(y: string, z: string): OpenClassWithMixedConstructors;
        static createFromInts(y: number, z: number): OpenClassWithMixedConstructors;
    }
    class DerivedClassWithSecondaryCtor extends OpenClassWithMixedConstructors {
        private constructor();
        static delegateToPrimary(y: string): DerivedClassWithSecondaryCtor;
        static delegateToCreateFromInts(y: number, z: number): DerivedClassWithSecondaryCtor;
    }
    class KotlinGreeter {
        constructor(greeting: string);
        readonly greeting: string;
    }
}

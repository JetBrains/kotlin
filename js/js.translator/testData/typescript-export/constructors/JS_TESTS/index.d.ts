type Nullable<T> = T | null | undefined
export class ClassWithDefaultCtor {
    constructor();
    readonly x: string;
}
export class ClassWithPrimaryCtor {
    constructor(x: string);
    readonly x: string;
}
export  class ClassWithSecondaryCtor {
    private constructor();
    readonly x: string;
    static create(y: string): ClassWithSecondaryCtor;
}
export  class ClassWithMultipleSecondaryCtors {
    private constructor();
    readonly x: string;
    static createFromString(y: string): ClassWithMultipleSecondaryCtors;
    static createFromInts(y: number, z: number): ClassWithMultipleSecondaryCtors;
}
export  class OpenClassWithMixedConstructors {
    constructor(x: string);
    readonly x: string;
    static createFromStrings(y: string, z: string): OpenClassWithMixedConstructors;
    static createFromInts(y: number, z: number): OpenClassWithMixedConstructors;
}
export class DerivedClassWithSecondaryCtor extends OpenClassWithMixedConstructors {
    private constructor();
    static delegateToPrimary(y: string): DerivedClassWithSecondaryCtor;
    static delegateToCreateFromInts(y: number, z: number): DerivedClassWithSecondaryCtor;
}
export class KotlinGreeter {
    constructor(greeting: string);
    readonly greeting: string;
}

type Nullable<T> = T | null | undefined
export declare class MyClass {
    constructor(stepId: number);
    get stepId(): number;
    qux(): number;
}
export declare function box(stepId: number): string;
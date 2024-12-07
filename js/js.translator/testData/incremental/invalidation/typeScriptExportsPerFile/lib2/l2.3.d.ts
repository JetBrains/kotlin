type Nullable<T> = T | null | undefined
export declare class MyClass {
    constructor(stepId: number);
    get stepId(): number;
    qux(): number;
}
declare namespace kotlin_main {
    type Nullable<T> = T | null | undefined
    function bar(): number;
    class MyClass {
        constructor(stepId: number);
        get stepId(): number;
        qux(): number;
    }
    function box(stepId: number): string;
}

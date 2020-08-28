type Nullable<T> = T | null | undefined
export namespace foo {
    const prop: number;
    class C {
        constructor(x: number);
        readonly x: number;
        doubleX(): number;
    }
    function box(): string;
}
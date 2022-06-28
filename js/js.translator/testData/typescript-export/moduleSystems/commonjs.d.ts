type Nullable<T> = T | null | undefined
export namespace foo {
    const prop: number;
    class C {
        constructor(x: number);
        get x(): number;
        doubleX(): number;
    }
    function box(): string;
}
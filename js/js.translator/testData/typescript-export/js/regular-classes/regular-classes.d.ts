declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        class A {
            constructor();
        }
        class A1 {
            constructor(x: number);
            get x(): number;
        }
        class A2 {
            constructor(x: string, y: boolean);
            get x(): string;
            get y(): boolean;
            set y(value: boolean);
        }
        class A3 {
            constructor();
            get x(): number;
        }
        class A4<T> {
            constructor(value: T);
            get value(): T;
            test(): T;
        }
        class A5 {
            constructor();
            static get Companion(): {
                get x(): number;
            };
        }
        class A6 {
            constructor();
            then(): number;
            catch(): number;
        }
        class GenericClassWithConstraint<T extends foo.A6> {
            constructor(test: T);
            get test(): T;
        }
    }
}

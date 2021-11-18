declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    const __doNotImplementIt: unique symbol
    type __doNotImplementIt = typeof __doNotImplementIt
    namespace foo {



        function invalid_args_name_sum(first_value: number, second_value: number): number;

        class A1 {
            constructor(first_value: number, second_value: number);
            readonly "first value": number;
            "second.value": number;
        }
        class A2 {
            constructor();
            "invalid:name": number;
        }
        class A3 {
            constructor();
            "invalid@name sum"(x: number, y: number): number;
            invalid_args_name_sum(first_value: number, second_value: number): number;
        }
        class A4 {
            constructor();
            static readonly Companion: {
                "@invalid+name@": number;
                "^)run.something.weird^("(): string;
            };
        }
    }
}
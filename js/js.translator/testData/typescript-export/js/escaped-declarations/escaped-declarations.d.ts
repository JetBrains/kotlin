declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    namespace foo {
        
        
        
        function invalid_args_name_sum(first_value: number, second_value: number): number;
        
        class A1 {
            constructor(first_value: number, second_value: number);
            get "first value"(): number;
            get "second.value"(): number;
            set "second.value"(value: number);
        }
        class A2 {
            constructor();
            get "invalid:name"(): number;
            set "invalid:name"(value: number);
        }
        class A3 {
            constructor();
            "invalid@name sum"(x: number, y: number): number;
            invalid_args_name_sum(first_value: number, second_value: number): number;
        }
        class A4 {
            constructor();
            static get Companion(): {
                get "@invalid+name@"(): number;
                set "@invalid+name@"(value: number);
                "^)run.something.weird^("(): string;
            };
        }
    }
}

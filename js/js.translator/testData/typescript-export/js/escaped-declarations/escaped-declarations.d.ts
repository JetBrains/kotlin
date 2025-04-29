declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {



        function invalid_args_name_sum(first_value: number, second_value: number): number;

        class A1 {
            constructor(first_value: number, second_value: number);
            get "first value"(): number;
            get "second.value"(): number;
            set "second.value"(value: number);
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace A1.$metadata$ {
            const constructor: abstract new () => A1;
        }
        class A2 {
            constructor();
            get "invalid:name"(): number;
            set "invalid:name"(value: number);
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace A2.$metadata$ {
            const constructor: abstract new () => A2;
        }
        class A3 {
            constructor();
            "invalid@name sum"(x: number, y: number): number;
            invalid_args_name_sum(first_value: number, second_value: number): number;
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace A3.$metadata$ {
            const constructor: abstract new () => A3;
        }
        class A4 {
            constructor();
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace A4.$metadata$ {
            const constructor: abstract new () => A4;
        }
        namespace A4 {
            abstract class Companion extends KtSingleton<Companion.$metadata$.constructor>() {
                private constructor();
            }
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace Companion.$metadata$ {
                abstract class constructor {
                    get "@invalid+name@"(): number;
                    set "@invalid+name@"(value: number);
                    "^)run.something.weird^("(): string;
                    private constructor();
                }
            }
        }
    }
}

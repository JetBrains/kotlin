declare namespace main {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace lib {
        abstract class Base {
            constructor();
            abstract value(suffix?: string): Promise<string>;
        }
        namespace Base {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => Base;
            }
        }
    }
    namespace foo {
        function createValue(): lib.Base;
        function readValue(value: lib.Base): Promise<string>;
    }
}



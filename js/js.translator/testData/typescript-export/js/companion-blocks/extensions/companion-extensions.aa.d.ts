declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        function append(value?: string): string;
        class ExportedWithCompanionExtensions {
            constructor();
        }
        namespace ExportedWithCompanionExtensions {
            /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
            namespace $metadata$ {
                const constructor: abstract new () => ExportedWithCompanionExtensions;
            }
        }
    }
}

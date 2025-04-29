declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    function KtSingleton<T>(): T & (abstract new() => any);
    namespace foo {
        interface ExportedInternalInterface {
        }
    }
    namespace foo {
        const exportedVal: number;
        function exportedFun(): number;
        class ExportedClass {
            constructor();
            get value(): number;
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace ExportedClass.$metadata$ {
            const constructor: abstract new () => ExportedClass;
        }
    }
    namespace foo {
        interface FileLevelExportedExternalInterface {
        }
    }
    namespace foo {
        const fileLevelExportedVal: number;
        function fileLevelExportedFun(): number;
        class FileLevelExportedClass {
            constructor();
            get value(): number;
        }
        /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
        namespace FileLevelExportedClass.$metadata$ {
            const constructor: abstract new () => FileLevelExportedClass;
        }
    }
}

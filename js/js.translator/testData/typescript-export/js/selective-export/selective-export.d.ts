declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
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
    }
}

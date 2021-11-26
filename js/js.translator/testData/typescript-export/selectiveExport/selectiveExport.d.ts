declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    const __doNotImplementIt: unique symbol
    type __doNotImplementIt = typeof __doNotImplementIt
    namespace foo {
        interface ExportedInternalInterface {
        }
    }
    namespace foo {
        interface FileLevelExportedExternalInterface {
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
        const fileLevelExportedVal: number;
        function fileLevelExportedFun(): number;
        class FileLevelExportedClass {
            constructor();
            get value(): number;
        }
    }
}

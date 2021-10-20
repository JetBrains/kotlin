declare namespace JS_TESTS {
    type Nullable<T> = T | null | undefined
    const __doNotImplementIt: unique symbol
    type __doNotImplementIt = typeof __doNotImplementIt
    namespace foo {
        interface ExportedInternalInterface {
        }
    }
    namespace foo {
        const exportedVal: number;
        function exportedFun(): number;
        class ExportedClass {
            constructor();
            readonly value: number;
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
            readonly value: number;
        }
    }
}

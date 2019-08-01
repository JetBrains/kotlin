type Nullable<T> = T | null | undefined

declare namespace foo {
    function box(): string

    const exportedVal: number;

    function exportedFun(): number

    class ExportedClass {
        constructor()
    }

    interface ExportedInternalInterface {
    }
}

declare namespace foo {
    const fileLevelExportedVal: number;

    function fileLevelExportedFun(): number

    class FileLevelExportedClass {
        constructor()
    }

    interface FileLevelExportedExternalInterface {
    }
}

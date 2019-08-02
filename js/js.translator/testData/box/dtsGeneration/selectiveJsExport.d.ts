type Nullable<T> = T | null | undefined

declare namespace foo {
    interface ExportedInternalInterface {
    }
}

declare namespace foo {
    interface FileLevelExportedExternalInterface {
    }
}

declare namespace foo {
    function box(): string

    const exportedVal: number;

    function exportedFun(): number

    class ExportedClass {
        constructor()
    }
}

declare namespace foo {
    const fileLevelExportedVal: number;

    function fileLevelExportedFun(): number

    class FileLevelExportedClass {
        constructor()
    }
}

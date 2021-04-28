type Nullable<T> = T | null | undefined
export interface ExportedInternalInterface {
}
export interface FileLevelExportedExternalInterface {
}
export function exportedFun(): number;
export class ExportedClass {
    constructor();
    readonly value: number;
}
export function fileLevelExportedFun(): number;
export class FileLevelExportedClass {
    constructor();
    readonly value: number;
}
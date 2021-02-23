type Nullable<T> = T | null | undefined
export interface ExportedInternalInterface {
}
export function exportedFun(): number;
export class ExportedClass {
    constructor();
    readonly value: number;
}
export interface FileLevelExportedExternalInterface {
}
export function fileLevelExportedFun(): number;
export class FileLevelExportedClass {
    constructor();
    readonly value: number;
}
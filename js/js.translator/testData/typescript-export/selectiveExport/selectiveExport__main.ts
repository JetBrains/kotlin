import { exportedFun, ExportedClass, fileLevelExportedFun, FileLevelExportedClass } from "./JS_TESTS/index.js";

function box(): string {
    const tens: number[] = [
        exportedFun(),
        new ExportedClass().value,
        fileLevelExportedFun(),
        new FileLevelExportedClass().value
    ];

    if (tens.every((value) => value === 10))
        return "OK";

    return "FAIL";
}

console.assert(box() == "OK")
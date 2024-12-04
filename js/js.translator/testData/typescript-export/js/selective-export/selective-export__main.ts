import foo = JS_TESTS.foo;

function box(): string {
    const tens: number[] = [
        foo.exportedVal,
        foo.exportedFun(),
        new foo.ExportedClass().value,
        foo.fileLevelExportedVal,
        foo.fileLevelExportedFun(),
        new foo.FileLevelExportedClass().value
    ];

    if (tens.every((value) => value === 10))
        return "OK";

    return "FAIL";
}
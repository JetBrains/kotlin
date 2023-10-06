tasks.create("check") {
    dependsOn(tasks.findByPath(":kotlin-stdlib:wasmJsTest"))
    dependsOn(tasks.findByPath(":kotlin-stdlib:wasmWasiTest"))
}
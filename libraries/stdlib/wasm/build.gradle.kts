tasks.create("check") {
    dependsOn(tasks.findByPath(":kotlin-stdlib-wasm-js:check"))
}
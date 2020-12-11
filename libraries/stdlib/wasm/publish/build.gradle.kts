description = "Kotlin Standard Library for experimental WebAssembly platform"

// Using separate project to publish a single klib from multiplatform build

publish {
    artifactId = "kotlin-stdlib-wasm"
    pom.packaging = "klib"
    artifact(tasks.getByPath(":kotlin-stdlib-wasm:jsJar")) {
        extension = "klib"
    }
}

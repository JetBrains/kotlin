import org.jetbrains.kotlin.build.wasmtools.WasmToolsExtension

extensions.create<WasmToolsExtension>(
    "wasmToolsKotlinBuild",
    project,
)

import org.jetbrains.kotlin.build.wasmtools.WasmToolsExtension

val wasmToolsExtension = extensions.create<WasmToolsExtension>(
    "wasmToolsKotlinBuild",
    project,
)

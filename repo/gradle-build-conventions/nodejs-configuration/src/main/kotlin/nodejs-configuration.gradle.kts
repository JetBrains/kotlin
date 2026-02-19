import org.jetbrains.kotlin.build.nodejs.NodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin

val nodeJs = NodeJsPlugin.apply(project)
val wasmNodeJs = WasmNodeJsPlugin.apply(project)

extensions.create<NodeJsExtension>(
    "nodeJsKotlinBuild",
    project,
    nodeJs,
)

extensions.create<NodeJsExtension>(
    "wasmNodeJsKotlinBuild",
    project,
    wasmNodeJs,
)
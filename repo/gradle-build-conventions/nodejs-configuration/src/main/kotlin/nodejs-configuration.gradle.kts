import org.jetbrains.kotlin.build.nodejs.NodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin

val nodeJs = NodeJsPlugin.apply(project).apply {
    downloadBaseUrl.set(null as String?)
}
val wasmNodeJs = WasmNodeJsPlugin.apply(project).apply {
    downloadBaseUrl.set(null as String?)
}

val nodeJsKotlinBuild = extensions.create<NodeJsExtension>(
    "nodeJsKotlinBuild",
    project,
    nodeJs,
    "javascript.engine.path.NodeJs",
    "nodejs.lts",
)

val wasmNodeJsKotlinBuild = extensions.create<NodeJsExtension>(
    "wasmNodeJsKotlinBuild",
    project,
    wasmNodeJs,
    "wasm.javascript.engine.path.NodeJs",
    "nodejs",
)

with(nodeJsKotlinBuild) {
    nodeJs.version.set(nodeJsVersion)
}

with(wasmNodeJsKotlinBuild) {
    wasmNodeJs.version.set(nodeJsVersion)
}

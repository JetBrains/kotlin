import org.jetbrains.kotlin.build.nodejs.NodeJsExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin

val nodeJsRoot = NodeJsRootPlugin.apply(project.rootProject)

extensions.create<NodeJsExtension>(
    "nodeJsKotlinBuild",
    nodeJsRoot,
)
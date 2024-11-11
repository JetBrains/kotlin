import org.jetbrains.kotlin.build.binaryen.BinaryenExtension
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenRootPlugin

val binaryenRoot = BinaryenRootPlugin.apply(project.rootProject)

extensions.create<BinaryenExtension>(
    "binaryenKotlinBuild",
    binaryenRoot,
)
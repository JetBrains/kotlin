// COMPILER_PLUGIN: compose-compiler-plugin.jar generateFunctionKeyMetaAnnotations=true
// FULL_JDK

import androidx.compose.runtime.Composable

@Composable
fun Greeting() {
}

fun box(): String {
    val method = Class.forName("BoxKt").declaredMethods.single { it.name == "Greeting" }
    return if (method.parameterCount == 2) "OK"
    else "fail: Compose plugin did not rewrite Greeting (parameterCount=${method.parameterCount})"
}

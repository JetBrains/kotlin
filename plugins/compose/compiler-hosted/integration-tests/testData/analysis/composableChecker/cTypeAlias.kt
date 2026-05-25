// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*
typealias Content = @Composable () -> Unit
@Composable fun C() {}
@Composable fun C2(content: Content) { content() }
@Composable fun C3() {
    val inner: Content = { C() }
    C2 { C() }
    C2 { inner() }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, localProperty, propertyDeclaration,
typeAliasDeclaration */

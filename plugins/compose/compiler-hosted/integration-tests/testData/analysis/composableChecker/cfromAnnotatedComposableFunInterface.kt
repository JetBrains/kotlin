// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.Composable

fun interface A { @Composable fun f() }
@Composable fun B() {
  val f = @Composable { B() }
  A(f)
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, interfaceDeclaration, lambdaLiteral, localProperty,
propertyDeclaration */

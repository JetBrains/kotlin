// RUN_PIPELINE_TILL: CODEGEN

// module: main
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable

@Composable
fun WithBoxWithConstraints() {
    BoxWithConstraints {
        BasicText("${constraints.maxWidth}")
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral */

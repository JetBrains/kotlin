import androidx.compose.runtime.Composable

@Composable
fun SimpleAnimatedContentSample() {
    @Composable fun Foo() {}

    AnimatedContent(1f) {
        Foo()
    }
}

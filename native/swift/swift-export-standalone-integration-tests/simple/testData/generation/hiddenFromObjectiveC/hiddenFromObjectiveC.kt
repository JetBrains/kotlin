// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@HiddenFromObjC
class ThisShouldBeHidden

fun consume_hidden_class(arg: ThisShouldBeHidden): Unit = TODO()
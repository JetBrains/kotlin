// WITH_RUNTIME
val v1 = listOf(1, 2, 3, 11, 33, 25, 100)
    .filter<caret> { it % 2 == 0 }
    .isNotEmpty() // Some Comment
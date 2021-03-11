fun foo() {
    buildString {
        with(xxx) {
            with(f()) Label@ {
                g()?.let Label1@ {
                    listOf(1, 2, 3, 4, 5).filterTo(collection) { item ->
                        doIt() {
                            val v = Label2@ { p: Int ->
                                x(1, Label3@ { Something().apply { <caret> } })
                            }
                        }
                    }
                }
            }
        }
    }
}
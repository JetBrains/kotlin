fun a() {
    val b = 3
    val select = """
                select
                ${a
                <caret>}
                from T                
                """
}

// IGNORE_FORMATTER
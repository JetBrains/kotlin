fun some() {
    when {
        true && false ->
        else <caret>-> Unit
    }
}

// SET_FALSE: ALIGN_IN_COLUMNS_CASE_BRANCH
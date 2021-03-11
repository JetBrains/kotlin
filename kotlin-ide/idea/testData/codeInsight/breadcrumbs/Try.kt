fun foo() {
    try {
        try {

        }
        finally {
            try {

            }
            catch (e: RuntimeException) {
                <caret>
            }
        }
    }
    catch(e: Exception) {

    }
}
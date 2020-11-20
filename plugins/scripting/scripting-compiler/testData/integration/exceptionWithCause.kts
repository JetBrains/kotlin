try {
    try {
        throw Exception("Error!")
    } catch (e: Exception) {
        throw Exception("Oh no", e)
    }
} catch (e: Exception) {
    throw Exception("Top", e)
}

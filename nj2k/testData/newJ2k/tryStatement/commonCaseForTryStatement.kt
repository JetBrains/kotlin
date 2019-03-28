try {
    callMethod(params)
} catch (e: Exception) {
    println(1)
} catch (e: IOException) {
    println(0)
} finally {
    println(3)
}
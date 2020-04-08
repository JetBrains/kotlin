suspend fun fff() {

}

suspend fun ggg() {
    return <lineMarker descr="Suspend function call">fff</lineMarker>()
}
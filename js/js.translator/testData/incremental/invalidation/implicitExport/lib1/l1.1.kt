@JsExport
fun consumeCollection(list: List<String>): String? {
    return list.getOrNull(0)
}

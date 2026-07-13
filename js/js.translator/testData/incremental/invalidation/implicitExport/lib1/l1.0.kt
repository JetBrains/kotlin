@JsExport
fun consumeCollection(map: Map<String, String>): String? {
    return map["foo"]
}

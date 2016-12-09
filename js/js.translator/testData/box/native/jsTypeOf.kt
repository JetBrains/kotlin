fun box(): String {
    var result = jsTypeOf(23)
    if (result != "number") return "fail1: $result"

    result = jsTypeOf("23")
    if (result != "string") return "fail2: $result"

    result = jsTypeOf({ x: Int -> x })
    if (result != "function") return "fail3: $result"

    result = jsTypeOf(object {})
    if (result != "object") return "fail4: $result"

    result = jsTypeOf(true)
    if (result != "boolean") return "fail5: $result"

    return "OK"
}
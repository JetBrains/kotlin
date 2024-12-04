fun box(stepId: Int, isWasm: Boolean): String {
    when (stepId) {
        0, 1 -> {
            if (makeMyInterfaceObject(false).interfaceFunction() != "$stepId") return "Fail x = false"
            if (makeMyInterfaceObject(true).interfaceFunction() != "$stepId") return "Fail x = true"
        }
        else -> return "Unknown"
    }
    return "OK"
}

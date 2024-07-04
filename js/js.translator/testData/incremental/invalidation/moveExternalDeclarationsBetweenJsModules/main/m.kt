fun box(stepId: Int, isWasm: Boolean): String {
    if (isWasm && (stepId == 5 || stepId == 6)) return "OK" //Now only support module load with filename
    val x = testFunction()
    if (x != stepId) {
        return "Fail: $x != $stepId"
    }
    return "OK"
}

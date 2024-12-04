fun box(stepId: Int, isWasm: Boolean): String {
    val parent = Parent("parent")
    val child = Child("child")

    if (parent.objectName != "parent") return "fail: initial parent objectName at step $stepId"
    if (child.objectName != "child") return "fail: initial child objectName at step $stepId"

    parent.objectName = "updated parent"

    if (parent.objectName != "updated parent") return "fail: updated parent objectName at step $stepId"

    child.objectName = "updated child"

    if (child.objectName != "updated child") return "fail: updated child objectName at step $stepId"

    if (!parent.isValid) return "fail: parent is invalid at step $stepId"
    if (!child.isValid) return "fail: child is invalid at step $stepId"

    return "OK"
}

fun box(stepId: Int): String {
    val parent = Parent("parent")
    val child = Child("child")

    if (parent.name != "parent") return "fail: initial parent name at step $stepId"
    if (child.name != "child") return "fail: initial child name at step $stepId"

    parent.name = "updated parent"

    if (parent.name != "updated parent") return "fail: updated parent name at step $stepId"

    child.name = "updated child"

    if (child.name != "updated child") return "fail: updated child name at step $stepId"

    if (!parent.isValid) return "fail: parent is invalid at step $stepId"
    if (!child.isValid) return "fail: child is invalid at step $stepId"

    return "OK"
}

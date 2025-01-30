@file:OptIn(ExperimentalAssociatedObjects::class)

import kotlin.reflect.*

fun box(stepId: Int, isWasm: Boolean): String {
    if (Outer::class.findAssociatedObject<Annotation1>() != Outer.Inner1.Companion) return "fail1"
    if (Outer::class.findAssociatedObject<Annotation2>() != Outer.Inner2.Companion) return "fail2"
    if (Outer.Inner2::class.findAssociatedObject<Annotation1>() != Outer.Inner1.Companion) return "fail3"
    if (Outer.Inner2::class.findAssociatedObject<Annotation2>() != Outer.Inner2.Companion) return "fail4"

    if (Outer::class.findAssociatedObject<Annotation3>() != Outer.Inner3.Companion) return "fail5"
    if (Outer::class.findAssociatedObject<Annotation4>() != Outer.Inner4.Companion) return "fail6"
    if (Outer.Inner4::class.findAssociatedObject<Annotation3>() != Outer.Inner3.Companion) return "fail6"
    if (Outer.Inner4::class.findAssociatedObject<Annotation4>() != Outer.Inner4.Companion) return "fail7"
    return "OK"
}

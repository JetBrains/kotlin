// WITH_STDLIB
// ISSUE: KT-89027

import lombok.AccessLevel
import lombok.Builder

// `access = PRIVATE` makes the builder the annotated class's own business, not nobody's: the class itself must
// be able to call the setters and `build()` of the builder it asked for. In Java that follows from a class being
// able to reach a private member of its own nested class; Kotlin's `private` inside the builder class would mean
// that class alone, so the functions generated into it are public there instead - the builder class staying
// private is what keeps them out of everyone else's reach.
@Builder(access = AccessLevel.PRIVATE)
class AccessPrivateTarget(val x: Int) {
    companion object {
        fun makeInternally(x: Int): AccessPrivateTarget = builder().x(x).build()
    }
}

@Builder(access = AccessLevel.PRIVATE)
class CalledFromClassBody(val x: Int) {
    fun make(): CalledFromClassBody = builder().x(1).build()
}

// Outside the class nothing of the builder is reachable, which is what `PRIVATE` is for.
fun fromOutside() {
    AccessPrivateTarget.<!INVISIBLE_REFERENCE!>builder<!>()
}

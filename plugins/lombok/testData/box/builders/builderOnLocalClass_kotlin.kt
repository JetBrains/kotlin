// ISSUE: KT-88848

import lombok.Builder
import kotlin.test.assertEquals

// A local class can hold no companion object - `companion object` inside one is
// WRONG_MODIFIER_CONTAINING_DECLARATION - so there is nowhere to put the `builder()` factory, and the
// class-level annotation is reported as `ANNOTATION_HAS_NO_EFFECT`, `LOCAL_CLASS` never having been an allowed
// target. Nothing at all may be generated for such a class: the builder class nested in one is not itself
// local, and generating its callables failed with "You should use ConeClassLikeLookupTagWithFixedSymbol for
// local <local>/..." (KT-88848). That holds for a constructor and a function builder inside a local class just
// as much, and neither of those carries a diagnostic of its own - the annotation sits on a target that is
// perfectly allowed, and only the class around it is the problem.
fun localBuilders(): String {
    <!ANNOTATION_HAS_NO_EFFECT!>@Builder<!>
    class LocalWithClassBuilder(val value: Int)

    class LocalWithConstructorBuilder(val value: Int) {
        @Builder
        constructor() : this(0)
    }

    class LocalWithMethodBuilder {
        @Builder
        fun make(value: Int): String = value.toString()
    }

    return LocalWithClassBuilder(1).value.toString() +
            LocalWithConstructorBuilder().value +
            LocalWithMethodBuilder().make(2)
}

fun box(): String {
    assertEquals("102", localBuilders())

    return "OK"
}

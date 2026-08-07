// IGNORE_BACKEND_K2: ANY
// ISSUE: KT-88133
// TODO: currently fails because of "Inconsistent target list for lambda annotation: [CLASS, FILE]" exception
// Compiler already reports `ANNOTATION_HAS_NO_EFFECT` warning on lambdas but a generator generates something but should not.

import lombok.EqualsAndHashCode
import lombok.NoArgsConstructor
import lombok.ToString
import lombok.extern.java.Log

fun box(): String {
    val log = @Log { }
    val toString = @ToString { }
    val noArgsConstructor = @NoArgsConstructor { }
    val equalsAndHashCode = @EqualsAndHashCode { }

    return "OK"
}

import dependency.X
import dependency.Y
import dependency.Z

fun foo(s: String, x: X, y: Y, z: Z) {
    if (s in <caret>)
}

// EXIST: x
// EXIST: y
// ABSENT: z

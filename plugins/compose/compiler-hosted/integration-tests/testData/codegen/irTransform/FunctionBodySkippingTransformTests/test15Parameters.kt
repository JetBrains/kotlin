import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable
fun Example(
    a00: Int = 0,
    a01: Int = 0,
    a02: Int = 0,
    a03: Int = 0,
    a04: Int = 0,
    a05: Int = 0,
    a06: Int = 0,
    a07: Int = 0,
    a08: Int = 0,
    a09: Int = 0,
    a10: Int = 0,
    a11: Int = 0,
    a12: Int = 0,
    a13: Int = 0,
    a14: Int = 0
) {
    // in order
    Example(
        a00,
        a01,
        a02,
        a03,
        a04,
        a05,
        a06,
        a07,
        a08,
        a09,
        a10,
        a11,
        a12,
        a13,
        a14
    )
    // in opposite order
    Example(
        a14,
        a13,
        a12,
        a11,
        a10,
        a09,
        a08,
        a07,
        a06,
        a05,
        a04,
        a03,
        a02,
        a01,
        a00
    )
}

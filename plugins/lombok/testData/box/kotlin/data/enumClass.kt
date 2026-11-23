// ISSUE: KT-91394

import lombok.Data
import kotlin.test.assertEquals

<!ANNOTATION_HAS_NO_EFFECT!>@Data<!>
enum class Color { RED }

fun box(): String {
    assertEquals("RED", Color.RED.toString())
    return "OK"
}

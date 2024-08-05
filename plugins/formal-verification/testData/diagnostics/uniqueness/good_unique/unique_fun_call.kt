// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.NeverVerify
import org.jetbrains.kotlin.formver.plugin.Unique

@Unique
fun g(): Int {
    return 0
}

fun f(@Unique x: Int) {

}

fun use_f() {
    f(g())
}

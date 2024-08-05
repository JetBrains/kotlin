// UNIQUE_CHECK_ONLY

import org.jetbrains.kotlin.formver.plugin.NeverConvert
import org.jetbrains.kotlin.formver.plugin.NeverVerify
import org.jetbrains.kotlin.formver.plugin.Unique
import org.jetbrains.kotlin.formver.plugin.Borrowed

fun f(@Unique x: Int) {

}

<!UNIQUENESS_VIOLATION!>fun use_f(@Borrowed @Unique y: Int) {
    f(y)
}<!>

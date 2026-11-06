// ISSUE: KT-91266
// RENDER_DIAGNOSTICS_FULL_TEXT

import lombok.Data

open class A(val code: Int)

<!CALL_SUPER_NOT_CALLED!>@Data<!>
class B(code: Int, val extra: String) : A(code)

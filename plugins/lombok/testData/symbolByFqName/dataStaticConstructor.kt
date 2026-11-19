// callable: /Range.Companion.of
// DO_NOT_REQUIRE_SYMBOL_RESTORATION
// DO_NOT_REQUIRE_NON_PSI_SYMBOL_RESTORATION
import lombok.Data

@Data(staticConstructor = "of")
class Range(val from: Int, var to: Int)

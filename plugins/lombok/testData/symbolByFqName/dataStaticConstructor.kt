// callable: /Range.Companion.of
import lombok.Data

@Data(staticConstructor = "of")
class Range(val from: Int, var to: Int)

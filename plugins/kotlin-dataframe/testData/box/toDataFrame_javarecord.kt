// FILE: JavaRecord.java
import java.util.List;
import java.util.Arrays;

public record JavaRecord(
    int i,
    Bean bean
) {}

// FILE: Bean.java
import java.util.List;
import java.util.Arrays;

public class Bean {
    public int getI() {
        return 42;
    }

    public List<String> getStringList() {
        return Arrays.asList("aaa", "bbb", "ccc");
    }

    public int[] getArray() {
        return new int[]{1, 2, 3};
    }
}


// FILE: test.kt

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*

class KotlinRecord(
    val javaRecord: JavaRecord,
)

fun box(): String {
    val res = listOf(
        KotlinRecord(
            JavaRecord(1, Bean()),
        ),
    ).toDataFrame(maxDepth = 2)
    val v1: Int = res[0].javaRecord.i
    val v2: Int? = res[0].javaRecord.bean.i
    res.compileTimeSchema().print()
    res.assert()
    return "OK"
}

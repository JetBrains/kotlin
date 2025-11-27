// JVM_TARGET: 1.8

// FILE: JavaRecord.java
import java.util.List;
import java.util.Arrays;

public class JavaRecord {
    public int getI() {
        return 42;
    }

    public List<String> getStringList() {
        return Arrays.asList("aaa", "bbb", "ccc");
    }

    public List<Bean> getBeans() {
        return Arrays.asList(new Bean());
    }

    public List<Bean> getBeanWithParameter(int i) {
        return Arrays.asList(new Bean());
    }
}


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
            JavaRecord(),
        ),
    ).toDataFrame(maxDepth = 2)
    val i: Int = res[0].javaRecord.i
    res.compileTimeSchema().print()
    val l: List<String?> = res[0].javaRecord.stringList
    val array: IntArray? = res[0].javaRecord.beans[0].array
    res.assert()
    return "OK"
}

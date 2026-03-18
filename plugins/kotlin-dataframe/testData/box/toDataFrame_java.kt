// JVM_TARGET: 1.8

// FILE: JavaPojo.java
import java.util.List;
import java.util.Arrays;

public class JavaPojo {
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
    val javaPojo: JavaPojo,
)

fun box(): String {
    val res = listOf(
        KotlinRecord(
            JavaPojo(),
        ),
    ).toDataFrame(maxDepth = 2)
    val i: Int = res[0].javaPojo.i
    res.compileTimeSchema().print()
    val l: List<String?> = res[0].javaPojo.stringList
    val array: IntArray? = res[0].javaPojo.beans[0].array
    res.assert()
    return "OK"
}

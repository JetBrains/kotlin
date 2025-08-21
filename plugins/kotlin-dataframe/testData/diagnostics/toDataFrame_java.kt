// FILE: JavaRecord.java

public class JavaRecord {
    public int getI() {
        return 42;
    }

    public List<String> getAaa() {
        return List.of("aaa", "bbb", "ccc");
    }

    public List<Bean> getBean() {
        return List.of(new Bean());
    }

    public List<Bean> getBeanWithParameter(int i) {
        return List.of(new Bean());
    }
}


// FILE: Bean.java

public class Bean {
    public int getI() {
        return 42;
    }

    public List<String> getAaa() {
        return List.of("aaa", "bbb", "ccc");
    }

    public int[] getArray() {

    }
}


// FILE: test.kt

import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*

class S(
    val javaRecord: JavaRecord,
)

fun box(): String {
    val res = listOf(
        S(
            JavaRecord(),
        ),
    ).toDataFrame(maxDepth = 2)
    res.javaRecord.i
    res.javaRecord.aaa
    res.javaRecord.bean
    return "OK"
}

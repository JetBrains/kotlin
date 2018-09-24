//FILE: lib/R.java
package lib;

public class R {
    public static class id {
        public final static int textView = 100;
    }
}

//FILE: app/R.java
package app;

public class R {
    public static class layout {
        public final static int mainActivity = 100;
    }
}

//FILE: app/R2.java
package app;

public class R2 { // For ButterKnife library project support
    public static class layout {
        public final static int mainActivity = 100;
    }
}

//FILE: app/B.java
package app;

public class B {
    public static class id {
        public final static int textView = 200;
    }

    public final static boolean a1 = false;
    public final static byte a2 = 1;
    public final static int a3 = 2;
    public final static short a4 = 3;
    public final static long a5 = 4L;
    public final static char a6 = '5';
    public final static float a7 = 6.0f;
    public final static double a8 = 7.0;
    public final static String a9 = "A";
}

//FILE: test.kt
package app

import lib.R as LibR
import lib.R.id.textView

annotation class Bind(val id: Int)

annotation class Anno(
        val a1: Boolean,
        val a2: Byte,
        val a3: Int,
        val a4: Short,
        val a5: Long,
        val a6: Char,
        val a7: Float,
        val a8: Double,
        val a9: String)

class MyActivity {
    @Bind(LibR.id.textView)
    fun foo() {}

    @Bind(lib.R.id.textView)
    fun foo2() {}

    @Bind(app.R.layout.mainActivity)
    fun foo3() {}

    @Bind(R.layout.mainActivity)
    fun foo4() {}

    @Bind(R2.layout.mainActivity)
    @Anno(a1 = B.a1, a2 = B.a2, a3 = B.a3, a4 = B.a4, a5 = B.a5, a6 = B.a6, a7 = B.a7, a8 = B.a8, a9 = B.a9)
    fun foo5() {}

    @Bind(B.id.textView)
    fun plainIntConstant() {}
}

// EXPECTED_ERROR class B is public, should be declared in a file named B.java
// EXPECTED_ERROR class R is public, should be declared in a file named R.java
// EXPECTED_ERROR class R is public, should be declared in a file named R.java
// EXPECTED_ERROR class R2 is public, should be declared in a file named R2.java
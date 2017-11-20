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

//FILE: app/B.java
package app;

public class B {
    public static class id {
        public final static int textView = 200;
    }
}

//FILE: test.kt
package app

import lib.R as LibR
import lib.R.id.textView

annotation class Bind(val id: Int)

class MyActivity {
    @Bind(LibR.id.textView)
    fun foo() {}

    @Bind(lib.R.id.textView)
    fun foo2() {}

    @Bind(app.R.layout.mainActivity)
    fun foo3() {}

    @Bind(R.layout.mainActivity)
    fun foo4() {}

    @Bind(B.id.textView)
    fun notResource() {}
}
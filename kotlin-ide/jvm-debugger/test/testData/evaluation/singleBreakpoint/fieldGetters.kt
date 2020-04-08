// FILE: fieldGetters.kt
package fieldGetters

import forTests.FieldsGetters
import forTests.FieldsGetters.*

fun main(args: Array<String>) {
    //Breakpoint!
    val a = 5
}

class K1 {
    val a: Int = 0
        get() = field + 1
}

class K2 {
    @JvmField
    val a: Int = 0
}

// EXPRESSION: K1().a
// RESULT: 1: I

// EXPRESSION: K1().a_field
// RESULT: 0: I

// EXPRESSION: K2().a
// RESULT: 0: I

// EXPRESSION: K2().a_field
// RESULT: 0: I

// EXPRESSION: PublicField().foo
// RESULT: "a": Ljava/lang/String;

// EXPRESSION: PublicField().foo_field
// RESULT: Unresolved reference: foo_field

// EXPRESSION: PackagePrivateField().foo
// RESULT: "b": Ljava/lang/String;

// EXPRESSION: PackagePrivateField().foo_field
// RESULT: Unresolved reference: foo_field

// EXPRESSION: ProtectedField().foo
// RESULT: "c": Ljava/lang/String;

// EXPRESSION: ProtectedField().foo_field
// RESULT: Unresolved reference: foo_field

// EXPRESSION: PrivateField().foo
// RESULT: "d": Ljava/lang/String;

// EXPRESSION: PrivateField().foo_field
// RESULT: Unresolved reference: foo_field

// EXPRESSION: PublicFieldGetter().foo
// RESULT: "b": Ljava/lang/String;

// EXPRESSION: PublicFieldGetter().foo_field
// RESULT: "a": Ljava/lang/String;

// EXPRESSION: PrivateFieldPublicGetter().foo
// RESULT: "d": Ljava/lang/String;

// EXPRESSION: PrivateFieldPublicGetter().foo_field
// RESULT: "c": Ljava/lang/String;

// EXPRESSION: PrivateFieldPrivateGetter().foo
// RESULT: "f": Ljava/lang/String;

// EXPRESSION: PrivateFieldPrivateGetter().foo_field
// RESULT: "e": Ljava/lang/String;

// EXPRESSION: PublicGetter1().foo
// RESULT: "g": Ljava/lang/String;

// EXPRESSION: PublicGetter1().foo_field
// RESULT: "a": Ljava/lang/String;

// EXPRESSION: PublicGetter2().foo
// RESULT: "h": Ljava/lang/String;

// EXPRESSION: PublicGetter2().foo_field
// RESULT: "b": Ljava/lang/String;

// EXPRESSION: PrivateGetter1().foo
// RESULT: "d": Ljava/lang/String;

// EXPRESSION: PrivateGetter1().foo_field
// RESULT: Unresolved reference: foo_field

// EXPRESSION: PublicGetterOnly().foo
// RESULT: "a": Ljava/lang/String;

// EXPRESSION: PublicGetterOnly().foo_field
// RESULT: Unresolved reference: foo_field

// EXPRESSION: PublicFieldAndGetterInParent().foo
// RESULT: "a": Ljava/lang/String;

// EXPRESSION: PublicFieldAndGetterInParent().foo_field
// RESULT: "b": Ljava/lang/String;

// FILE: forTests/FieldsGetters.java
package forTests;

public class FieldsGetters {
    public static class PublicField {
        public String foo = "a";
    }

    public static class PackagePrivateField {
        String foo = "b";
    }

    public static class ProtectedField {
        protected String foo = "c";
    }

    public static class PrivateField {
        private String foo = "d";
    }

    public static class PublicFieldGetter {
        public final String foo = "a";

        public String getFoo() {
            return "b";
        }
    }

    public static class PrivateFieldPublicGetter {
        private final String foo = "c";

        public String getFoo() {
            return "d";
        }
    }

    public static class PrivateFieldPrivateGetter {
        private final String foo = "e";

        public String getFoo() {
            return "f";
        }
    }

    public static class PublicGetter1 extends PublicField {
        public String getFoo() {
            return "g";
        }
    }

    public static class PublicGetter2 extends PackagePrivateField {
        public String getFoo() {
            return "h";
        }
    }

    public static class PrivateGetter1 extends PrivateField {
        private String getFoo() {
            return "g";
        }
    }

    public static class PublicGetterOnly {
        public String getFoo() {
            return "a";
        }
    }

    public static class PublicFieldAndGetterInParent extends PublicGetterOnly {
        public String foo = "b";
    }

    public abstract class AbstractGetter {
        public String foo = "c";
        public abstract String getFoo();
    }
}
import org.jetbrains.annotations.*;

class A {

    /* rare nullable, handle with caution */
    public String nullableString() {
        if (Math.random() > 0.999) {
            return "a string";
        }
        return null;
    }

    public void takesNotNullString(@NotNull String s) {
        System.out.println(s.substring(1));
    }

    public void aVoid() {
        String aString;
        if (nullableString() != null) {
            aString = nullableString();
            if (aString != null) {
                for (int i = 0; i < 10; i++) {
                    takesNotNullString(aString); // Bang-bang here
                    aString = nullableString();
                }
            }
            else {
                aString = "aaa";
            }
        }
        else {
            aString = "bbbb";
        }
    }
}
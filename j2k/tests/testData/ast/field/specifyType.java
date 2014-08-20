import org.jetbrains.annotations.Nullable;
import java.util.*;

class A {
    private final List<String> field1 = new ArrayList<String>();
    final List<String> field2 = new ArrayList<String>();
    public final int field3 = 0;
    protected final int field4 = 0;

    private List<String> field5 = new ArrayList<String>();
    List<String> field6 = new ArrayList<String>();

    private int field7 = 0;
    int field8 = 0;

    @Nullable private String field9 = "a"
    @Nullable private String field10 = foo();

    String foo() { return "x"; }

    void bar() {
        field5 = new ArrayList<String>();
        field7++;
        field8++;
        field9 = null;
        field10 = null;
    }
}
public class TestAssignmentInCondition {
    private int i;

    public void foo(int x) {
        if ((i = x) > 0) System.out.println(">0");
    }
}
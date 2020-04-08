public class AssignmentAsExpression {
    private int field;
    private int field2;

    public void assign(int value) {
        int v = field = value;
        field = field2 = value;
        int j;
        int i = j = 0;
    }
}
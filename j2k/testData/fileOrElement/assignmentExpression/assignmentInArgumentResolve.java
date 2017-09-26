public class TestAssignmentInArgumentConfusingResolve {
    private int x = 0;

    public void setX(int xx) {
        notify(x = xx);
    }

    private void notify(int x) {}
}
package test;

public class TestAssignmentInReturn {
    private String last;

    public String foo(String s) {
        return last = s;
    }
}
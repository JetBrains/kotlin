package test;

public class TestValReassign {
    private String s1;
    private String s2;

    public TestValReassign(String s1) {
        this.s1 = s1;
    }

    public TestValReassign(String s1, String s2) {
        this(s1);
        this.s2 = s2;
    }
}
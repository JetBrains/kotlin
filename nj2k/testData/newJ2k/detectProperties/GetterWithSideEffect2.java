public class C {
    private String x = "";

    public String getX() {
        System.out.println("getter invoked");
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    void foo() {
        System.out.println("x = " + x);
    }
}
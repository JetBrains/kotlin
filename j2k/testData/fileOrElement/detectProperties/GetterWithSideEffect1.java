public class C {
    private String myX = "";

    public String getX() {
        System.out.println("getter invoked");
        return myX;
    }

    public void setX(String x) {
        this.myX = x;
    }

    void foo() {
        System.out.println("myX = " + myX);
    }
}
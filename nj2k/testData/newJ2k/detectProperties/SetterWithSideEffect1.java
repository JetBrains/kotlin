public class C {
    private String myX = "";

    public String getX() {
        return myX;
    }

    public void setX(String x) {
        System.out.println("setter invoked");
        this.myX = x;
    }

    void foo() {
        myX = "a";
    }
}
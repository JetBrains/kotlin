public class C {
    private String x = "";

    public String getX() {
        System.out.println("getter invoked");
        return this.x;
    }

    public void setX(String x) {
        this.x = x;
    }
}
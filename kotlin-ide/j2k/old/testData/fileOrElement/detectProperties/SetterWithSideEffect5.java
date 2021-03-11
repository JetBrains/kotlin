public class C {
    private String x = "";

    public String getX() {
        return x;
    }

    public void setX(String x) {
        System.out.println("setter invoked");
        this.x = x;
    }
}
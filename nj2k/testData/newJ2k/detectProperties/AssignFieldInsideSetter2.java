public class C {
    private String x = "";
    C other = null;

    public String getX() {
        return x;
    }

    void setX(String x) {
        System.out.println("setter invoked");
        if (other != null) {
            this.other.x = x;
        }
        this.x = x;
    }
}
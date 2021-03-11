public class Bean {
    private String prop = "value";
    public String getProp2() { return prop; }
    public void setProp2(String prop) { this.prop = prop; }

    void test() {
        getProp2();
        setProp2("");
    }
}
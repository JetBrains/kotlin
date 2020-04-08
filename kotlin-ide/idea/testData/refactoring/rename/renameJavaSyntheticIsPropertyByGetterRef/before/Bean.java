public class Bean {
    private boolean prop;
    public boolean isProp() { return prop; }
    public void setProp(boolean prop) { this.prop = prop; }

    void test() {
        isProp();
        setProp(true);
    }
}
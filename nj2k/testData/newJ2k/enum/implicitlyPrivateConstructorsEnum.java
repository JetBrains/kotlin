public enum JavaEnum {
    A("a"), B;

    JavaEnum(String x) {
        this.x = x;
    }

    JavaEnum() {
        this.x = "default";
    }

    protected String x;
}
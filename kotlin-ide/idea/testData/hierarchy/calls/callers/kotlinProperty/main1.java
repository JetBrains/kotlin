class JA {
    public String name = new KBase().getName() + new KA().getName();

    public String getName() {
        return new KBase().getName() + new KA().getName();
    }
}
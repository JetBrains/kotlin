public class WithModifiersOnAccessors {
    private synchronized void methSync() {}
    protected strictfp void methStrict() {}

    private int sync = 0;
    public synchronized int getSync() { return sync; }
    public synchronized void setSync(int sync) { this.sync = sync; }

    public double strict = 0.0;
    public strictfp double getStrict() { return strict; }
}
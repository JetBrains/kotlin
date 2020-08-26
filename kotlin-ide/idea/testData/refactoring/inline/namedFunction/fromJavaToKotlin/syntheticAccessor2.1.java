public class AccessorsBug {
    public void setSmth(int i) {
        System.out.println("in setter"+i);
    }
    public int getSmth() {
        return 42;
    }
}
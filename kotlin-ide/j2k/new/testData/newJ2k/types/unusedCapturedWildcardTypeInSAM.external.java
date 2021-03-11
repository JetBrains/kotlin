public interface SamFace<T> {
    void samMethod(int p);
}

public class SamAcceptor<T> {
    public void acceptSam(SamFace<T> sam) {
    }
}
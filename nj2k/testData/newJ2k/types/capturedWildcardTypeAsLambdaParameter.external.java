public interface SamFace<T> {
    void samMethod(T p);
}

public class SamAcceptor<T> {
    public void acceptSam(SamFace<T> sam) {
    }
}
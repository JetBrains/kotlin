import java.util.List;

class X {
    private final List<Y> list;

    X(List<Y> list) {
        this.list = list;
    }

    class Y{}
}
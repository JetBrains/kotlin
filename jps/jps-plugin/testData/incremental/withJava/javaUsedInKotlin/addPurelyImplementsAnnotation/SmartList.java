import java.util.AbstractList;
import kotlin.jvm.PurelyImplements;

//@PurelyImplements("kotlin.collections.MutableList")
public class SmartList<E> extends AbstractList<E> {


    @Override
    public boolean add(E e) {
        return true;
    }

    @Override
    public E get(int index) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }
}
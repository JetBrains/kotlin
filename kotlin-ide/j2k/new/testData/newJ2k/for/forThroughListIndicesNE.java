import java.util.List;
import java.util.ArrayList;

class C{
    void foo(List<String> list) {
        for (int i = 0; i != list.size(); i++) {
            list.set(i, "a");
        }
    }
}

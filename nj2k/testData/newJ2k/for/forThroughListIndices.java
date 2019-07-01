import java.util.List;
import java.util.ArrayList;

class C{
    void foo1(List<String> list) {
        for (int i = 0; i < list.size(); i++) {
            list.set(i, "a");
        }
    }

    void foo2(ArrayList<String> list) {
        for (int i = 0; i < list.size(); i++) {
            list.set(i, "a");
        }
    }
}

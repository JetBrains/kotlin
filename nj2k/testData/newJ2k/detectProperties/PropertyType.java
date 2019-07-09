import java.util.ArrayList;
import java.util.List;

class A {
    private ArrayList<String> list1 = new ArrayList<String>();
    private List<String> list2 = new ArrayList<String>();
    private List<String> myList3 = new ArrayList<String>();

    public List<String> getList1() {
        return list1;
    }
    public List<String> getList2() {
        return list2;
    }
    public List<String> getList3() {
        return myList3;
    }

    void foo() {
        list1.add("a");
        list2.add("a");
        myList3.add("a");
    }
}
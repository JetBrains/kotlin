public class JavaUsage {

    public static void main(String[] args) {
        SomePojo obj = new SomePojo();
        obj.setAge(12);
        boolean v = obj.isHuman();
        obj.setHuman(!v);
        System.out.println(obj);
    }
}

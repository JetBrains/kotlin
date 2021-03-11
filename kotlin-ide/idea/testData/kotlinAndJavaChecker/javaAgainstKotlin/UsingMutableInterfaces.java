public class UsingMutableInterfaces {
    public static class Lists {
        public static <E> void useCMutableList(CMutableList<E> cMutableList, E elem, java.util.Collection<E> other) {
            java.util.Iterator<E> iter = cMutableList.iterator();
            cMutableList.addAll(other);
            cMutableList.add(elem);
            cMutableList.isEmpty();
            cMutableList.clear();
            cMutableList.getSize();
            cMutableList.size();
            boolean b = cMutableList.remove(elem);
            E e1 = cMutableList.remove(3);
            E e2 = cMutableList.removeAt(6);
        }
    }
}
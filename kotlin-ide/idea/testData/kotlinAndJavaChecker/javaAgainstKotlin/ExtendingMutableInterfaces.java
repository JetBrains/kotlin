public class EtendingMutableInterfaces {
    public static class Lists {
        <error descr="Class 'ExtendIList1' must either be declared abstract or implement abstract method 'size()' in 'List'">public static class ExtendIList1 implements IMutableList<String></error> {

        }

        <error descr="Class 'ExtendIList2' must either be declared abstract or implement abstract method 'size()' in 'List'">public static class ExtendIList2<E> implements IMutableList<E></error> {

        }

        // Compiler bug causes remove(int) to clash, see KT-17782
        //public static class ExtendCList1<E> extends CMutableList<E> {
        //
        //}
        //
        //public static class ExtendCList2<E> extends CMutableList<String> {
        //
        //}

        public static class ExtendSList extends SMutableList {

        }

        public static class ExtendAList extends AMutableList {

        }
    }

    public static class Collections {
        <error descr="Class 'ExtendICollection1' must either be declared abstract or implement abstract method 'size()' in 'Collection'">public static class ExtendICollection1 implements IMutableCollection<String></error> {

        }

        <error descr="Class 'ExtendICollection2' must either be declared abstract or implement abstract method 'size()' in 'Collection'">public static class ExtendICollection2<E> implements IMutableCollection<E></error> {

        }

        public static class ExtendCCollection1<E> extends CMutableCollection<E> {

        }

        public static class ExtendCCollection2<E> extends CMutableCollection<String> {

        }

        public static class ExtendSCollection extends SMutableCollection {

        }

        public static class ExtendACollection extends AMutableCollection {

        }
    }

    public static class Sets {
        <error descr="Class 'ExtendISet1' must either be declared abstract or implement abstract method 'size()' in 'Set'">public static class ExtendISet1 implements IMutableSet<String></error> {

        }

        <error descr="Class 'ExtendISet2' must either be declared abstract or implement abstract method 'size()' in 'Set'">public static class ExtendISet2<E> implements IMutableSet<E></error> {

        }

        public static class ExtendCSet1<E> extends CMutableSet<E> {

        }

        public static class ExtendCSet2<E> extends CMutableSet<String> {

        }

        public static class ExtendSSet extends SMutableSet {

        }

        public static class ExtendASet extends AMutableSet {

        }
    }

    public static class Iterables {
        <error descr="Class 'ExtendIIterable1' must either be declared abstract or implement abstract method 'iterator()' in 'Iterable'">public static class ExtendIIterable1 implements IMutableIterable<String></error> {

        }

        <error descr="Class 'ExtendIIterable2' must either be declared abstract or implement abstract method 'iterator()' in 'Iterable'">public static class ExtendIIterable2<E> implements IMutableIterable<E></error> {

        }

        public static class ExtendCIterable1<E> extends CMutableIterable<E> {

        }

        public static class ExtendCIterable2<E> extends CMutableIterable<String> {

        }

        public static class ExtendSIterable extends SMutableIterable {

        }

        public static class ExtendAIterable extends AMutableIterable {

        }
    }

    public static class Iterators {
        <error descr="Class 'ExtendIIterator1' must either be declared abstract or implement abstract method 'hasNext()' in 'Iterator'">public static class ExtendIIterator1 implements IMutableIterator<String></error> {

        }

        <error descr="Class 'ExtendIIterator2' must either be declared abstract or implement abstract method 'hasNext()' in 'Iterator'">public static class ExtendIIterator2<E> implements IMutableIterator<E></error> {

        }

        public static class ExtendCIterator1<E> extends CMutableIterator<E> {

        }

        public static class ExtendCIterator2<E> extends CMutableIterator<String> {

        }

        public static class ExtendSIterator extends SMutableIterator {

        }

        public static class ExtendAIterator extends AMutableIterator {

        }
    }

    public static class Maps {
        <error descr="Class 'ExtendIMap1' must either be declared abstract or implement abstract method 'size()' in 'Map'">public static class ExtendIMap1 implements IMutableMap<String, Integer></error> {

        }

        <error descr="Class 'ExtendIMap2' must either be declared abstract or implement abstract method 'size()' in 'Map'">public static class ExtendIMap2<E> implements IMutableMap<String, E></error> {

        }

        public static class ExtendCMap1<K, V> extends CMutableMap<K, V> {

        }

        public static class ExtendCMap2<V> extends CMutableMap<String, V> {

        }

        // NOTE: looks like a bug in compiler see KT-17738

        //public static class ExtendSMap extends SMutableMap<A> {
        //
        //}
        //
        //public static class ExtendABMap extends ABMutableMap {
        //
        //}
    }

    public static class MapEntrys {
        <error descr="Class 'ExtendIMapEntry1' must either be declared abstract or implement abstract method 'getKey()' in 'Entry'">public static class ExtendIMapEntry1 implements IMutableMapEntry<String, Integer></error> {

        }

        <error descr="Class 'ExtendIMapEntry2' must either be declared abstract or implement abstract method 'getKey()' in 'Entry'">public static class ExtendIMapEntry2<E> implements IMutableMapEntry<String, E></error> {

        }

        public static class ExtendCMapEntry1<K, V> extends CMutableMapEntry<K, V> {

        }

        public static class ExtendCMapEntry2<V> extends CMutableMapEntry<String, V> {

        }

        public static class ExtendSMapEntry extends SMutableMapEntry<A> {

        }

        public static class ExtendAMapEntry extends ABMutableMapEntry {

        }
    }
}
//file
package demo;

import java.util.Iterator;

class Test implements Iterable<String> {
    @Override
    public Iterator<String> iterator() {
        return null;
    }

    public Iterator<String> push(Iterator<String> i) {
        Iterator<String> j = i;
        return j;
    }
}

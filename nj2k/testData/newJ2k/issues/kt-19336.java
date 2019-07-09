public class TestReturnsArray {
    public String[] strings(int n) {
        String[] result = new String[n];
        for (int i = 0; i < n; ++i) {
            result[i] = Integer.toString(i);
        }
        return result;
    }
}
public class NullableArray {
    public String[] createArrayFailure(int size) {
        return new String[size];
    }

    public String[] createArraySuccess(int size) {
        return new String[] {};
    }
} 
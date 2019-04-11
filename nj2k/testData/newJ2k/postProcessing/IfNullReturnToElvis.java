import java.io.File;

class C {
    String foo(File file) {
        File parent = file.getParentFile();
        if (parent == null) return "";
        return parent.getName();
    }
}
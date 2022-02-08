package xxx;

import xxx.JavaWithInner.TableRenderer.TableRow;

public class JavaWithInner {
    public static class TableRenderer{
        public interface TableRow {
        }
    }

    public static class TextRenderer implements TableRow {
        public void method() {}
    }
}
// NO_CONVERSION_EXPECTED
import java.io.*;
import java.util.Collections;
import java.util.List;

class C {
    <selection>List<File></selection> foo() {
        return Collections.emptyList();
    }
}
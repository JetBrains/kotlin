import java.util.HashMap;
import java.util.Map;

class Test {
    public static void main(String[] args) {
        Map<String, String> resultMap = new HashMap<String, String>();
        for (final Map.Entry<String, String> entry : resultMap.entrySet()) {
            String key = entry.getKey();
            String type = entry.getValue();

            if (key.equals("myKey")) {
                System.out.println(type);
            }
        }
    }
}
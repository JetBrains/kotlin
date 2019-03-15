import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.stream.Collectors;

public class KotlinPrePushHook {
    private static final String BRANCH_DATA_URL = "https://raw.githubusercontent.com/Kotlin/kotlin-branch-status/master/status.txt";
    private static final String MESSAGE_PREFIX = "MESSAGE.";

    private static final List<String> KOTLIN_GIT_REPOS = Arrays.asList(
            "git@github.com:JetBrains/kotlin.git",
            "https://github.com/JetBrains/kotlin.git");

    private static final String REF_PREFIX = "refs/heads/";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            error("Usage: <remote refs> <target repository>, got " + Arrays.toString(args));
        }

        List<String> remoteRefs = Arrays
                .stream(args[0].split(","))
                .filter(ref -> ref.startsWith(REF_PREFIX))
                .map(ref -> ref.substring(REF_PREFIX.length())).collect(Collectors.toList());

        String targetRepo = args[1];

        if (!KOTLIN_GIT_REPOS.contains(targetRepo)) {
            return;
        }

        check(remoteRefs);
    }

    private static void check(List<String> remoteRefs) throws Exception {
        Map<String, String> branchProperties = getProperties(new URL(BRANCH_DATA_URL));

        if (branchProperties.isEmpty()) {
            return;
        }

        Map<String, String> messages = new HashMap<>();
        Map<String, String> branches = new HashMap<>();

        for (Map.Entry<String, String> entry : branchProperties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.startsWith(MESSAGE_PREFIX)) {
                messages.put(key.substring(MESSAGE_PREFIX.length()), value);
            } else {
                branches.put(key, value);
            }
        }

        for (Map.Entry<String, String> entry : branches.entrySet()) {
            String branchName = entry.getKey();

            if (remoteRefs.contains(branchName)) {
                String message = messages.get(entry.getValue());
                if (message == null) {
                    error("Invalid message key '" + entry.getValue() + "', expected one of these: " + messages.keySet());
                }

                //noinspection ConstantConditions
                if (!showWarning(message, branchName)) {
                    error("Push aborted.");
                }
            }
        }
    }

    private static void error(String message) {
        //noinspection UseOfSystemOutOrSystemErr
        System.err.println(message);
        System.exit(1);
    }

    private static boolean showWarning(String reason, String targetBranch) {
        String baseMessage = "You are about to commit to '" + targetBranch + "',\n" +
                             "and it is probably not the best idea.\n" +
                             "Please think twice before pressing 'Yes'.";

        String confirmationMessage = "Do you still want to commit to '" + targetBranch + "'?";

        String completeMessage = baseMessage + "\n\nReason: " + reason.replace("\\n", "\n") + "\n\n" + confirmationMessage;

        int result = JOptionPane.showOptionDialog(
                null, completeMessage, "Friendly warning",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null,
                new String[] { "Yes", "No" }, "No");

        return result == JOptionPane.YES_OPTION;
    }

    private static Map<String, String> getProperties(URL url) throws Exception {
        URLConnection connection = url.openConnection();

        try (InputStream is = connection.getInputStream()) {
            Properties properties = new Properties();
            properties.load(is);

            Map<String, String> propertyMap = new HashMap<>();
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                    propertyMap.put((String) entry.getKey(), (String) entry.getValue());
                }
            }

            return propertyMap;
        } catch (IOException e) {
            System.err.println("Can't fetch " + BRANCH_DATA_URL + " (" + e.getMessage() + ").");
            System.err.println("Pre-push hook won't work.");
            return Collections.emptyMap();
        }
    }
}

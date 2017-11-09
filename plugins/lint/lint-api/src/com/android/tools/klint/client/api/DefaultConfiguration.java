/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.klint.client.api;

import static com.android.SdkConstants.CURRENT_PLATFORM;
import static com.android.SdkConstants.PLATFORM_WINDOWS;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Project;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.TextFormat;
import com.android.utils.XmlUtils;
import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Default implementation of a {@link Configuration} which reads and writes
 * configuration data into {@code lint.xml} in the project directory.
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public class DefaultConfiguration extends Configuration {
    private final LintClient mClient;
    /** Default name of the configuration file */
    public static final String CONFIG_FILE_NAME = "lint.xml"; //$NON-NLS-1$

    // Lint XML File
    @NonNull
    private static final String TAG_ISSUE = "issue"; //$NON-NLS-1$
    @NonNull
    private static final String ATTR_ID = "id"; //$NON-NLS-1$
    @NonNull
    private static final String ATTR_SEVERITY = "severity"; //$NON-NLS-1$
    @NonNull
    private static final String ATTR_PATH = "path"; //$NON-NLS-1$
    @NonNull
    private static final String ATTR_REGEXP = "regexp"; //$NON-NLS-1$
    @NonNull
    private static final String TAG_IGNORE = "ignore"; //$NON-NLS-1$
    @NonNull
    private static final String VALUE_ALL = "all"; //$NON-NLS-1$

    private final Configuration mParent;
    private final Project mProject;
    private final File mConfigFile;
    private boolean mBulkEditing;

    /** Map from id to list of project-relative paths for suppressed warnings */
    private Map<String, List<String>> mSuppressed;

    /** Map from id to regular expressions. */
    @Nullable
    private Map<String, List<Pattern>> mRegexps;

    /**
     * Map from id to custom {@link Severity} override
     */
    private Map<String, Severity> mSeverity;

    protected DefaultConfiguration(
            @NonNull LintClient client,
            @Nullable Project project,
            @Nullable Configuration parent,
            @NonNull File configFile) {
        mClient = client;
        mProject = project;
        mParent = parent;
        mConfigFile = configFile;
    }

    protected DefaultConfiguration(
            @NonNull LintClient client,
            @NonNull Project project,
            @Nullable Configuration parent) {
        this(client, project, parent, new File(project.getDir(), CONFIG_FILE_NAME));
    }

    /**
     * Creates a new {@link DefaultConfiguration}
     *
     * @param client the client to report errors to etc
     * @param project the associated project
     * @param parent the parent/fallback configuration or null
     * @return a new configuration
     */
    @NonNull
    public static DefaultConfiguration create(
            @NonNull LintClient client,
            @NonNull Project project,
            @Nullable Configuration parent) {
        return new DefaultConfiguration(client, project, parent);
    }

    /**
     * Creates a new {@link DefaultConfiguration} for the given lint config
     * file, not affiliated with a project. This is used for global
     * configurations.
     *
     * @param client the client to report errors to etc
     * @param lintFile the lint file containing the configuration
     * @return a new configuration
     */
    @NonNull
    public static DefaultConfiguration create(@NonNull LintClient client, @NonNull File lintFile) {
        return new DefaultConfiguration(client, null /*project*/, null /*parent*/, lintFile);
    }

    @Override
    public boolean isIgnored(
            @NonNull Context context,
            @NonNull Issue issue,
            @Nullable Location location,
            @NonNull String message) {
        ensureInitialized();

        String id = issue.getId();
        List<String> paths = mSuppressed.get(id);
        if (paths == null) {
            paths = mSuppressed.get(VALUE_ALL);
        }
        if (paths != null && location != null) {
            File file = location.getFile();
            String relativePath = context.getProject().getRelativePath(file);
            for (String suppressedPath : paths) {
                if (suppressedPath.equals(relativePath)) {
                    return true;
                }
                // Also allow a prefix
                if (relativePath.startsWith(suppressedPath)) {
                    return true;
                }
            }
        }

        if (mRegexps != null) {
            List<Pattern> regexps = mRegexps.get(id);
            if (regexps == null) {
                regexps = mRegexps.get(VALUE_ALL);
            }
            if (regexps != null && location != null) {
                // Check message
                for (Pattern regexp : regexps) {
                    Matcher matcher = regexp.matcher(message);
                    if (matcher.find()) {
                        return true;
                    }
                }

                // Check location
                File file = location.getFile();
                String relativePath = context.getProject().getRelativePath(file);
                boolean checkUnixPath = false;
                for (Pattern regexp : regexps) {
                    Matcher matcher = regexp.matcher(relativePath);
                    if (matcher.find()) {
                        return true;
                    } else if (regexp.pattern().indexOf('/') != -1) {
                        checkUnixPath = true;
                    }
                }

                if (checkUnixPath && CURRENT_PLATFORM == PLATFORM_WINDOWS) {
                    relativePath = relativePath.replace('\\', '/');
                    for (Pattern regexp : regexps) {
                        Matcher matcher = regexp.matcher(relativePath);
                        if (matcher.find()) {
                            return true;
                        }
                    }
                }
            }
        }

        return mParent != null && mParent.isIgnored(context, issue, location, message);
    }

    @NonNull
    protected Severity getDefaultSeverity(@NonNull Issue issue) {
        if (!issue.isEnabledByDefault()) {
            return Severity.IGNORE;
        }

        return issue.getDefaultSeverity();
    }

    @Override
    @NonNull
    public Severity getSeverity(@NonNull Issue issue) {
        ensureInitialized();

        Severity severity = mSeverity.get(issue.getId());
        if (severity == null) {
            severity = mSeverity.get(VALUE_ALL);
        }

        if (severity != null) {
            return severity;
        }

        if (mParent != null) {
            return mParent.getSeverity(issue);
        }

        return getDefaultSeverity(issue);
    }

    private void ensureInitialized() {
        if (mSuppressed == null) {
            readConfig();
        }
    }

    private void formatError(String message, Object... args) {
        if (args != null && args.length > 0) {
            message = String.format(message, args);
        }
        message = "Failed to parse `lint.xml` configuration file: " + message;
        LintDriver driver = new LintDriver(new IssueRegistry() {
            @Override @NonNull public List<Issue> getIssues() {
                return Collections.emptyList();
            }
        }, mClient);
        mClient.report(new Context(driver, mProject, mProject, mConfigFile),
                IssueRegistry.LINT_ERROR,
                mProject.getConfiguration(driver).getSeverity(IssueRegistry.LINT_ERROR),
                Location.create(mConfigFile), message, TextFormat.RAW);
    }

    private void readConfig() {
        mSuppressed = new HashMap<String, List<String>>();
        mSeverity = new HashMap<String, Severity>();

        if (!mConfigFile.exists()) {
            return;
        }

        try {
            Document document = XmlUtils.parseUtfXmlFile(mConfigFile, false);
            NodeList issues = document.getElementsByTagName(TAG_ISSUE);
            Splitter splitter = Splitter.on(',').trimResults().omitEmptyStrings();
            for (int i = 0, count = issues.getLength(); i < count; i++) {
                Node node = issues.item(i);
                Element element = (Element) node;
                String idList = element.getAttribute(ATTR_ID);
                if (idList.isEmpty()) {
                    formatError("Invalid lint config file: Missing required issue id attribute");
                    continue;
                }
                Iterable<String> ids = splitter.split(idList);

                NamedNodeMap attributes = node.getAttributes();
                for (int j = 0, n = attributes.getLength(); j < n; j++) {
                    Node attribute = attributes.item(j);
                    String name = attribute.getNodeName();
                    String value = attribute.getNodeValue();
                    if (ATTR_ID.equals(name)) {
                        // already handled
                    } else if (ATTR_SEVERITY.equals(name)) {
                        for (Severity severity : Severity.values()) {
                            if (value.equalsIgnoreCase(severity.name())) {
                                for (String id : ids) {
                                    mSeverity.put(id, severity);
                                }
                                break;
                            }
                        }
                    } else {
                        formatError("Unexpected attribute \"%1$s\"", name);
                    }
                }

                // Look up ignored errors
                NodeList childNodes = element.getChildNodes();
                if (childNodes.getLength() > 0) {
                    for (int j = 0, n = childNodes.getLength(); j < n; j++) {
                        Node child = childNodes.item(j);
                        if (child.getNodeType() == Node.ELEMENT_NODE) {
                            Element ignore = (Element) child;
                            String path = ignore.getAttribute(ATTR_PATH);
                            if (path.isEmpty()) {
                                String regexp = ignore.getAttribute(ATTR_REGEXP);
                                if (regexp.isEmpty()) {
                                    formatError("Missing required attribute %1$s or %2$s under %3$s",
                                        ATTR_PATH, ATTR_REGEXP, idList);
                                } else {
                                    addRegexp(idList, ids, n, regexp, false);
                                }
                            } else {
                                // Normalize path format to File.separator. Also
                                // handle the file format containing / or \.
                                if (File.separatorChar == '/') {
                                    path = path.replace('\\', '/');
                                } else {
                                    path = path.replace('/', File.separatorChar);
                                }

                                if (path.indexOf('*') != -1) {
                                    String regexp = globToRegexp(path);
                                    addRegexp(idList, ids, n, regexp, false);
                                } else {
                                    for (String id : ids) {
                                        List<String> paths = mSuppressed.get(id);
                                        if (paths == null) {
                                            paths = new ArrayList<String>(n / 2 + 1);
                                            mSuppressed.put(id, paths);
                                        }
                                        paths.add(path);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (SAXParseException e) {
            formatError(e.getMessage());
        } catch (Exception e) {
            mClient.log(e, null);
        }
    }

    @NonNull
    public static String globToRegexp(@NonNull String glob) {
        StringBuilder sb = new StringBuilder(glob.length() * 2);
        int begin = 0;
        sb.append('^');
        for (int i = 0, n = glob.length(); i < n; i++) {
            char c = glob.charAt(i);
            if (c == '*') {
                begin = appendQuoted(sb, glob, begin, i) + 1;
                if (i < n - 1 && glob.charAt(i + 1) == '*') {
                    i++;
                    begin++;
                }
                sb.append(".*?");
            } else if (c == '?') {
                begin = appendQuoted(sb, glob, begin, i) + 1;
                sb.append(".?");
            }
        }
        appendQuoted(sb, glob, begin, glob.length());
        sb.append('$');
        return sb.toString();
    }

    private static int appendQuoted(StringBuilder sb, String s, int from, int to) {
        if (to > from) {
            boolean isSimple = true;
            for (int i = from; i < to; i++) {
                char c = s.charAt(i);
                if (!Character.isLetterOrDigit(c) && c != '/' && c != ' ') {
                    isSimple = false;
                    break;
                }
            }
            if (isSimple) {
                for (int i = from; i < to; i++) {
                    sb.append(s.charAt(i));
                }
                return to;
            }
            sb.append(Pattern.quote(s.substring(from, to)));
        }
        return to;
    }

    private void addRegexp(@NonNull String idList, @NonNull Iterable<String> ids, int n,
            @NonNull String regexp, boolean silent) {
        try {
            if (mRegexps == null) {
                mRegexps = new HashMap<String, List<Pattern>>();
            }
            Pattern pattern = Pattern.compile(regexp);
            for (String id : ids) {
                List<Pattern> paths = mRegexps.get(id);
                if (paths == null) {
                    paths = new ArrayList<Pattern>(n / 2 + 1);
                    mRegexps.put(id, paths);
                }
                paths.add(pattern);
            }
        } catch (PatternSyntaxException e) {
            if (!silent) {
                formatError("Invalid pattern %1$s under %2$s: %3$s",
                        regexp, idList, e.getDescription());
            }
        }
    }

    private void writeConfig() {
        try {
            // Write the contents to a new file first such that we don't clobber the
            // existing file if some I/O error occurs.
            File file = new File(mConfigFile.getParentFile(),
                    mConfigFile.getName() + ".new"); //$NON-NLS-1$

            Writer writer = new BufferedWriter(new FileWriter(file));
            writer.write(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +     //$NON-NLS-1$
                    "<lint>\n");                                         //$NON-NLS-1$

            if (!mSuppressed.isEmpty() || !mSeverity.isEmpty()) {
                // Process the maps in a stable sorted order such that if the
                // files are checked into version control with the project,
                // there are no random diffs just because hashing algorithms
                // differ:
                Set<String> idSet = new HashSet<String>();
                for (String id : mSuppressed.keySet()) {
                    idSet.add(id);
                }
                for (String id : mSeverity.keySet()) {
                    idSet.add(id);
                }
                List<String> ids = new ArrayList<String>(idSet);
                Collections.sort(ids);

                for (String id : ids) {
                    writer.write("    <");                               //$NON-NLS-1$
                    writer.write(TAG_ISSUE);
                    writeAttribute(writer, ATTR_ID, id);
                    Severity severity = mSeverity.get(id);
                    if (severity != null) {
                        writeAttribute(writer, ATTR_SEVERITY,
                                severity.name().toLowerCase(Locale.US));
                    }

                    List<Pattern> regexps = mRegexps != null ? mRegexps.get(id) : null;
                    List<String> paths = mSuppressed.get(id);
                    if (paths != null && !paths.isEmpty()
                            || regexps != null && !regexps.isEmpty()) {
                        writer.write('>');
                        writer.write('\n');
                        // The paths are already kept in sorted order when they are modified
                        // by ignore(...)
                        if (paths != null) {
                            for (String path : paths) {
                                writer.write("        <");                   //$NON-NLS-1$
                                writer.write(TAG_IGNORE);
                                writeAttribute(writer, ATTR_PATH, path.replace('\\', '/'));
                                writer.write(" />\n");                       //$NON-NLS-1$
                            }
                        }
                        if (regexps != null) {
                            for (Pattern regexp : regexps) {
                                writer.write("        <");                   //$NON-NLS-1$
                                writer.write(TAG_IGNORE);
                                writeAttribute(writer, ATTR_REGEXP, regexp.pattern());
                                writer.write(" />\n");                       //$NON-NLS-1$
                            }
                        }
                        writer.write("    </");                          //$NON-NLS-1$
                        writer.write(TAG_ISSUE);
                        writer.write('>');
                        writer.write('\n');
                    } else {
                        writer.write(" />\n");                           //$NON-NLS-1$
                    }
                }
            }

            writer.write("</lint>");                                     //$NON-NLS-1$
            writer.close();

            // Move file into place: move current version to lint.xml~ (removing the old ~ file
            // if it exists), then move the new version to lint.xml.
            File oldFile = new File(mConfigFile.getParentFile(),
                    mConfigFile.getName() + '~'); //$NON-NLS-1$
            if (oldFile.exists()) {
                oldFile.delete();
            }
            if (mConfigFile.exists()) {
                mConfigFile.renameTo(oldFile);
            }
            boolean ok = file.renameTo(mConfigFile);
            if (ok && oldFile.exists()) {
                oldFile.delete();
            }
        } catch (Exception e) {
            mClient.log(e, null);
        }
    }

    private static void writeAttribute(
            @NonNull Writer writer, @NonNull String name, @NonNull String value)
            throws IOException {
        writer.write(' ');
        writer.write(name);
        writer.write('=');
        writer.write('"');
        writer.write(value);
        writer.write('"');
    }

    @Override
    public void ignore(
            @NonNull Context context,
            @NonNull Issue issue,
            @Nullable Location location,
            @NonNull String message) {
        // This configuration only supports suppressing warnings on a per-file basis
        if (location != null) {
            ignore(issue, location.getFile());
        }
    }

    /**
     * Marks the given issue and file combination as being ignored.
     *
     * @param issue the issue to be ignored in the given file
     * @param file the file to ignore the issue in
     */
    public void ignore(@NonNull Issue issue, @NonNull File file) {
        ensureInitialized();

        String path = mProject != null ? mProject.getRelativePath(file) : file.getPath();

        List<String> paths = mSuppressed.get(issue.getId());
        if (paths == null) {
            paths = new ArrayList<String>();
            mSuppressed.put(issue.getId(), paths);
        }
        paths.add(path);

        // Keep paths sorted alphabetically; makes XML output stable
        Collections.sort(paths);

        if (!mBulkEditing) {
            writeConfig();
        }
    }

    @Override
    public void setSeverity(@NonNull Issue issue, @Nullable Severity severity) {
        ensureInitialized();

        String id = issue.getId();
        if (severity == null) {
            mSeverity.remove(id);
        } else {
            mSeverity.put(id, severity);
        }

        if (!mBulkEditing) {
            writeConfig();
        }
    }

    @Override
    public void startBulkEditing() {
        mBulkEditing = true;
    }

    @Override
    public void finishBulkEditing() {
        mBulkEditing = false;
        writeConfig();
    }

    @VisibleForTesting
    File getConfigFile() {
        return mConfigFile;
    }
}
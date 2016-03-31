/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.tools.lint.checks;

import static com.android.SdkConstants.ANDROID_STRING_PREFIX;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_BACKGROUND;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_LEFT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_RIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_LEFT_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_RIGHT_OF;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_ORIENTATION;
import static com.android.SdkConstants.ATTR_STYLE;
import static com.android.SdkConstants.ATTR_TEXT;
import static com.android.SdkConstants.BUTTON;
import static com.android.SdkConstants.LINEAR_LAYOUT;
import static com.android.SdkConstants.RELATIVE_LAYOUT;
import static com.android.SdkConstants.STRING_PREFIX;
import static com.android.SdkConstants.TABLE_ROW;
import static com.android.SdkConstants.TAG_STRING;
import static com.android.SdkConstants.VALUE_SELECTABLE_ITEM_BACKGROUND;
import static com.android.SdkConstants.VALUE_TRUE;
import static com.android.SdkConstants.VALUE_VERTICAL;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Check which looks at the order of buttons in dialogs and makes sure that
 * "the dismissive action of a dialog is always on the left whereas the affirmative actions
 * are on the right."
 * <p>
 * This only looks for the affirmative and dismissive actions named "OK" and "Cancel";
 * "Cancel" usually works, but the affirmative action often has many other names -- "Done",
 * "Send", "Go", etc.
 * <p>
 * TODO: Perhaps we should look for Yes/No dialogs and suggested they be rephrased as
 * Cancel/OK dialogs? Similarly, consider "Abort" a synonym for "Cancel" ?
 */
public class ButtonDetector extends ResourceXmlDetector {
    /** Name of cancel value ("Cancel") */
    private static final String CANCEL_LABEL = "Cancel";
    /** Name of OK value ("Cancel") */
    private static final String OK_LABEL = "OK";
    /** Name of Back value ("Back") */
    private static final String BACK_LABEL = "Back";
    /** Yes */
    private static final String YES_LABEL = "Yes";
    /** No */
    private static final String NO_LABEL = "No";

    /** Layout text attribute reference to {@code @android:string/ok} */
    private static final String ANDROID_OK_RESOURCE =
            ANDROID_STRING_PREFIX + "ok"; //$NON-NLS-1$
    /** Layout text attribute reference to {@code @android:string/cancel} */
    private static final String ANDROID_CANCEL_RESOURCE =
            ANDROID_STRING_PREFIX + "cancel"; //$NON-NLS-1$
    /** Layout text attribute reference to {@code @android:string/yes} */
    private static final String ANDROID_YES_RESOURCE =
            ANDROID_STRING_PREFIX + "yes"; //$NON-NLS-1$
    /** Layout text attribute reference to {@code @android:string/no} */
    private static final String ANDROID_NO_RESOURCE =
            ANDROID_STRING_PREFIX + "no"; //$NON-NLS-1$

    private static final Implementation IMPLEMENTATION = new Implementation(
            ButtonDetector.class,
            Scope.RESOURCE_FILE_SCOPE);

    /** The main issue discovered by this detector */
    public static final Issue ORDER = Issue.create(
            "ButtonOrder", //$NON-NLS-1$
            "Button order",

            "According to the Android Design Guide,\n" +
            "\n" +
            "\"Action buttons are typically Cancel and/or OK, with OK indicating the preferred " +
            "or most likely action. However, if the options consist of specific actions such " +
            "as Close or Wait rather than a confirmation or cancellation of the action " +
            "described in the content, then all the buttons should be active verbs. As a rule, " +
            "the dismissive action of a dialog is always on the left whereas the affirmative " +
            "actions are on the right.\"\n" +
            "\n" +
            "This check looks for button bars and buttons which look like cancel buttons, " +
            "and makes sure that these are on the left.",

            Category.USABILITY,
            8,
            Severity.WARNING,
            IMPLEMENTATION)
            .addMoreInfo(
                    "http://developer.android.com/design/building-blocks/dialogs.html"); //$NON-NLS-1$

    /** The main issue discovered by this detector */
    public static final Issue STYLE = Issue.create(
            "ButtonStyle", //$NON-NLS-1$
            "Button should be borderless",

            "Button bars typically use a borderless style for the buttons. Set the " +
            "`style=\"?android:attr/buttonBarButtonStyle\"` attribute " +
            "on each of the buttons, and set `style=\"?android:attr/buttonBarStyle\"` on " +
            "the parent layout",

            Category.USABILITY,
            5,
            Severity.WARNING,
            IMPLEMENTATION)
            .addMoreInfo(
                    "http://developer.android.com/design/building-blocks/buttons.html"); //$NON-NLS-1$

    /** The main issue discovered by this detector */
    public static final Issue BACK_BUTTON = Issue.create(
            "BackButton", //$NON-NLS-1$
            "Back button",
            // TODO: Look for ">" as label suffixes as well

            "According to the Android Design Guide,\n" +
            "\n" +
            "\"Other platforms use an explicit back button with label to allow the user " +
            "to navigate up the application's hierarchy. Instead, Android uses the main " +
            "action bar's app icon for hierarchical navigation and the navigation bar's " +
            "back button for temporal navigation.\"" +
            "\n" +
            "This check is not very sophisticated (it just looks for buttons with the " +
            "label \"Back\"), so it is disabled by default to not trigger on common " +
            "scenarios like pairs of Back/Next buttons to paginate through screens.",

            Category.USABILITY,
            6,
            Severity.WARNING,
            IMPLEMENTATION)
            .setEnabledByDefault(false)
            .addMoreInfo(
                 "http://developer.android.com/design/patterns/pure-android.html"); //$NON-NLS-1$

    /** The main issue discovered by this detector */
    public static final Issue CASE = Issue.create(
            "ButtonCase", //$NON-NLS-1$
            "Cancel/OK dialog button capitalization",

            "The standard capitalization for OK/Cancel dialogs is \"OK\" and \"Cancel\". " +
            "To ensure that your dialogs use the standard strings, you can use " +
            "the resource strings @android:string/ok and @android:string/cancel.",

            Category.USABILITY,
            2,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Set of resource names whose value was either OK or Cancel */
    private Set<String> mApplicableResources;

    /**
     * Map of resource names we'd like resolved into strings in phase 2. The
     * values should be filled in with the actual string contents.
     */
    private Map<String, String> mKeyToLabel;

    /**
     * Set of elements we've already warned about. If we've already complained
     * about a cancel button, don't also report the OK button (since it's listed
     * for the warnings on OK buttons).
     */
    private Set<Element> mIgnore;

    /** Constructs a new {@link ButtonDetector} */
    public ButtonDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(BUTTON, TAG_STRING);
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.LAYOUT || folderType == ResourceFolderType.VALUES;
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        int phase = context.getPhase();
        if (phase == 1 && mApplicableResources != null) {
            // We found resources for the string "Cancel"; perform a second pass
            // where we check layout text attributes against these strings.
            context.getDriver().requestRepeat(this, Scope.RESOURCE_FILE_SCOPE);
        }
    }

    private static String stripLabel(String text) {
        text = text.trim();
        if (text.length() > 2
                && (text.charAt(0) == '"' || text.charAt(0) == '\'')
                && (text.charAt(0) == text.charAt(text.length() - 1))) {
            text = text.substring(1, text.length() - 1);
        }

        return text;
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        // This detector works in two passes.
        // In pass 1, it looks in layout files for hardcoded strings of "Cancel", or
        // references to @string/cancel or @android:string/cancel.
        // It also looks in values/ files for strings whose value is "Cancel",
        // and if found, stores the corresponding keys in a map. (This is necessary
        // since value files are processed after layout files).
        // Then, if at the end of phase 1 any "Cancel" string resources were
        // found in the value files, then it requests a *second* phase,
        // where it looks only for <Button>'s whose text matches one of the
        // cancel string resources.
        int phase = context.getPhase();
        String tagName = element.getTagName();
        if (phase == 1 && tagName.equals(TAG_STRING)) {
            NodeList childNodes = element.getChildNodes();
            for (int i = 0, n = childNodes.getLength(); i < n; i++) {
                Node child = childNodes.item(i);
                if (child.getNodeType() == Node.TEXT_NODE) {
                    String text = child.getNodeValue();
                    for (int j = 0, len = text.length(); j < len; j++) {
                        char c = text.charAt(j);
                        if (!Character.isWhitespace(c)) {
                            if (c == '"' || c == '\'') {
                                continue;
                            }
                            if (LintUtils.startsWith(text, CANCEL_LABEL, j)) {
                                String label = stripLabel(text);
                                if (label.equalsIgnoreCase(CANCEL_LABEL)) {
                                    String name = element.getAttribute(ATTR_NAME);
                                    foundResource(context, name, element);

                                    if (!label.equals(CANCEL_LABEL)
                                            && LintUtils.isEnglishResource(context, true)
                                            && context.isEnabled(CASE)) {
                                        assert label.equalsIgnoreCase(CANCEL_LABEL);
                                        context.report(CASE, child, context.getLocation(child),
                                            String.format(
                                            "The standard Android way to capitalize %1$s " +
                                            "is \"Cancel\" (tip: use `@android:string/cancel` instead)",
                                            label));
                                    }
                                }
                            } else if (LintUtils.startsWith(text, OK_LABEL, j)) {
                                String label = stripLabel(text);
                                if (label.equalsIgnoreCase(OK_LABEL)) {
                                    String name = element.getAttribute(ATTR_NAME);
                                    foundResource(context, name, element);

                                    if (!label.equals(OK_LABEL)
                                            && LintUtils.isEnglishResource(context, true)
                                            && context.isEnabled(CASE)) {
                                        assert text.trim().equalsIgnoreCase(OK_LABEL) : text;
                                        context.report(CASE, child, context.getLocation(child),
                                            String.format(
                                            "The standard Android way to capitalize %1$s " +
                                            "is \"OK\" (tip: use `@android:string/ok` instead)",
                                            label));
                                    }
                                }
                            } else if (LintUtils.startsWith(text, BACK_LABEL, j) &&
                                    stripLabel(text).equalsIgnoreCase(BACK_LABEL)) {
                                String name = element.getAttribute(ATTR_NAME);
                                foundResource(context, name, element);
                            }
                            break;
                        }
                    }
                }
            }
        } else if (tagName.equals(BUTTON)) {
            if (phase == 1) {
                if (isInButtonBar(element)
                        && !element.hasAttribute(ATTR_STYLE)
                        && !VALUE_SELECTABLE_ITEM_BACKGROUND.equals(
                                element.getAttributeNS(ANDROID_URI, ATTR_BACKGROUND))
                        && (context.getProject().getMinSdk() >= 11
                            || context.getFolderVersion() >= 11)
                        && context.isEnabled(STYLE)
                        && !parentDefinesSelectableItem(element)) {
                    context.report(STYLE, element, context.getLocation(element),
                            "Buttons in button bars should be borderless; use " +
                            "`style=\"?android:attr/buttonBarButtonStyle\"` (and " +
                            "`?android:attr/buttonBarStyle` on the parent)");
                }
            }

            String text = element.getAttributeNS(ANDROID_URI, ATTR_TEXT);
            if (phase == 2) {
                if (mApplicableResources.contains(text)) {
                    String key = text;
                    if (key.startsWith(STRING_PREFIX)) {
                        key = key.substring(STRING_PREFIX.length());
                    }
                    String label = mKeyToLabel.get(key);
                    boolean isCancel = CANCEL_LABEL.equalsIgnoreCase(label);
                    if (isCancel) {
                        if (isWrongCancelPosition(element)) {
                            reportCancelPosition(context, element);
                        }
                    } else if (OK_LABEL.equalsIgnoreCase(label)) {
                        if (isWrongOkPosition(element)) {
                            reportOkPosition(context, element);
                        }
                    } else {
                        assert BACK_LABEL.equalsIgnoreCase(label) : label + ':' + context.file;
                        Location location = context.getLocation(element);
                        if (context.isEnabled(BACK_BUTTON)) {
                            context.report(BACK_BUTTON, element, location,
                                "Back buttons are not standard on Android; see design guide's " +
                                "navigation section");
                        }
                    }
                }
            } else if (text.equals(CANCEL_LABEL) || text.equals(ANDROID_CANCEL_RESOURCE)) {
                if (isWrongCancelPosition(element)) {
                    reportCancelPosition(context, element);
                }
            } else if (text.equals(OK_LABEL) || text.equals(ANDROID_OK_RESOURCE)) {
                if (isWrongOkPosition(element)) {
                    reportOkPosition(context, element);
                }
            } else {
                boolean isYes = text.equals(ANDROID_YES_RESOURCE);
                if (isYes || text.equals(ANDROID_NO_RESOURCE)) {
                    Attr attribute = element.getAttributeNodeNS(ANDROID_URI, ATTR_TEXT);
                    Location location = context.getLocation(attribute);
                    String message = String.format("%1$s actually returns \"%2$s\", not \"%3$s\"; "
                                    + "use %4$s instead or create a local string resource for %5$s",
                            text,
                            isYes ? OK_LABEL : CANCEL_LABEL,
                            isYes ? YES_LABEL : NO_LABEL,
                            isYes ? ANDROID_OK_RESOURCE : ANDROID_CANCEL_RESOURCE,
                            isYes ? YES_LABEL : NO_LABEL);
                    context.report(CASE, element, location, message);
                }
            }
        }
    }

    private static boolean parentDefinesSelectableItem(Element element) {
        String background = element.getAttributeNS(ANDROID_URI, ATTR_BACKGROUND);
        if (VALUE_SELECTABLE_ITEM_BACKGROUND.equals(background)) {
            return true;
        }

        Node parent = element.getParentNode();
        if (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
            return parentDefinesSelectableItem((Element) parent);
        }

        return false;
    }

    /** Report the given OK button as being in the wrong position */
    private void reportOkPosition(XmlContext context, Element element) {
        report(context, element, false /*isCancel*/);
    }

    /** Report the given Cancel button as being in the wrong position */
    private void reportCancelPosition(XmlContext context, Element element) {
        report(context, element, true /*isCancel*/);
    }

    /**
     * We've found a resource reference to some label we're interested in ("OK",
     * "Cancel", "Back", ...). Record the corresponding name such that in the
     * next pass through the layouts we can check the context (for OK/Cancel the
     * button order etc).
     */
    private void foundResource(XmlContext context, String name, Element element) {
        if (!LintUtils.isEnglishResource(context, true)) {
            return;
        }

        if (!context.getProject().getReportIssues()) {
            // If this is a library project not being analyzed, ignore it
            return;
        }

        if (mApplicableResources == null) {
            mApplicableResources = new HashSet<String>();
        }

        mApplicableResources.add(STRING_PREFIX + name);

        // ALSO record all the other string resources in this file to pick up other
        // labels. If you define "OK" in one resource file and "Cancel" in another
        // this won't work, but that's probably not common and has lower overhead.
        Node parentNode = element.getParentNode();

        List<Element> items = LintUtils.getChildren(parentNode);
        if (mKeyToLabel == null) {
            mKeyToLabel = new HashMap<String, String>(items.size());
        }
        for (Element item : items) {
            String itemName = item.getAttribute(ATTR_NAME);
            NodeList childNodes = item.getChildNodes();
            for (int i = 0, n = childNodes.getLength(); i < n; i++) {
                Node child = childNodes.item(i);
                if (child.getNodeType() == Node.TEXT_NODE) {
                    String text = stripLabel(child.getNodeValue());
                    if (!text.isEmpty()) {
                        mKeyToLabel.put(itemName, text);
                        break;
                    }
                }
            }
        }
    }

    /** Report the given OK/Cancel button as being in the wrong position */
    private void report(XmlContext context, Element element, boolean isCancel) {
        if (!context.isEnabled(ORDER)) {
            return;
        }

        if (mIgnore != null && mIgnore.contains(element)) {
            return;
        }

        int target = context.getProject().getTargetSdk();
        if (target < 14) {
            // If you're only targeting pre-ICS UI's, this is not an issue
            return;
        }

        boolean mustCreateIcsLayout = false;
        if (context.getProject().getMinSdk() < 14) {
            // If you're *also* targeting pre-ICS UIs, then this reverse button
            // order is correct for layouts intended for pre-ICS and incorrect for
            // ICS layouts.
            //
            // Therefore, we need to know if this layout is an ICS layout or
            // a pre-ICS layout.
            boolean isIcsLayout = context.getFolderVersion() >= 14;
            if (!isIcsLayout) {
                // This layout is not an ICS layout. However, there *must* also be
                // an ICS layout here, or this button order will be wrong:
                File res = context.file.getParentFile().getParentFile();
                File[] resFolders = res.listFiles();
                String fileName = context.file.getName();
                if (resFolders != null) {
                    for (File folder : resFolders) {
                        String folderName = folder.getName();
                        if (folderName.startsWith(SdkConstants.FD_RES_LAYOUT)
                                && folderName.contains("-v14")) { //$NON-NLS-1$
                            File layout = new File(folder, fileName);
                            if (layout.exists()) {
                                // Yes, a v14 specific layout is available so this pre-ICS
                                // layout order is not a problem
                                return;
                            }
                        }
                    }
                }
                mustCreateIcsLayout = true;
            }
        }

        List<Element> buttons = LintUtils.getChildren(element.getParentNode());

        if (mIgnore == null) {
            mIgnore = new HashSet<Element>();
        }
        for (Element button : buttons) {
            // Mark all the siblings in the ignore list to ensure that we don't
            // report *both* the Cancel and the OK button in "OK | Cancel"
            mIgnore.add(button);
        }

        String message;
        if (isCancel) {
            message = "Cancel button should be on the left";
        } else {
            message = "OK button should be on the right";
        }

        if (mustCreateIcsLayout) {
            message = String.format(
                    "Layout uses the wrong button order for API >= 14: Create a " +
                    "`layout-v14/%1$s` file with opposite order: %2$s",
                    context.file.getName(), message);
        }

        // Show existing button order? We can only do that for LinearLayouts
        // since in for example a RelativeLayout the order of the elements may
        // not be the same as the visual order
        String layout = element.getParentNode().getNodeName();
        if (layout.equals(LINEAR_LAYOUT) || layout.equals(TABLE_ROW)) {
            List<String> labelList = getLabelList(buttons);
            String wrong = describeButtons(labelList);
            sortButtons(labelList);
            String right = describeButtons(labelList);
            message += String.format(" (was \"%1$s\", should be \"%2$s\")", wrong, right);
        }

        Location location = context.getLocation(element);
        context.report(ORDER, element, location, message);
    }

    /**
     * Sort a list of label buttons into the expected order (Cancel on the left,
     * OK on the right
     */
    private static void sortButtons(List<String> labelList) {
        for (int i = 0, n = labelList.size(); i < n; i++) {
            String label = labelList.get(i);
            if (label.equalsIgnoreCase(CANCEL_LABEL) && i > 0) {
                swap(labelList, 0, i);
            } else if (label.equalsIgnoreCase(OK_LABEL) && i < n - 1) {
                swap(labelList, n - 1, i);
            }
        }
    }

    /** Swaps the strings at positions i and j */
    private static void swap(List<String> strings, int i, int j) {
        if (i != j) {
            String temp = strings.get(i);
            strings.set(i, strings.get(j));
            strings.set(j, temp);
        }
    }

    /** Creates a display string for a list of button labels, such as "Cancel | OK" */
    private static String describeButtons(List<String> labelList) {
        StringBuilder sb = new StringBuilder(80);
        for (String label : labelList) {
            if (sb.length() > 0) {
                sb.append(" | "); //$NON-NLS-1$
            }
            sb.append(label);
        }

        return sb.toString();
    }

    /** Returns the ordered list of button labels */
    private List<String> getLabelList(List<Element> views) {
        List<String> labels = new ArrayList<String>();

        if (mIgnore == null) {
            mIgnore = new HashSet<Element>();
        }

        for (Element view : views) {
            if (view.getTagName().equals(BUTTON)) {
                String text = view.getAttributeNS(ANDROID_URI, ATTR_TEXT);
                String label = getLabel(text);
                labels.add(label);

                // Mark all the siblings in the ignore list to ensure that we don't
                // report *both* the Cancel and the OK button in "OK | Cancel"
                mIgnore.add(view);
            }
        }

        return labels;
    }

    private String getLabel(String key) {
        String label = null;
        if (key.startsWith(ANDROID_STRING_PREFIX)) {
            if (key.equals(ANDROID_OK_RESOURCE)) {
                label = OK_LABEL;
            } else if (key.equals(ANDROID_CANCEL_RESOURCE)) {
                label = CANCEL_LABEL;
            }
        } else if (mKeyToLabel != null) {
            if (key.startsWith(STRING_PREFIX)) {
                label = mKeyToLabel.get(key.substring(STRING_PREFIX.length()));
            }
        }

        if (label == null) {
            label = key;
        }

        if (label.indexOf(' ') != -1 && label.indexOf('"') == -1) {
            label = '"' + label + '"';
        }

        return label;
    }

    /** Is the cancel button in the wrong position? It has to be on the left. */
    private static boolean isWrongCancelPosition(Element element) {
        return isWrongPosition(element, true /*isCancel*/);
    }

    /** Is the OK button in the wrong position? It has to be on the right. */
    private static boolean isWrongOkPosition(Element element) {
        return isWrongPosition(element, false /*isCancel*/);
    }

    private static boolean isInButtonBar(Element element) {
        assert element.getTagName().equals(BUTTON) : element.getTagName();
        Node parentNode = element.getParentNode();
        if (parentNode.getNodeType() != Node.ELEMENT_NODE) {
            return false;
        }
        Element parent = (Element) parentNode;

        String style = parent.getAttribute(ATTR_STYLE);
        if (style != null && style.contains("buttonBarStyle")) { //$NON-NLS-1$
            return true;
        }

        // Don't warn about single Cancel / OK buttons
        if (LintUtils.getChildCount(parent) < 2) {
            return false;
        }

        String layout = parent.getTagName();
        if (layout.equals(LINEAR_LAYOUT) || layout.equals(TABLE_ROW)) {
            String orientation = parent.getAttributeNS(ANDROID_URI, ATTR_ORIENTATION);
            if (VALUE_VERTICAL.equals(orientation)) {
                return false;
            }
        } else {
            return false;
        }

        // Ensure that all the children are buttons
        Node n = parent.getFirstChild();
        while (n != null) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (!BUTTON.equals(n.getNodeName())) {
                    return false;
                }
            }
            n = n.getNextSibling();
        }

        return true;
    }

    /** Is the given button in the wrong position? */
    private static boolean isWrongPosition(Element element, boolean isCancel) {
        Node parentNode = element.getParentNode();
        if (parentNode.getNodeType() != Node.ELEMENT_NODE) {
            return false;
        }
        Element parent = (Element) parentNode;

        // Don't warn about single Cancel / OK buttons
        if (LintUtils.getChildCount(parent) < 2) {
            return false;
        }

        String layout = parent.getTagName();
        if (layout.equals(LINEAR_LAYOUT) || layout.equals(TABLE_ROW)) {
            String orientation = parent.getAttributeNS(ANDROID_URI, ATTR_ORIENTATION);
            if (VALUE_VERTICAL.equals(orientation)) {
                return false;
            }

            if (isCancel) {
                Node n = element.getPreviousSibling();
                while (n != null) {
                    if (n.getNodeType() == Node.ELEMENT_NODE) {
                        return true;
                    }
                    n = n.getPreviousSibling();
                }
            } else {
                Node n = element.getNextSibling();
                while (n != null) {
                    if (n.getNodeType() == Node.ELEMENT_NODE) {
                        return true;
                    }
                    n = n.getNextSibling();
                }
            }

            return false;
        } else if (layout.equals(RELATIVE_LAYOUT)) {
            // In RelativeLayouts, look for attachments which look like a clear sign
            // that the OK or Cancel buttons are out of order:
            //   -- a left attachment on a Cancel button (where the left attachment
            //      is a button; we don't want to complain if it's pointing to a spacer
            //      or image or progress indicator etc)
            //   -- a right-side parent attachment on a Cancel button (unless it's also
            //      attached on the left, e.g. a cancel button stretching across the
            //      layout)
            // etc.
            if (isCancel) {
                if (element.hasAttributeNS(ANDROID_URI, ATTR_LAYOUT_TO_RIGHT_OF)
                        && isButtonId(parent, element.getAttributeNS(ANDROID_URI,
                                ATTR_LAYOUT_TO_RIGHT_OF))) {
                    return true;
                }
                if (isTrue(element, ATTR_LAYOUT_ALIGN_PARENT_RIGHT) &&
                        !isTrue(element, ATTR_LAYOUT_ALIGN_PARENT_LEFT)) {
                    return true;
                }
            } else {
                if (element.hasAttributeNS(ANDROID_URI, ATTR_LAYOUT_TO_LEFT_OF)
                        && isButtonId(parent, element.getAttributeNS(ANDROID_URI,
                                ATTR_LAYOUT_TO_RIGHT_OF))) {
                    return true;
                }
                if (isTrue(element, ATTR_LAYOUT_ALIGN_PARENT_LEFT) &&
                        !isTrue(element, ATTR_LAYOUT_ALIGN_PARENT_RIGHT)) {
                    return true;
                }
            }

            return false;
        } else {
            // TODO: Consider other button layouts - GridLayouts, custom views extending
            // LinearLayout etc?
            return false;
        }
    }

    /**
     * Returns true if the given attribute (in the Android namespace) is set to
     * true on the given element
     */
    private static boolean isTrue(Element element, String attribute) {
        return VALUE_TRUE.equals(element.getAttributeNS(ANDROID_URI, attribute));
    }

    /** Is the given target id the id of a {@code <Button>} within this RelativeLayout? */
    private static boolean isButtonId(Element parent, String targetId) {
        for (Element child : LintUtils.getChildren(parent)) {
            String id = child.getAttributeNS(ANDROID_URI, ATTR_ID);
            if (LintUtils.idReferencesMatch(id, targetId)) {
                return child.getTagName().equals(BUTTON);
            }
        }
        return false;
    }
}

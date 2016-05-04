// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintNewApiInspection
// INSPECTION_CLASS2: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintInlinedApiInspection
// INSPECTION_CLASS3: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintOverrideInspection

import android.animation.RectEvaluator
import android.annotation.TargetApi
import org.w3c.dom.DOMError
import org.w3c.dom.DOMErrorHandler
import org.w3c.dom.DOMLocator

import android.view.ViewGroup.LayoutParams
import android.app.Activity
import android.app.ApplicationErrorReport
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.Build
import android.widget.Chronometer
import android.widget.GridLayout
import dalvik.bytecode.OpcodeInfo

import android.os.Build.VERSION
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH
import android.os.Build.VERSION_CODES.JELLY_BEAN
import android.widget.TextView

@Suppress("SENSELESS_COMPARISON", "UNUSED_EXPRESSION", "UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
class ApiCallTest: Activity() {

    fun method(chronometer: Chronometer, locator: DOMLocator) {
        chronometer.<error descr="Call requires API level 16 (current min is 1): `setBackground`">setBackground(null)</error>

        // Virtual call
        <error descr="Call requires API level 11 (current min is 1): `getActionBar`">getActionBar()</error> // API 11
        <error descr="Call requires API level 11 (current min is 1): `getActionBar`">actionBar</error> // API 11

        // Class references (no call or field access)
        val error: DOMError? = null // API 8
        val clz = DOMErrorHandler::class // API 8

        // Method call
        chronometer.<error descr="Call requires API level 3 (current min is 1): `getOnChronometerTickListener`">onChronometerTickListener</error> // API 3

        // Inherited method call (from TextView
        chronometer.<error descr="Call requires API level 11 (current min is 1): `setTextIsSelectable`">setTextIsSelectable(true)</error> // API 11

        // TODO: fix UClassLiteralExpression and uncomment, must be: error descr="Class requires API level 14 (current min is 1): `GridLayout`"
        GridLayout::class

        // Field access
        val field = OpcodeInfo.<error descr="Field requires API level 11 (current min is 1): `MAXIMUM_VALUE`">MAXIMUM_VALUE</error> // API 11


        val fillParent = LayoutParams.FILL_PARENT // API 1
        // This is a final int, which means it gets inlined
        val matchParent = LayoutParams.MATCH_PARENT // API 8
        // Field access: non final
        val batteryInfo = report!!.<error descr="Field requires API level 14 (current min is 1): `batteryInfo`">batteryInfo</error>

        // Enum access
        if (Build.VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> <= Build.VERSION_CODES.LOLLIPOP) {
            val mode = PorterDuff.Mode.<error descr="Field requires API level 11 (current min is 1): `OVERLAY`">OVERLAY</error> // API 11
        }
    }

    fun test(rect: Rect) {
        if (Build.VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= Build.VERSION_CODES.LOLLIPOP) {
            RectEvaluator(rect); // OK
        }
        if (Build.VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= Build.VERSION_CODES.LOLLIPOP) {
            if (rect != null) {
                RectEvaluator(rect); // OK
            }
        }
    }

    fun test2(rect: Rect) {
        if (Build.VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= Build.VERSION_CODES.LOLLIPOP) {
            RectEvaluator(rect); // OK
        }
    }

    fun test3(rect: Rect) {
        if (Build.VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= Build.VERSION_CODES.GINGERBREAD) {
            <error descr="Class requires API level 18 (current min is 1): `RectEvaluator`">RectEvaluator()</error>; // ERROR
        }
    }

    fun test4(rect: Rect) {
        if (Build.VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= Build.VERSION_CODES.LOLLIPOP) {
            System.out.println("Something");
            RectEvaluator(rect); // OK
        } else {
            <error descr="Class requires API level 18 (current min is 1): `RectEvaluator`">RectEvaluator(rect)</error>; // ERROR
        }
    }

    fun test5(rect: Rect) {
        if (Build.VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= Build.VERSION_CODES.CUPCAKE) {
            <error descr="Class requires API level 18 (current min is 1): `RectEvaluator`">RectEvaluator(rect)</error>; // ERROR
        } else {
            RectEvaluator(rect); // ERROR
        }
    }

    fun test(priority: Boolean) {
        if (android.os.Build.VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Class requires API level 14 (current min is 1): `GridLayout`">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): `getOrientation`">getOrientation()</error>; // Flagged
        }

        if (Build.VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Class requires API level 14 (current min is 1): `GridLayout`">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): `getOrientation`">getOrientation()</error>; // Flagged
        }

        if (<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= ICE_CREAM_SANDWICH) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Class requires API level 14 (current min is 1): `GridLayout`">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): `getOrientation`">getOrientation()</error>; // Flagged
        }

        if (Build.VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Class requires API level 14 (current min is 1): `GridLayout`">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): `getOrientation`">getOrientation()</error>; // Flagged
        }

        if (Build.VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            <error descr="Class requires API level 14 (current min is 1): `GridLayout`">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): `getOrientation`">getOrientation()</error>; // Flagged
        } else {
            GridLayout(null).getOrientation(); // Not flagged
        }

        if (Build.VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= 14) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Class requires API level 14 (current min is 1): `GridLayout`">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): `getOrientation`">getOrientation()</error>; // Flagged
        }

        if (VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= VERSION_CODES.ICE_CREAM_SANDWICH) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Class requires API level 14 (current min is 1): `GridLayout`">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): `getOrientation`">getOrientation()</error>; // Flagged
        }

        // Nested conditionals
        if (Build.VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= Build.VERSION_CODES.HONEYCOMB) {
            if (priority) {
                <error descr="Class requires API level 14 (current min is 1): `GridLayout`">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): `getOrientation`">getOrientation()</error>; // Flagged
            } else {
                <error descr="Class requires API level 14 (current min is 1): `GridLayout`">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): `getOrientation`">getOrientation()</error>; // Flagged
            }
        } else {
            GridLayout(null).getOrientation(); // Flagged
        }

        // Nested conditionals 2
        if (Build.VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= Build.VERSION_CODES.JELLY_BEAN) {
            if (priority) {
                <error descr="Class requires API level 14 (current min is 1): `GridLayout`">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): `getOrientation`">getOrientation()</error>; // Not flagged
            } else {
                <error descr="Class requires API level 14 (current min is 1): `GridLayout`">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): `getOrientation`">getOrientation()</error>; // Not flagged
            }
        } else {
            <error descr="Class requires API level 14 (current min is 1): `GridLayout`">GridLayout(null)</error>; // Flagged
        }
    }

    fun test2(priority: Boolean) {
        if (android.os.Build.VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= VERSION_CODES.JELLY_BEAN) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Class requires API level 14 (current min is 1): `GridLayout`">GridLayout(null)</error>; // Flagged
        }

        if (android.os.Build.VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= 16) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Class requires API level 14 (current min is 1): `GridLayout`">GridLayout(null)</error>; // Flagged
        }

        if (android.os.Build.VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= 13) {
            <error descr="Class requires API level 14 (current min is 1): `GridLayout`">GridLayout(null)</error>.<error descr="Call requires API level 14 (current min is 1): `getOrientation`">getOrientation()</error>; // Flagged
        } else {
            GridLayout(null); // Flagged
        }

        if (Build.VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= Build.VERSION_CODES.JELLY_BEAN) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Class requires API level 14 (current min is 1): `GridLayout`">GridLayout(null)</error>; // Flagged
        }

        if (<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= JELLY_BEAN) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Class requires API level 14 (current min is 1): `GridLayout`">GridLayout(null)</error>; // Flagged
        }

        if (Build.VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= Build.VERSION_CODES.JELLY_BEAN) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Class requires API level 14 (current min is 1): `GridLayout`">GridLayout(null)</error>; // Flagged
        }

        if (Build.VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> < Build.VERSION_CODES.JELLY_BEAN) {
            <error descr="Class requires API level 14 (current min is 1): `GridLayout`">GridLayout(null)</error>; // Flagged
        } else {
            GridLayout(null).getOrientation(); // Not flagged
        }

        if (Build.VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= 16) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Class requires API level 14 (current min is 1): `GridLayout`">GridLayout(null)</error>; // Flagged
        }

        if (VERSION.<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= VERSION_CODES.JELLY_BEAN) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Class requires API level 14 (current min is 1): `GridLayout`">GridLayout(null)</error>; // Flagged
        }
    }

    fun test(textView: TextView) {
        if (textView.<error descr="Call requires API level 14 (current min is 1): `isSuggestionsEnabled`">isSuggestionsEnabled()</error>) {
            //ERROR
        }
        if (textView.<error descr="Call requires API level 14 (current min is 1): `isSuggestionsEnabled`">isSuggestionsEnabled</error>) {
            //ERROR
        }

        if (<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= JELLY_BEAN && textView.isSuggestionsEnabled()) {
            //NO ERROR
        }

        if (<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> >= JELLY_BEAN && textView.isSuggestionsEnabled) {
            //NO ERROR
        }

        if (<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> < JELLY_BEAN && textView.<error descr="Call requires API level 14 (current min is 1): `isSuggestionsEnabled`">isSuggestionsEnabled()</error>) {
            //ERROR
        }

        if (<error descr="Field requires API level 4 (current min is 1): `SDK_INT`">SDK_INT</error> < JELLY_BEAN && textView.<error descr="Call requires API level 14 (current min is 1): `isSuggestionsEnabled`">isSuggestionsEnabled</error>) {
            //ERROR
        }
    }

    @TargetApi(JELLY_BEAN)
    fun testWithAnnotation(textView: TextView) {
        if (textView.isSuggestionsEnabled()) {
            //NO ERROR, annotation
        }

        if (textView.isSuggestionsEnabled) {
            //NO ERROR, annotation
        }
    }

    // Return type
    internal // API 14
    val gridLayout: GridLayout?
        get() = null

    private val report: ApplicationErrorReport?
        get() = null
}
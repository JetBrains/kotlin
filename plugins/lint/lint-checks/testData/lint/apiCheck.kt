
// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintNewApiInspection
// INSPECTION_CLASS2: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintInlinedApiInspection
// INSPECTION_CLASS3: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintOverrideInspection

import android.animation.RectEvaluator
import android.annotation.TargetApi
import org.w3c.dom.DOMError
import org.w3c.dom.DOMErrorHandler
import org.w3c.dom.DOMLocator

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.app.Activity
import android.app.ApplicationErrorReport
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.Build
import android.widget.*
import dalvik.bytecode.OpcodeInfo

import android.os.Build.VERSION
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH
import android.os.Build.VERSION_CODES.JELLY_BEAN
import android.os.Bundle
import android.system.ErrnoException
import android.widget.TextView

@Suppress("SENSELESS_COMPARISON", "UNUSED_EXPRESSION", "UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
class ApiCallTest: Activity() {

    fun method(chronometer: Chronometer, locator: DOMLocator) {
        chronometer.<error descr="Call requires API level 16 (current min is 1): android.view.View#setBackground">setBackground</error>(null)

        // Ok
        Bundle().getInt("")

        // Ok, this constant is inlined
        View.<warning descr="Field requires API level 16 (current min is 1): android.view.View#SYSTEM_UI_FLAG_FULLSCREEN">SYSTEM_UI_FLAG_FULLSCREEN</warning>

        // Virtual call
        <error descr="Call requires API level 11 (current min is 1): android.app.Activity#getActionBar">getActionBar</error>() // API 11
        <error descr="Call requires API level 11 (current min is 1): android.app.Activity#getActionBar">actionBar</error> // API 11

        // Class references (no call or field access)
        val error: DOMError? = null // API 8
        val clz = DOMErrorHandler::class // API 8

        // Method call
        chronometer.<error descr="Call requires API level 3 (current min is 1): android.widget.Chronometer#getOnChronometerTickListener">onChronometerTickListener</error> // API 3

        // Inherited method call (from TextView
        chronometer.<error descr="Call requires API level 11 (current min is 1): android.widget.TextView#setTextIsSelectable">setTextIsSelectable</error>(true) // API 11

        GridLayout::class

        // Field access
        val field = OpcodeInfo.<warning descr="Field requires API level 11 (current min is 1): dalvik.bytecode.OpcodeInfo#MAXIMUM_VALUE">MAXIMUM_VALUE</warning> // API 11


        val fillParent = LayoutParams.FILL_PARENT // API 1
        // This is a final int, which means it gets inlined
        val matchParent = LayoutParams.MATCH_PARENT // API 8
        // Field access: non final
        val batteryInfo = report!!.<error descr="Field requires API level 14 (current min is 1): android.app.ApplicationErrorReport#batteryInfo">batteryInfo</error>

        // Enum access
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            val mode = PorterDuff.Mode.<error descr="Field requires API level 11 (current min is 1): android.graphics.PorterDuff.Mode#OVERLAY">OVERLAY</error> // API 11
        }
    }

    fun test(rect: Rect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            RectEvaluator(rect); // OK
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (rect != null) {
                RectEvaluator(rect); // OK
            }
        }
    }

    fun test2(rect: Rect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            RectEvaluator(rect); // OK
        }
    }

    fun test3(rect: Rect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            <error descr="Call requires API level 18 (current min is 1): android.animation.RectEvaluator#RectEvaluator">RectEvaluator</error>(); // ERROR
        }
    }

    fun test4(rect: Rect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            System.out.println("Something");
            RectEvaluator(rect); // OK
        } else {
            <error descr="Call requires API level 21 (current min is 1): android.animation.RectEvaluator#RectEvaluator">RectEvaluator</error>(rect); // ERROR
        }
    }

    fun test5(rect: Rect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
            <error descr="Call requires API level 21 (current min is 1): android.animation.RectEvaluator#RectEvaluator">RectEvaluator</error>(rect); // ERROR
        } else {
            <error descr="Call requires API level 21 (current min is 1): android.animation.RectEvaluator#RectEvaluator">RectEvaluator</error>(rect); // ERROR
        }
    }

    fun test(priority: Boolean, layout: ViewGroup) {
        if (layout is LinearLayout) {}
        layout as? LinearLayout

        if (layout is GridLayout) {}
        layout as? GridLayout

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#GridLayout">GridLayout</error>(null).<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#GridLayout">GridLayout</error>(null).<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
        }

        if (SDK_INT >= ICE_CREAM_SANDWICH) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#GridLayout">GridLayout</error>(null).<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#GridLayout">GridLayout</error>(null).<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            <error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#GridLayout">GridLayout</error>(null).<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
        } else {
            GridLayout(null).getOrientation(); // Not flagged
        }

        if (Build.VERSION.SDK_INT >= 14) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#GridLayout">GridLayout</error>(null).<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
        }

        if (VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#GridLayout">GridLayout</error>(null).<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
        }

        // Nested conditionals
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (priority) {
                <error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#GridLayout">GridLayout</error>(null).<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
            } else {
                <error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#GridLayout">GridLayout</error>(null).<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
            }
        } else {
            <error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#GridLayout">GridLayout</error>(null).<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
        }

        // Nested conditionals 2
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (priority) {
                GridLayout(null).getOrientation(); // Not flagged
            } else {
                GridLayout(null).getOrientation(); // Not flagged
            }
        } else {
            <error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#GridLayout">GridLayout</error>(null); // Flagged
        }
    }

    fun test2(priority: Boolean) {
        if (android.os.Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#GridLayout">GridLayout</error>(null); // Flagged
        }

        if (android.os.Build.VERSION.SDK_INT >= 16) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#GridLayout">GridLayout</error>(null); // Flagged
        }

        if (android.os.Build.VERSION.SDK_INT >= 13) {
            <error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#GridLayout">GridLayout</error>(null).<error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#getOrientation">getOrientation</error>(); // Flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#GridLayout">GridLayout</error>(null); // Flagged
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#GridLayout">GridLayout</error>(null); // Flagged
        }

        if (SDK_INT >= JELLY_BEAN) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#GridLayout">GridLayout</error>(null); // Flagged
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#GridLayout">GridLayout</error>(null); // Flagged
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            <error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#GridLayout">GridLayout</error>(null); // Flagged
        } else {
            GridLayout(null).getOrientation(); // Not flagged
        }

        if (Build.VERSION.SDK_INT >= 16) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#GridLayout">GridLayout</error>(null); // Flagged
        }

        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
            GridLayout(null).getOrientation(); // Not flagged
        } else {
            <error descr="Call requires API level 14 (current min is 1): android.widget.GridLayout#GridLayout">GridLayout</error>(null); // Flagged
        }
    }

    fun test(textView: TextView) {
        if (textView.<error descr="Call requires API level 14 (current min is 1): android.widget.TextView#isSuggestionsEnabled">isSuggestionsEnabled</error>()) {
            //ERROR
        }
        if (textView.<error descr="Call requires API level 14 (current min is 1): android.widget.TextView#isSuggestionsEnabled">isSuggestionsEnabled</error>) {
            //ERROR
        }

        if (SDK_INT >= JELLY_BEAN && textView.<error descr="Call requires API level 14 (current min is 1): android.widget.TextView#isSuggestionsEnabled">isSuggestionsEnabled</error>()) {
            //NO ERROR
        }

        if (SDK_INT >= JELLY_BEAN && textView.<error descr="Call requires API level 14 (current min is 1): android.widget.TextView#isSuggestionsEnabled">isSuggestionsEnabled</error>) {
            //NO ERROR
        }

        if (SDK_INT < JELLY_BEAN && textView.<error descr="Call requires API level 14 (current min is 1): android.widget.TextView#isSuggestionsEnabled">isSuggestionsEnabled</error>()) {
            //ERROR
        }

        if (SDK_INT < JELLY_BEAN && textView.<error descr="Call requires API level 14 (current min is 1): android.widget.TextView#isSuggestionsEnabled">isSuggestionsEnabled</error>) {
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

    fun testCatch() {
        try {

        } catch (e: <error descr="Class requires API level 21 (current min is 1): android.system.ErrnoException">ErrnoException</error>) {

        }
    }

    fun testOverload() {
        // this overloaded addOval available only on API Level 21
        Path().<error descr="Call requires API level 21 (current min is 1): android.graphics.Path#addOval">addOval</error>(0f, 0f, 0f, 0f, Path.Direction.CW)
    }

    // Return type
    internal // API 14
    val gridLayout: GridLayout?
        get() = null

    private val report: ApplicationErrorReport?
        get() = null
}
package com.example.databinding

import android.content.Context
import android.content.res.Resources
import androidx.databinding.*
import androidx.databinding.adapters.AdapterViewBindingAdapter
import androidx.annotation.IntegerRes
import android.util.Log
import android.view.*
import android.widget.*


interface Displayable {
    fun displayString(res: Resources): String
}


@InverseBindingMethods(
        InverseBindingMethod(type = Spinner::class, attribute = "selectionEnum", method = "getSelectedItem", event = "app:selectionAttrChanged")
)
open class EnumAdapter<DispEnum>(val context: Context,
                                 enumClass: Class<DispEnum>,
                                 @IntegerRes var resource: Int = android.R.layout.simple_spinner_item,
                                 @IntegerRes var dropDownResource: Int = android.R.layout.simple_spinner_dropdown_item,
                                 val isOptional: Boolean = false) :
        BaseAdapter() where DispEnum : Displayable, DispEnum : Enum<DispEnum>
{
    val inflater: LayoutInflater = LayoutInflater.from(context)
    val konstants: Array<DispEnum> = enumClass.enumConstants

    override fun getItem(position: Int): DispEnum? {
        if (isOptional) {
            return if (position == 0) null else konstants[position - 1]
        }
        return konstants[position]
    }

    override fun getItemId(position: Int): Long = position + 0L

    override fun getCount(): Int = konstants.size + if (isOptional) 1 else 0

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent, resource)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent, dropDownResource)
    }

    private fun createViewFromResource(position: Int, convertView: View?, parent: ViewGroup, resource: Int): View {
        val view = convertView ?: inflater.inflate(resource, parent, false)
        val textView: TextView
        try {
            textView = view as TextView
        } catch (e: ClassCastException) {
            Log.e("EnumAdapter", "You must supply a resource ID for a TextView")
            return view
        }

        val enum = getItem(position)
        textView.text = enum?.displayString(context.resources) ?: "-----"
        return view
    }

    companion object {
        @JvmStatic fun optionalPosition(e: Enum<*>?): Int {
            e?.let { return it.ordinal + 1}
            return 0
        }
    }
}

@BindingAdapter("app:selectionEnum", "app:selectionChanged", "app:selectionAttrChanged", requireAll = false)
fun bindSpinnerEnum(spinner: Spinner, newValue: Enum<*>?,
                    itemSelectedListener: AdapterViewBindingAdapter.OnItemSelected?,
                    changeListener: InverseBindingListener?) {
    val spinnerAdapter = spinner.adapter
    if (spinnerAdapter !is EnumAdapter<*>) {
        throw UnsupportedOperationException("app:selectionEnum attribute on Spinner requires EnumAdapter")
    }
    if (changeListener == null && itemSelectedListener == null)
        spinner.onItemSelectedListener = null
    else
        spinner.onItemSelectedListener = AdapterViewBindingAdapter.OnItemSelectedComponentListener(itemSelectedListener, nothingingSelectedListener, changeListener)

    val isOptional = spinnerAdapter.isOptional
    if (!isOptional && newValue == null) {
        throw IllegalStateException("Can't set a null value for app:selectionEnum, adapter.isOptional is false")
    }
    if (spinner.selectedItem != newValue) {
        val sel = if (isOptional) EnumAdapter.optionalPosition(newValue) else newValue!!.ordinal
        spinner.setSelection(sel)
    }
}

val nothingingSelectedListener = object : AdapterViewBindingAdapter.OnNothingSelected {
    override fun onNothingSelected(parent: AdapterView<*>?) {
    }
}
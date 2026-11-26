// RUN_PIPELINE_TILL: CODEGEN
// WITH_STDLIB
// https://issuetracker.google.com/590731116
@file:OptIn(kotlinx.parcelize.Experimental::class)
@file:Suppress("PARCELABLE_TYPE_NOT_SUPPORTED", "PARCELABLE_TYPE_CONTAINS_NOT_SUPPORTED")

package test

import kotlinx.parcelize.*
import android.os.Parcelable

data class MyData(val x: String)
class RegularClass(val x: String)
interface MyInterface
enum class MyEnum { A }
object MyObject
data object MyDataObject
@JvmInline
value class MyValueClass(val x: Int)

typealias RegularAlias = RegularClass

@Parcelize
class ParcelableWithNonDataClasses(
    val regular: <!INAPPLICABLE_DATA_CLASS!>@DataClass<!> RegularClass,
    val nullableRegular: <!INAPPLICABLE_DATA_CLASS!>@DataClass<!> RegularClass?,
    val iface: <!INAPPLICABLE_DATA_CLASS!>@DataClass<!> MyInterface,
    val enumVal: <!INAPPLICABLE_DATA_CLASS!>@DataClass<!> MyEnum,
    val obj: <!INAPPLICABLE_DATA_CLASS!>@DataClass<!> MyObject,
    val dataObj: <!INAPPLICABLE_DATA_CLASS!>@DataClass<!> MyDataObject,
    val valueClass: <!INAPPLICABLE_DATA_CLASS!>@DataClass<!> MyValueClass,
    val stringVal: <!INAPPLICABLE_DATA_CLASS!>@DataClass<!> String,
    val listAnnotated: <!INAPPLICABLE_DATA_CLASS!>@DataClass<!> List<MyData>,
    val listOfNonData: List<<!INAPPLICABLE_DATA_CLASS!>@DataClass<!> RegularClass>,
    val aliasNonData: <!INAPPLICABLE_DATA_CLASS!>@DataClass<!> RegularAlias,
    val rawValueRegular: @RawValue <!INAPPLICABLE_DATA_CLASS!>@DataClass<!> RegularClass,
) : Parcelable

@Parcelize
class VarargData(
    vararg val items: @DataClass MyData,
) : Parcelable

@Parcelize
class VarargNonData(
    vararg val items: <!INAPPLICABLE_DATA_CLASS!>@DataClass<!> RegularClass,
) : Parcelable

class TypeParameter<T>(val t: <!INAPPLICABLE_DATA_CLASS!>@DataClass<!> T)

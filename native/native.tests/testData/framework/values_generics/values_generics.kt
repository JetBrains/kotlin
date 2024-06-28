/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

// All classes and methods should be used in tests
@file:Suppress("UNUSED")

package conversions

// Generics
abstract class BaseData{
    abstract fun asString():String
}

data class SomeData(val num:Int = 42):BaseData() {
    override fun asString(): String = num.toString()
}

data class SomeOtherData(val str:String):BaseData() {
    fun anotherFun(){}
    override fun asString(): String = str
}

interface NoGeneric<T> {
  fun myVal():T
}

data class SomeGeneric<T>(val t:T):NoGeneric<T>{
  override fun myVal(): T = t
}

class GenOpen<T:Any?>(val arg:T)
class GenNonNull<T:Any>(val arg:T)

class GenCollectionsNull<T>(val arg: T, val coll: List<T>)
class GenCollectionsNonNull<T:Any>(val arg: T, val coll: List<T>)

//Force @class declaration at top of file with Objc variance
object ForceUse {
    val gvo = GenVarOut(SomeData())
}

class GenVarOut<out T:Any>(val arg:T)

class GenVarIn<in T:Any>(tArg:T){
    private val t = tArg

    fun valString():String = t.toString()

    fun goIn(t:T){
        //Just taking a val
    }
}

class GenVarUse<T:Any>(val arg:T){
    fun varUse(a:GenVarUse<out T>, b:GenVarUse<in T>){
        //Should complile but do nothing
    }
}

fun variCoType():GenVarOut<BaseData>{
    val compileVarOutSD:GenVarOut<SomeData> = GenVarOut(SomeData(890))
    val compileVarOut:GenVarOut<BaseData> = compileVarOutSD
    return compileVarOut
}

fun variContraType():GenVarIn<SomeData>{
    val compileVariIn:GenVarIn<BaseData> = GenVarIn(SomeData(1890))
    val compileVariInSD:GenVarIn<SomeData> = compileVariIn
    return compileVariInSD
}

open class GenBase<T:Any>(val t:T)
class GenEx<TT:Any, T:Any>(val myT:T, baseT:TT):GenBase<TT>(baseT)
class GenEx2<T:Any, S:Any>(val myT:S, baseT:T):GenBase<T>(baseT)

class GenExAny<TT:Any, T:Any>(val myT:T, baseT:TT):GenBase<Any>(baseT)

class GenNullability<T:Any>(val arg: T, val nArg:T?){
    fun asNullable():T? = arg
    val pAsNullable:T?
        get() = arg
}

fun starGeneric(arg: GenNonNull<*>):Any{
    return arg.arg
}

class GenOuter<A:Any>(val a:A){
    class GenNested<B:Any>(val b:B)
    inner class GenInner<C:Any>(val c:C, val aInner:A) {
        fun outerFun(): A = a
        val outerVal: A = a
    }
}

class GenOuterSame<A:Any>(val a:A){
    class GenNestedSame<A:Any>(val a:A)
    inner class GenInnerSame<A:Any>(val a:A)
    class NestedNoGeneric()
}

fun genInnerFunc(obj: GenOuter<SomeOtherData>.GenInner<SomeData>) {}
fun <A:Any, C:Any> genInnerFuncAny(obj: GenOuter<A>.GenInner<C>){}

fun genInnerCreate(): GenOuter<SomeData>.GenInner<SomeOtherData> =
        GenOuter(SomeData(33)).GenInner(SomeOtherData("ppp"), SomeData(77))

class GenOuterBlank(val sd: SomeData) {
    inner class GenInner<T>(val arg: T){
        fun fromOuter(): SomeData = sd
    }
}

class GenOuterBlank2<T>(val oarg: T) {
    inner class GenInner(val arg: T){
        fun fromOuter(): T = oarg
    }
}

class GenOuterDeep<T>(val oarg: T) {
    inner class GenShallowInner(){
        inner class GenDeepInner(){
            fun o(): T = oarg
        }
    }
}

class GenOuterDeep2() {
    inner class Before()
    inner class GenShallowOuterInner() {
        inner class GenShallowInner<T>() {
            inner class GenDeepInner()
        }
    }
    inner class After()
}

class GenBothBlank(val a: SomeData) {
    inner class GenInner(val b: SomeOtherData)
}

class GenClashId<id : Any, id_ : Any>(val arg: id, val arg2: id_){
    fun x(): Any = "Foo"
}

class GenClashClass<ValuesGenericsClashingData : Any, NSArray : Any, int32_t : Any>(
        val arg: ValuesGenericsClashingData, val arg2: NSArray, val arg3: int32_t
) {
    fun sd(): SomeData = SomeData(88)
    fun list(): List<SomeData> = listOf(SomeData(11), SomeData(22))
    fun int(): Int = 55
    fun clash(): ClashingData = ClashingData("aaa")
}

data class ClashingData(val str: String)

class GenClashNames<ValuesGenericsClashnameClass, ValuesGenericsClashnameProtocol, ValuesGenericsClashnameParam, ValuesGenericsValues_genericsKt>() {
    fun foo() = ClashnameClass("nnn")

    fun bar(): ClashnameProtocol = object : ClashnameProtocol{
        override val str = "qqq"
    }

    fun baz(arg: ClashnameParam): Boolean {
        return arg.str == "meh"
    }
}

class GenClashEx<ValuesGenericsClashnameClass>: ClashnameClass("ttt"){
    fun foo() = ClashnameClass("nnn")
}

open class ClashnameClass(val str: String)
interface ClashnameProtocol {
    val str: String
}
data class ClashnameParam(val str: String)

class GenExClash<ValuesGenericsSomeData:Any>(val myT:ValuesGenericsSomeData):GenBase<SomeData>(SomeData(55))

class SelfRef : GenBasic<SelfRef>()

open class GenBasic<T>()

//Extensions
fun <T:Any> GenNonNull<T>.foo(): T = arg

class StarProjectionInfiniteRecursion<T : StarProjectionInfiniteRecursion<T>>

fun testStarProjectionInfiniteRecursion(x: StarProjectionInfiniteRecursion<*>) {}
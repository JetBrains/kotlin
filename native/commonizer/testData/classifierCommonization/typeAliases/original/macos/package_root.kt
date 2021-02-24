class A

// Lifted up type aliases:
typealias B = A // class at the RHS
typealias C = B // TA at the RHS, expanded to the same class
typealias C2 = C // 2x TA at the RHS, expanded to the same class
typealias C3 = C2 // 3x TA at the RHS, expanded to the same class

typealias D = A // class/TA expanded to the same class at the RHS
typealias E = B // different TAs expanded to the same class at the RHS

typealias F = List<String> // parameterized type at the RHS
typealias G = List<String> // different parameterized types at the RHS

typealias H<T> = List<T> // TA with own parameters
typealias I<R> = List<R> // TAs with own parameters with different names

typealias I2<T> = List<T>
typealias I3<R> = I2<R>
typealias I4 = I2<String>

typealias I5<V, K> = Map<K, V>
typealias I6<T, R> = I5<T, R>

typealias I7<K, V> = Map<K, V>
typealias I8<T, R> = I7<R, T>
typealias I9<Q, W> = I8<Q, W>

typealias J<T> = Function<T> // function type at the RHS
typealias K<R> = Function<R> // function types with different type parameter names
typealias L<T> = Function<T> // different kinds of function types
typealias M = () -> Unit // same return type
typealias N = () -> Unit // different return types
typealias O = (String) -> Int // same argument type
typealias P = (String) -> Int // different argument types

typealias Q<T> = (List<M>) -> Map<T, O> // something complex
typealias R = Function<C> // something complex

// Type aliases converted to expect classes:
typealias S = String
typealias T = String

// Nullability:
typealias U = A // same nullability of the RHS class
typealias V = A // different nullability of the RHS class
typealias W = U // same nullability of the RHS TA
typealias X = U // different nullability of the RHS TA
typealias Y = V // TA at the RHS with the different nullability of own RHS

// Supertypes:
typealias FILE = __sFILE
final class __sFILE : kotlinx.cinterop.CStructVar {}

typealias uuid_t = __darwin_uuid_t
typealias __darwin_uuid_t = kotlinx.cinterop.CArrayPointer<kotlinx.cinterop.UByteVar>

val uuid: uuid_t get() = TODO()

// Type alias chain that is present in one target only:
class AA
typealias BB = AA
typealias CC = BB
typealias DD = CC
typealias EE = List<String>
typealias FF = EE
typealias GG = FF
typealias HH<T> = List<T>
typealias II<R> = HH<R>
typealias JJ<S> = II<S>
typealias KK = HH<String>
typealias LL = II<String>
typealias MM = JJ<String>
typealias NN = (String) -> Int
typealias OO = NN
typealias PP = OO
typealias QQ<T1, T2> = (T1) -> T2
typealias RR<R1, R2> = QQ<R1, R2>
typealias SS<S1, S2> = RR<S1, S2>
typealias TT = QQ<String, Int>
typealias UU = RR<String, Int>
typealias VV = SS<String, Int>
typealias WW = TT
typealias XX = UU
typealias YY = VV
typealias ZZ<T> = QQ<T, Int>
typealias AAA = ZZ<String>
typealias BBB = AAA
typealias CCC<T> = QQ<String, T>
typealias DDD = CCC<Int>
typealias EEE = DDD
typealias FFF<R> = RR<R, Int>
typealias GGG = FFF<String>
typealias HHH = GGG
typealias III<R> = RR<String, R>
typealias JJJ = III<Int>
typealias KKK = JJJ
typealias LLL<S> = SS<S, Int>
typealias MMM = LLL<String>
typealias NNN = MMM
typealias OOO<S> = SS<String, S>
typealias PPP = OOO<Int>
typealias QQQ = PPP

fun getBB(): BB = TODO()
fun getCC(): CC = TODO()
fun getDD(): DD = TODO()
fun getEE(): EE = TODO()
fun getFF(): FF = TODO()
fun getGG(): GG = TODO()
fun <U> getHH(): HH<U> = TODO()
fun <V> getII(): II<V> = TODO()
fun <W> getJJ(): JJ<W> = TODO()
fun getKK(): KK = TODO()
fun getLL(): LL = TODO()
fun getMM(): MM = TODO()
fun getNN(): NN = TODO()
fun getOO(): OO = TODO()
fun getPP(): PP = TODO()
fun <U1, U2> getQQ(): QQ<U1, U2> = TODO()
fun <V1, V2> getRR(): RR<V1, V2> = TODO()
fun <W1, W2> getSS(): SS<W1, W2> = TODO()
fun getTT(): TT = TODO()
fun getUU(): UU = TODO()
fun getVV(): VV = TODO()
fun getWW(): WW = TODO()
fun getXX(): XX = TODO()
fun getYY(): YY = TODO()
fun <U> getZZ(): ZZ<U> = TODO()
fun getAAA(): AAA = TODO()
fun getBBB(): BBB = TODO()
fun <U> getCCC(): CCC<U> = TODO()
fun getDDD(): DDD = TODO()
fun getEEE(): EEE = TODO()
fun <V> getFFF(): FFF<V> = TODO()
fun getGGG(): GGG = TODO()
fun getHHH(): HHH = TODO()
fun <V> getIII(): III<V> = TODO()
fun getJJJ(): JJJ = TODO()
fun getKKK(): KKK = TODO()
fun <W> getLLL(): LLL<W> = TODO()
fun getMMM(): MMM = TODO()
fun getNNN(): NNN = TODO()
fun <W> getOOO(): OOO<W> = TODO()
fun getPPP(): PPP = TODO()
fun getQQQ(): QQQ = TODO()

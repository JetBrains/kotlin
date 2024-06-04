// CURIOUS_ABOUT: <init>, <clinit>, getStrAttr, access$requiredValue
import com.jetbrains.rhizomedb.*

@GeneratedEntityType
class MyEntity(override val eid: EID) : Entity {
    @ValueAttribute(Indexing.INDEXED)
    val str1: String by str1Attr

    @ValueAttribute
    val str2: String? by str2Attr

    @Many
    @ValueAttribute
    val strs: Set<String> by strsAttr
}

fun ChangeScope.MyEntity(str1: String, str2: String? = null, vararg strs: String): MyEntity {
    return MyEntity.new {
        it[MyEntity.str1Attr] = str1
        it[MyEntity.str2Attr] = str2
        it[MyEntity.strsAttr] = strs.toSet()
    }
}

fun ChangeScope.boxImpl(): String {
    register(MyEntity)

    assertEquals(0, MyEntity.all().size) { return "$it: unexpected MyEntity" }
    val e1 = MyEntity("42")
    val e2 = MyEntity("42", "43", "Hello", "World")
    val e3 = MyEntity("43", null, "lol", "lol1")
    assertEquals(3, MyEntity.all().size) { return "$it: should be exactly three MyEntity" }

    val es = entities(MyEntity.str1Attr, "42").toList().sortedBy { it.eid }
    assertEquals(listOf(e1, e2), es) { return "$it: list of entitites are not the same" }

    val (es1, es2) = es
    assertEquals("42", es1.str1) { return "$it: unexpected attribute value" }
    assertEquals(null, es1.str2) { return "$it: unexpected attribute value" }
    assertEquals(emptySet(), es1.strs) { return "$it: unexpected attribute value" }

    assertEquals("42", es2.str1) { return "$it: unexpected attribute value" }
    assertEquals("43", es2.str2) { return "$it: unexpected attribute value" }
    assertEquals(setOf("Hello", "World"), es2.strs) { return "$it: unexpected attribute value" }

    return "OK"
}

fun box(): String = changeBox(ChangeScope::boxImpl)

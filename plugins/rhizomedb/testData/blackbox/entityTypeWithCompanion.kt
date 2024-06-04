import com.jetbrains.rhizomedb.*

@GeneratedEntityType
class MyEntity(override val eid: EID) : Entity {
    companion object {
        val X = 42
    }
}

fun ChangeScope.boxImpl(): String {
    register(MyEntity)
    assertEquals(0, MyEntity.all().size) { return "$it: unexpected MyEntity" }
    MyEntity.new {}
    assertEquals(1, MyEntity.all().size) { return "$it: should be exactly one MyEntity" }
    val x = entity(EntityType.Ident, MyEntity.entityTypeIdent)!!
    assertEquals("<unknown>", x.module) { return "$it: wrong module name" }

    return "OK"
}

fun box(): String = changeBox(ChangeScope::boxImpl)

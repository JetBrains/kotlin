import com.jetbrains.rhizomedb.*

interface MarkerInterface

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    <!NONE_APPLICABLE("")!>companion object : MarkerInterface {
        val X = 42
    }<!>
}
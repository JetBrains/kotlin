import com.jetbrains.rhizomedb.*

interface MarkerInterface

@GeneratedEntityType
class MyEntity : Entity {
    companion object : MarkerInterface {
        val X = 42
    }
}

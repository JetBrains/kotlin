import java.util.Random

fun getRandomEnumEntry() =
        with (Enum.values()) {
            get(Random().nextInt(size))
        }
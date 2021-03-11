sealed interface KotlinSealedInterface
sealed class KotlinSealedClass

interface KotlinInterface: KotlinSealedInterface
class KotlinClass: KotlinSealedClass()
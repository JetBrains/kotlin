import java.io.Serializable

internal open class BaseEntity : Serializable {
    var id: Long? = null

    companion object {
        private const val serialVersionUID = 1L
    }
}

internal class AuditableEntity : BaseEntity() {
    var createdBy: String? = null
    var modifiedBy: String? = null

    companion object {
        private const val serialVersionUID = 2L
    }
}
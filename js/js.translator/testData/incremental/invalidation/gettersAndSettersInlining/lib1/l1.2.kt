open class Parent(objectName: String) {
   var objectName: String
        get() = _name
        set(objectName) {
          isValid = true
          _name = objectName
        }

    var isValid = false
        private set

   private var _name: String = objectName;
}

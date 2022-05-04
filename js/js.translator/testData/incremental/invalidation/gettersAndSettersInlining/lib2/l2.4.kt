class Child(objectName: String) : Parent(objectName) {
   override var objectName: String
        get() = _name
        set(objectName) {
           _name = objectName
           isValid = true
        }

  private var _name: String = objectName

   override var isValid = false
        private set
}

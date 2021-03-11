// "Move type parameter constraint to 'where' clause" "true"
class A<<caret>T : Cloneable> where T : Comparable<*> {
}

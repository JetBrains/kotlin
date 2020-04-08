// "Create type parameter 'Test' in class 'C'" "false"
// ACTION: Create class 'Test'
// ACTION: Create test
// ERROR: Unresolved reference: Test
class C : <caret>Test() {

}
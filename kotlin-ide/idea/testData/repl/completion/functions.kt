>> fun foo() {}
>> fun foo(i: Int) {}
>> fun foo(s: String = "asd") {}
>> foo(3)
-- fo
// EXIST: { itemText: "foo", tailText:"()" }
// EXIST: { itemText: "foo", tailText:"(i: Int)" }
// EXIST: { itemText: "foo", tailText:"(s: String = ...)" }
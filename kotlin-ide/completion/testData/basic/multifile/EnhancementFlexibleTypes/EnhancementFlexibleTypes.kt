fun temp() {
    EnhancementFlexibleTypes.test<caret>
}

// EXIST: { lookupString: "testNotNull", tailText:"(string: String)" }
// EXIST: { lookupString: "testNullable", tailText:"(string: String?)" }
fun box(stepId: Int, isWasm: Boolean): String {
    val a = ClassA()
    when (stepId) {
        0 -> {
            if (a.testExtension() != "testExtension fakeOverrideExtension 0") return "Fail extension"
            if (a.testGetProperty() != "testGetProperty fakeOverrideGetProperty 0") return "Fail getProperty"
            if (a.testSetPropertySetter() != "testSetPropertySetter fakeOverrideSetProperty setter 0") return "Fail setPropertySetter"
            if (a.testSetPropertyGetter() != "testSetPropertySetter fakeOverrideSetProperty setter 0 fakeOverrideSetProperty getter 0") return a.testSetPropertyGetter()
        }
        1 -> {
            if (a.testExtension() != "testExtension fakeOverrideExtension 1") return "Fail extension"
            if (a.testGetProperty() != "testGetProperty fakeOverrideGetProperty 0") return "Fail getProperty"
            if (a.testSetPropertySetter() != "testSetPropertySetter fakeOverrideSetProperty setter 0") return "Fail setPropertySetter"
            if (a.testSetPropertyGetter() != "testSetPropertySetter fakeOverrideSetProperty setter 0 fakeOverrideSetProperty getter 0") return a.testSetPropertyGetter()
        }
        2 -> {
            if (a.testExtension() != "testExtension fakeOverrideExtension 1") return "Fail extension"
            if (a.testGetProperty() != "testGetProperty fakeOverrideGetProperty 2") return "Fail getProperty"
            if (a.testSetPropertySetter() != "testSetPropertySetter fakeOverrideSetProperty setter 0") return "Fail setPropertySetter"
            if (a.testSetPropertyGetter() != "testSetPropertySetter fakeOverrideSetProperty setter 0 fakeOverrideSetProperty getter 0") return a.testSetPropertyGetter()
        }
        3 -> {
            if (a.testExtension() != "testExtension fakeOverrideExtension 1") return "Fail extension"
            if (a.testGetProperty() != "testGetProperty fakeOverrideGetProperty 2") return "Fail getProperty"
            if (a.testSetPropertySetter() != "testSetPropertySetter fakeOverrideSetProperty setter 3") return "Fail setPropertySetter"
            if (a.testSetPropertyGetter() != "testSetPropertySetter fakeOverrideSetProperty setter 3 fakeOverrideSetProperty getter 0") return a.testSetPropertyGetter()
        }
        4 -> {
            if (a.testExtension() != "testExtension fakeOverrideExtension 1") return "Fail extension"
            if (a.testGetProperty() != "testGetProperty fakeOverrideGetProperty 2") return "Fail getProperty"
            if (a.testSetPropertySetter() != "testSetPropertySetter fakeOverrideSetProperty setter 3") return "Fail setPropertySetter"
            if (a.testSetPropertyGetter() != "testSetPropertySetter fakeOverrideSetProperty setter 3 fakeOverrideSetProperty getter 4") return a.testSetPropertyGetter()
        }
        else -> return "Unknown"
    }
    return "OK"
}

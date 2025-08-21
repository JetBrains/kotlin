package com.example;

import kotlinx.serialization.json.Json;

public class JavaTestFixtures {
    public Json createJsonSerializer() {
        return new JvmTestFixtures().createJsonSerializer();
    }
}
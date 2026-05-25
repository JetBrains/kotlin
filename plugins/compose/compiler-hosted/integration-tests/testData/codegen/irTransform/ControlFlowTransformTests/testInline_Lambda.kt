fun Test(condition: Boolean) {
    T {
        compose {
            M1 {
                if (condition) return@compose
            }
        }
    }
}

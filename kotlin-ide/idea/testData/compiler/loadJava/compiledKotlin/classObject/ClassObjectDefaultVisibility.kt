package test

public class Pub {
    companion object {}
}

private class Pri {
    companion object {}
}

class Int {
    companion object {}
}

public class Outer {
    public class Pub {
        companion object {}
    }

    private class Pri {
        companion object {}
    }

    class Int {
        companion object {}
    }

    protected class Pro {
        companion object {}
    }
}
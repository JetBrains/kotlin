class Lambda {
    void example() {
        doJob(arg -> arg + arg, "Mary");
    }

    void doJob(Job job, String arg) {
        System.out.println(job.doJob(arg));
    }
}

interface Job {
    String doJob(String arg);
}
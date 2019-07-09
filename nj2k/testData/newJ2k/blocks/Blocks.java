//method
void bar(int a) {}
void foo() {
    {
        int a = 1;
        bar(a);
    }

    {
        int a = 2;
        bar(a);
    }

    {
        bar(3);
    }
}
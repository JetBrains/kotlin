class JsFoo {
    static instances = new Set();
    constructor(value) {
        this.value = value;
        JsFoo.instances.add(this);
    }
}
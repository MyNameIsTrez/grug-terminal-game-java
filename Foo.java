class Foo {
    private native void foo();

    static {
        System.loadLibrary("bar");
        System.loadLibrary("foo");
    }

    public static void main(String[] args) {
        Foo foo = new Foo();
        foo.foo();
    }
}

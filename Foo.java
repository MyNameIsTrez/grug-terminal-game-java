class Foo {
    private native boolean foo();

    static {
        System.loadLibrary("foo");
    }

    public static void main(String[] args) {
        Foo foo = new Foo();
        System.out.println(foo.foo());
    }
}

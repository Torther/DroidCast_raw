package ink.mol.droidcast_raw;

public class Main {
    private static int width = 0;
    private static int height = 0;

    public static void main(String[] args) {
        new KtMain().main(args);
    }

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }

    public static void setWH(int width, int height) {
        Main.width = width;
        Main.height = height;
    }
}

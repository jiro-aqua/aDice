package jp.sblo.pandora.dice;

public class Natives {
    static {
        System.loadLibrary("adice");
    }

    public static native boolean countIndexWordsNative( int[] params , byte[] buff ,  int[]indexPtr  );
}

package jp.sblo.pandora.adice;

import java.io.File;

public class FileUtils {
    /**
     * remove all files and directories of specified path
     * @param path
     */
    public static void removeDirectory( File path )
    {
        File[] files = path.listFiles();
        if ( files != null ){
            for( File file : files ){
                if ( file.isDirectory() ){
                    removeDirectory( file );
                }
                file.delete();
            }
        }
    }

}

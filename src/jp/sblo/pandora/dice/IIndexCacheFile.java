package jp.sblo.pandora.dice;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public interface IIndexCacheFile
{
	FileInputStream	getInput() throws FileNotFoundException;
	FileOutputStream getOutput() throws FileNotFoundException;
}

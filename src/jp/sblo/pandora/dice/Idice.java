/**
 *
 */
package jp.sblo.pandora.dice;

import java.util.HashMap;

/**
 * @author Jiro
 *
 */
public interface Idice {
	IdicInfo open(String filename);

	int getDicNum();

	boolean isEnable(int num);

	void search(int num, String word);

//	boolean conjugationSearch(int num, String word);

	boolean isMatch(int num);

	IdicResult getResult(int num);

	IdicResult getMoreResult(int num);

	boolean hasMoreResult(int num);

	void close(IdicInfo info);

	IdicInfo getDicInfo(int num);

	IdicInfo getDicInfo(String filename);

	void 	setIrreg(HashMap<String, String> irreg);

	void	swap(IdicInfo info , int dir);
}

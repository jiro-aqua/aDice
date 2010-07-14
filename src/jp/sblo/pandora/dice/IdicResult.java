/**
 *
 */
package jp.sblo.pandora.dice;

/**
 * @author Jiro
 *
 */
public interface IdicResult {
	int		getCount();
	String	getIndex(int idx);
	String	getDisp(int idx);
	byte	getAttr(int idx);
	String	getTrans(int idx);
	String	getPhone(int idx);
	String	getSample(int idx);
	IdicInfo getDicInfo(int idx);
}

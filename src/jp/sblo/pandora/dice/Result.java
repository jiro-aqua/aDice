package jp.sblo.pandora.dice;

import java.util.ArrayList;


final class	Result	extends  ArrayList<Element> implements IdicResult {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public int		getCount()
	{
		return size();
	}
	@Override
	public final String	getIndex(int idx)
	{
		return get(idx).mIndex;
	}
	@Override
	public final String	getDisp(int idx)
	{
		return get(idx).mDisp;
	}
	@Override
	public byte	getAttr(int idx)
	{
		return get(idx).mAttr;
	}

	@Override
	public final String	getTrans(int idx)
	{
		return get(idx).mTrans;
	}

	@Override
	public final String	getPhone(int idx)
	{
		return get(idx).mPhone;
	}

	@Override
	public final String	getSample(int idx)
	{
		return get(idx).mSample;
	}

	@Override
	public IdicInfo getDicInfo(int idx)
	{
		return get(idx).mDic;
	}
}

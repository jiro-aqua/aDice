package jp.sblo.pandora.dice;

final public class Element {
	public	IdicInfo	mDic=null;
	public	byte	mAttr=0;
	public	String	mIndex=null;
	public	String	mDisp=null;
	public	String	mTrans=null;
	public  String  mSample=null;
	public  String  mPhone=null;

	public	Element(IdicInfo parent){
		mDic = parent;
	}

	public	Element(){
	}

}


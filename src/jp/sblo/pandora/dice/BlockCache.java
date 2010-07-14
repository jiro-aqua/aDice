package jp.sblo.pandora.dice;

//import java.lang.ref.SoftReference;
//import java.util.HashMap;
//import java.util.Set;
//import java.util.WeakHashMap;
//
//public class BlockCache  {
//	/** シリアライズID */
//	private static final long serialVersionUID = -1140373677231662504L;
//
//	private final static int GCTHRESHOLD = 10000;
//	static	WeakHashMap <Integer, SoftReference<byte[]>>	mHash = new WeakHashMap <Integer, SoftReference<byte[]>>();
//
//	/**
//	 * コンストラクタ
//	 *
//	 */
//	public BlockCache() {
//	}
//
//	public	byte[]	get(int key)
//	{
//		final SoftReference<byte[]> ref = mHash.get(key);
//		if ( ref != null ){
//			return ref.get();
//		}
//		return null;
//	}
//
//	public	void	put(int key , byte[] data )
//	{
////		// 新しいものを入れる時にガベージコレクトをする
////		if ( mHash.size() > GCTHRESHOLD ){
////			Set<Integer> hashset = mHash.keySet();
////
////			for( Integer hashkey : hashset ){
////				SoftReference<byte[]> ref  = mHash.get( hashkey );
////				if ( ref != null ){
////					if ( ref.get() == null ){
////						mHash.remove(hashkey);
////					}
////				}
////			}
////		}
//		mHash.put(key, new SoftReference<byte[]>(data));
//	}
//
//}

import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map;

public class BlockCache extends LinkedHashMap<Integer, SoftReference<byte[]>> {
	/** シリアライズID */
	private static final long serialVersionUID = -1140373677231662504L;

	private final static int CACHESIZE = 1000;

	/**
	 * 一番古いエントリを消すかどうか
	 *
	 * @param entry
	 *            最も古いエントリ
	 */
	protected boolean removeEldestEntry(Map.Entry<Integer, SoftReference<byte[]>> envtry) {
		if (size() > CACHESIZE) {
			return true;
		} else {
			return false;
		}
	}
	public	byte[]	getBuff(int key)
	{
		final SoftReference<byte[]> ref = get(key);
		if ( ref != null ){
			return ref.get();
		}
		return null;
	}
	public	void	putBuff(int key , byte[] data )
	{
		put(key, new SoftReference<byte[]>(data));
	}


}

package jp.sblo.pandora.dice;

import java.io.RandomAccessFile;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

//import android.util.Log;

public class IndexCache
{
	private boolean mFix;
	private int mBlockSize;
	private RandomAccessFile mFile;
	private int mStart;
	private int mSize;
	private WeakHashMap<Integer,WeakReference<byte[]>>	mMap = new WeakHashMap<Integer,WeakReference<byte[]>>();
	private byte[] mFixedBuffer;

	public IndexCache( RandomAccessFile file , int start , int size ){
		mFile = file;
		mStart = start;
		mSize = size;
		if ( mSize < 1024*512 ){
			mFix = true;
			mBlockSize = mSize;
		}else{
			mFix = false;
			mBlockSize = 1024;
		}
	}

	byte[]	getSegment(int segment)
	{
		byte[] segmentdata = null;

		if ( mFix ){
			if ( mFixedBuffer == null ){
				mFixedBuffer = new byte[mSize];
				try{
					mFile.seek(mStart);
					mFile.read(mFixedBuffer , 0, mSize );
				}
				catch( IOException e ){
				}
			}
			return mFixedBuffer;
		}

		WeakReference<byte[]> ref = mMap.get(segment);
		if ( ref != null ){
			segmentdata = ref.get();
		}
		if ( segmentdata == null ){
			segmentdata = new byte[mBlockSize];
			try{
				mFile.seek(mStart + segment*mBlockSize );
				int len = mFile.read(segmentdata, 0, mBlockSize );
				if ( len == mBlockSize || len == mSize%mBlockSize ){
					mMap.put(segment , new WeakReference<byte[]>(segmentdata) );
			//		Log.e( "aDice" , "miss hit! read index cache!" + segment );
				}else{
//					Log.e( "aDice" , "Can't read index!");
					return  null;
				}
			}
			catch( IOException e ){
//				Log.e( "aDice" , "Can't read index!");
				return null;
			}
		}
		return segmentdata;
	}



	public int getShort( int ptr )
	{
		int segment = ptr / mBlockSize;
		int address = ptr % mBlockSize;
		byte[] segmentdata = getSegment(segment++);

		int dat=0;
		if ( segmentdata != null ){
			int b=0;
			b = segmentdata[address++];
			b &= 0xFF;
			dat |= b;

			if ( address >= mBlockSize ){
				address %= mBlockSize;
				segmentdata = getSegment(segment++);
			}
			b = segmentdata[address++];
			b &= 0xFF;
			dat |= (b<<8);
		}
		return dat;
	}

	public int getInt( int ptr )
	{
		int segment = ptr / mBlockSize;
		int address = ptr % mBlockSize;
		byte[] segmentdata = getSegment(segment++);

		int dat=0;
		if ( segmentdata != null ){
			int b=0;
			b = segmentdata[address++];
			b &= 0xFF;
			dat |= b;
			if ( address >= mBlockSize ){
				address %= mBlockSize;
				segmentdata = getSegment(segment++);
			}
			b = segmentdata[address++];
			b &= 0xFF;
			dat |= (b<<8);
			if ( address >= mBlockSize ){
				address %= mBlockSize;
				segmentdata = getSegment(segment++);
			}
			b = segmentdata[address++];
			b &= 0xFF;
			dat |= (b<<16);
			if ( address >= mBlockSize ){
				address %= mBlockSize;
				segmentdata = getSegment(segment++);
			}
			b = segmentdata[address++];
			b &= 0x7F;
			dat |= (b<<24);
		}
		return dat;
	}
	private static int compareArrayAsUnsigned(byte[] aa, int pa, int la, byte[] ab, int pb, int lb)
	{
		while (la-- > 0) {
			short sa = aa[pa++];
			if (lb-- > 0) {
				short sb = ab[pb++];
				if (sa != sb) {
					sa &= 0xFF;
					sb &= 0xFF;
					return (sa - sb);
				}
			} else {
				return 1;
			}
		}
		if (lb > 0) {
			short sb = ab[pb++];
			if ( sb == 0x09 ){		// 比較対象の'\t'は'\0'とみなす
				return 0;
			}
			return -1;
		}
		return 0;
	}

	public int compare(byte[] aa, int pa, int la , int ptr, int len)
	{
		int segment = ptr / mBlockSize;
		int address = ptr % mBlockSize;
		byte[] segmentdata = getSegment(segment++);

		if ( segmentdata == null ) return -1;

		if ( address + len < mBlockSize ){
			return compareArrayAsUnsigned( aa , pa , la , segmentdata , address , len );
		}else{
			int lena = mBlockSize - address;
			int ret = compareArrayAsUnsigned( aa , pa , la , segmentdata , address , lena );
			if ( ret != 0 ){
				return ret;
			}
			address = 0;
			segmentdata = getSegment(segment++);
			return compareArrayAsUnsigned( aa , pa+lena , la-lena , segmentdata , address , len-lena );
		}
	}


	public boolean createIndex(int blockbits ,int nindex ,int[] indexPtr )
	{
		// インデックスの先頭から見出し語のポインタを拾っていく
		int i;
		int ptr = 0;
		byte[] segmentdata = getSegment(0);
		int lastsegment = 0;

		for (i = 0; i < nindex; i++) {
			ptr += blockbits; // ブロック番号サイズポインタを進める
			indexPtr[i] = ptr; // 見出し語部分のポインタを保存

			int segment = ptr / mBlockSize;
			int address = ptr % mBlockSize;
			if ( segment != lastsegment ){
				segmentdata = getSegment(segment);
				lastsegment = segment;
			}
			// 見出し語長さ＋\0分進める
			while (segmentdata[address++] != 0){
				ptr++;
				if ( address >= mBlockSize ){
					address = 0;
					segmentdata = getSegment(segment+1);
					lastsegment = segment+1;
				}
			}
			ptr++;
		}
		if (i == nindex) { // エラー無しで終われば
			indexPtr[i] = ptr + blockbits; // ターミネータを入れておく
			return true;
		}
		return false;
	}

}

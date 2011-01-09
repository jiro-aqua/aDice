package jp.sblo.pandora.dice;

import java.io.IOException;
import java.io.RandomAccessFile;
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

	private byte[] mSegmentData = null;
	byte[] getSegmentWithoutCache(int segment,int blocksize)
	{
        if ( mSegmentData == null ){
            mSegmentData = new byte[blocksize];
        }
        try{
            mFile.seek(mStart + segment*blocksize );
            mFile.read(mSegmentData, 0, blocksize );
        }
        catch( IOException e ){
            return null;
        }
        return mSegmentData;
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

		if ( len < 0 ){
		    return 1;
		}

		if ( address + len < mBlockSize ){
//	        String test = Index.decodetoCharBuffer(Charset.forName("BOCU-1"), segmentdata , address , len).toString();
//	        Log.e( "==========================>"+"("+ptr+")" , test );
			return compareArrayAsUnsigned( aa , pa , la , segmentdata , address , len );
		}else{
			int lena = mBlockSize - address;
			int leno = la;
			if ( la >= lena ){
			    leno = lena;
			}
			int ret = compareArrayAsUnsigned( aa , pa , leno , segmentdata , address , lena );
//	        String test = Index.decodetoCharBuffer(Charset.forName("BOCU-1"), segmentdata , address , lena).toString();
//	        Log.e( "=========================->" , test );
			if ( ret != 0 ){
				return ret;
			}
			if ( la < lena ){
			    return -1;
			}
			address = 0;
			segmentdata = getSegment(segment++);
//	         test = Index.decodetoCharBuffer(Charset.forName("BOCU-1"), segmentdata , address , len-lena).toString();
//	        Log.e( "=========================+>" , test );
			return compareArrayAsUnsigned( aa , pa+lena , la-lena , segmentdata , address , len-lena );
		}
	}


	public boolean createIndex(int blockbits ,int nindex ,int[] indexPtr )
	{
		// インデックスの先頭から見出し語のポインタを拾っていく
//		int i;
//		int ptr = 0;
//		byte[] segmentdata = getSegment(0);
//		int lastsegment = 0;

	    int blocksize = 64*1024;
		int [] params= new int[]{ 0 , 0 , nindex , blocksize , blockbits , 1 , 0 };

		int segment=0;

        while( Natives.countIndexWordsNative( params , getSegmentWithoutCache(segment++,blocksize) ,  indexPtr  ) );

        mSegmentData = null;
//        while( countIndexWords( params , getSegment(segment++) ,  indexPtr  ) );

		//if (i == nindex) { // エラー無しで終われば
			indexPtr[params[0]] = params[1] + blockbits; // ターミネータを入れておく
			return true;
		//}
//		return false;
	}

/*
	private boolean countIndexWords( int[] params , byte[] buff ,  int[]indexPtr  )
	{
	    int curidx = params[0];
	    int curptr = params[1];
	    int max = params[2];
	    int buffmax = params[3];
	    int blockbits= params[4];
	    int found = params[5];
	    int ignore = params[6];

	    int i=0;

        //
	    for( ;i<buffmax && curidx < max ;i++ ){
            if ( ignore > 0 ){
                ignore--;
            }else if ( found != 0){
    	        int ptr = curptr + i + blockbits;  // ブロック番号サイズポインタを進める
    	        indexPtr[curidx++] = ptr;          // 見出し語部分のポインタを保存
    	        ignore = blockbits-1;
    	        found = 0;
	        }else if ( buff[i]==0 ){
	            found = 1;
	        }
	    }

        params[0] = curidx;
        params[1] = curptr+i ;
        params[5] = found ;
        params[6] = ignore ;
	    if ( curidx < max ){
	        return true;
	    }else{
	        return false;
	    }
	}
*/

}

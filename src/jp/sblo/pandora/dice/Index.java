/**
 *
 */
package jp.sblo.pandora.dice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.WeakHashMap;

//import android.util.Log;

//import android.util.Log;

/**
 * @author Jiro
 *
 */
/**
 * @author jiro
 *
 */
class Index implements IdicInfo
{
    protected String m_filename;
    protected int m_bodyptr;
    protected Result mSearchResult;

    protected int m_start;
    protected int m_size;
    protected int m_blockbits;
    protected int m_nindex;
    protected int m_blocksize;
    protected boolean m_match;
//	protected int m_cachesize; // キャッシュサイズ
    protected int m_searchmax; // 最大検索件数
//	protected boolean m_isirreg; // 不定変化辞書フラグ
    protected boolean m_accent; // アクセント同一視検索スイッチ
    protected boolean m_english; // 英語語尾変化スイッチ
//	protected boolean m_thai; // タイ語使用スイッチ
    protected boolean m_notuse; // 使用しないフラグ

    protected String m_IndexFont; // 見出し語フォント
    protected int m_IndexSize; // 見出し語フォントサイズ
    protected String m_TransFont; // 訳文フォント
    protected int m_TransSize; // 訳文フォントサイズ
    protected boolean m_bPhonetic; // 発音記号使用有無
    protected String m_PhoneticFont; // 発音記号フォント
    protected int m_PhoneticSize; // 発音記号フォントサイズ
    protected boolean m_bSample; // 用例使用有無
    protected String m_SampleFont; // 用例フォント
    protected int m_SampleSize; // 用例フォントサイズ

    protected String m_dicname; // 辞書名
    //protected IIrreg m_irreg;

    protected int[] mIndexPtr;
    protected byte[] mIndexArray;

    protected Charset mMainCharset;
    protected Charset mPhoneCharset;
    protected boolean mUnicode;
    protected BlockCache mBlockCache;
    static	protected HashMap<String,String>	mIrreg=null;
    protected WeakHashMap<String,ByteBuffer>	mEncodeCache = new WeakHashMap<String,ByteBuffer>();

    protected AnalyzeBlock mAnalyze = null;
    protected int	mLastIndex=0;
    protected IndexCache mIndexCache;

    public static final String[][] t_variation1 = {
            // org var
            { "a", "ÀÁÂÃÄÅàáâãäåÆæ" }, { "c", "Çç" }, { "e", "ÈÉÊËèéêë" }, { "i", "ÌÍÎÏìíîï" }, { "d", "Ðð" },
            { "n", "Ññ" }, { "o", "ÒÓÔÕÖØòóôõöøŒœ" }, { "u", "ÙÚÛÜùúûü" }, { "y", "ÝÞýþÿ" }, { "s", "ß" }, };

    private RandomAccessFile mSrcStream = null;
//	final private String TAG = "aDice";

    Index(String filename, int start, int size, int nindex, boolean blockbits, int blocksize,boolean unicode)
    {
        m_filename = filename;
        m_start = start;
        m_size = size;
        m_nindex = nindex;
        m_blockbits = (blockbits) ? 4 : 2;
        m_blocksize = blocksize;
//		m_cachesize = 10;
        m_searchmax = 10;
//		m_isirreg = false;
        m_accent = false;
        m_english = false;
        m_notuse = false;
//		m_irreg = iirreg;
        m_IndexSize = 0; // 見出し語フォントサイズ
        m_TransSize = 0; // 訳文フォントサイズ
        m_bPhonetic = false; // 発音記号使用有無
        m_PhoneticSize = 0; // 発音記号フォントサイズ
        m_bSample = false; // 用例使用有無
        m_SampleSize = 0; // 用例フォントサイズ

        mSearchResult = new Result();
        mBlockCache = new BlockCache();

        mUnicode = unicode;
        if (unicode) {
            mPhoneCharset = mMainCharset = Charset.forName("BOCU-1");
        } else {
            mPhoneCharset = Charset.forName("ISO_8859-1");
            mMainCharset = Charset.forName("X-SJIS");
        }
        try {
            mSrcStream = new RandomAccessFile(new File(m_filename), "r");
        } catch (FileNotFoundException e) {
        }
        mAnalyze = new AnalyzeBlock(unicode);

        mIndexCache = new IndexCache( mSrcStream , m_start , m_size );
    }

    /**
     * byte配列の本文文字列をCharBufferに変換する
     */
    static public CharBuffer decodetoCharBuffer(Charset cs, byte[] array, int pos, int len)
    {
        return cs.decode(ByteBuffer.wrap(array, pos, len));
    }

    /**
     * 本文の文字列をByteBufferに変換する
     */
    static protected ByteBuffer encodetoByteBuffer(Charset cs, String str)
    {
        //Log.e( "encode" , str );
        return cs.encode(str);
    }


    /**
     * インデックス領域を検索
     *
     * @param word
     * @return
     */
    public int searchIndexBlock(String word)
    {
        int min = 0;
        int max = m_nindex-1;

        ByteBuffer __word = mEncodeCache.get(word);
        if ( __word==null  ){
            __word = encodetoByteBuffer(mMainCharset, word);
            mEncodeCache.put(word,__word);
        }
        int limit = __word.limit();
        byte[] _word = new byte[limit];
        System.arraycopy(__word.array(), 0, _word, 0, limit);
        int _wordlen = _word.length;

        int[] indexPtr = mIndexPtr;
        int blockbits = m_blockbits;
        IndexCache indexCache = mIndexCache;

        for (int i = 0; i < 32; i++) {
            if ((max - min) <= 1) {
                return min;
            }
            final int look = (min + max) / 2;
            final int len = indexPtr[look + 1] - indexPtr[look] - blockbits;
            final int comp = indexCache.compare( _word, 0, _wordlen, indexPtr[look], len);
//			Log.e( "==========================>look",""+look);
            if ( comp < 0 ) {
                max = look;
            } else if ( comp > 0 ){
                min = look;
            }else{	// match
                return look;
            }
        }
        return min;
    }

    /**
     *
     * @return
     */
    @Override
    public boolean readIndexBlock( IIndexCacheFile indexcache )
    {
        boolean ret = false;

        if (mSrcStream != null) {
            //mIndexArray = new byte[m_size];
            m_bodyptr = m_start + m_size; // 本体位置=( index開始位置＋インデックスのサイズ)

//			// インデックス全体を一気読み
//			try {
//				mSrcStream.seek(m_start);
//				int len = mSrcStream.read(mIndexArray, 0, m_size);

//				if (len == m_size) {

//					// インデクスをキャッシュファイルから読込
//					if(false){
////						final ByteBuffer bb = ByteBuffer.allocate( (m_nindex+1) *  4 );
////						bb.order( ByteOrder.LITTLE_ENDIAN );
////						final IntBuffer ib = bb.asIntBuffer();
////
////						try{
////			    			final FileChannel srcChannel = indexcache.getInput().getChannel() ;
////			    			srcChannel.read(bb);
////			    			srcChannel.close();
////			    			mIndexPtr = new int[m_nindex+1];
////			    			ib.get(mIndexPtr);
////			    			ret = true;
////						}
////						catch( IOException e ){
////						}
//
                    if ( indexcache != null ){
                        try {
                            FileInputStream fis = indexcache.getInput();
                            final byte[]	buff = new byte[ (m_nindex+1) *  4 ];
                            final int readlen = fis.read( buff ) ;
                            fis.close();

                            if ( readlen == buff.length ){
                                final int indexlen = m_nindex;
                                final int[] indexptr = mIndexPtr = new int[m_nindex+1];
    //							Log.e(TAG, "Loading from index from cache");
                                int ptr =0;
                                for( int i=0;i <= indexlen ;i++ ){
                                    int b;
                                    int dat=0;
                                    b = buff[ptr++];
                                    b &= 0xFF;
                                    dat = b;
                                    b = buff[ptr++];
                                    b &= 0xFF;
                                    dat |= (b<<8);
                                    b = buff[ptr++];
                                    b &= 0xFF;
                                    dat |= (b<<16);
                                    b = buff[ptr++];
                                    b &= 0xFF;
                                    dat |= (b<<24);
                                    indexptr[i] = dat;
                                }
                                ret = true;
//								Log.e(TAG, "Loaded from index from cache");
                            }
                        }
                        catch( IOException e){
                        }
//						// TODO:読む時に不一致でエラーが出た時どうなるか対策が必要
                    }

                    // キャッシュになかったら、この場で読む
                    if ( ret == false ){
//						Log.e(TAG, "Createing index");

                        // インデックスの先頭から見出し語のポインタを拾っていく
                        final int nindex = m_nindex;
                        final int [] indexPtr = mIndexPtr = new int[nindex + 1]; // インデックスポインタの配列確保
                        ret = mIndexCache.createIndex(m_blockbits, nindex, indexPtr);
                        if ( ret ){
//							Log.e(TAG, "Create index complete");

                            byte[] buff = new byte[indexPtr.length*4];
                            int p=0;
                            for( int c=0;c<=nindex;c++ ){
                                int	data = indexPtr[c];
                                buff[p++] = (byte)(data&0xFF);	data >>= 8;
                                buff[p++] = (byte)(data&0xFF);	data >>= 8;
                                buff[p++] = (byte)(data&0xFF);	data >>= 8;
                                buff[p++] = (byte)(data&0xFF);
                            }

                            // インデクスをファイルキャッシュに書込
                            if ( indexcache!=null ){
                                try {
                                    FileOutputStream fos = indexcache.getOutput();
                                    fos.write(buff , 0, buff.length );
                                    fos.close();
    //								Log.e(TAG, "Wrote index to cache file");
                                } catch (IOException e) {
    //								Log.e(TAG, "Couldn't write index to cache file");
                                }
                            }
                        }
                        // エラーがあれば、OutOfBoundsでcatchされるはず
                    }
                }
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
        if (!ret) {
            mIndexPtr = null;
        }

        return ret;
    }


    /**
     * num個目の見出し語の実体が入っているブロック番号を返す
     */
    public int getBlockNo(int num)
    {
        int blkptr = mIndexPtr[num] - m_blockbits;

        mLastIndex = num;

        int ptr = blkptr;

        if (m_blockbits == 4) {
            return mIndexCache.getInt(ptr);
        }else{
            return mIndexCache.getShort(ptr);
        }
    }

    /**
     * 次の０までの長さを返す
     *
     * @param array
     * @param pos
     * @return
     */
    static protected int getLengthToNextZero(byte[] array, int pos)
    {
        int len = 0;
        while (array[pos + len] != 0)
            len++;
        return len;
    }

    String AccentIgnoreSearch(String _word)
    {
        return AccentIgnoreSearch(_word, true, true);
    }

    String AccentIgnoreSearch(String _word, boolean irreg)
    {
        return AccentIgnoreSearch(_word, irreg, true);
    }

    boolean IsMatch()
    {
        return m_match;
    }

    // 以下 IdicInfoの実装
    @Override
    public String GetFilename()
    {
        return m_filename;
    }

//	@Override
//	public int GetCacheSize()
//	{
//		return m_cachesize;
//	}
//
//	@Override
//	public void SetCacheSize(int c)
//	{
//		m_cachesize = c;
////		mBlockCache.setCacheSize(c);
//	}

    @Override
    public int GetSearchMax()
    {
        return m_searchmax;
    }

    @Override
    public void SetSearchMax(int m)
    {
        m_searchmax = m;
    }

//	@Override
//	public void SetIrreg(boolean b)
//	{
//		m_isirreg = b;
//	}
//
//	@Override
//	public boolean GetIrreg()
//	{
//		return m_isirreg;
//	}

    @Override
    public void SetAccent(boolean b)
    {
        m_accent = b;
    }

    @Override
    public boolean GetAccent()
    {
        return m_accent;
    }

    @Override
    public void SetEnglish(boolean b)
    {
        m_english = b;
    }

    @Override
    public boolean GetEnglish()
    {
        return m_english;
    }

//	@Override
//	public void SetThai(boolean b)
//	{
//		m_thai = b;
//	}
//
//	@Override
//	public boolean GetThai()
//	{
//		return m_thai;
//	}

    @Override
    public void SetNotuse(boolean b)
    {
        m_notuse = b;
    }

    @Override
    public boolean GetNotuse()
    {
        return m_notuse;
    }

    @Override
    public void SetIndexFont(String b)
    {
        m_IndexFont = b;
    }

    @Override
    public String GetIndexFont()
    {
        return m_IndexFont;
    }

    @Override
    public void SetIndexSize(int b)
    {
        m_IndexSize = b;
    }

    @Override
    public int GetIndexSize()
    {
        return m_IndexSize;
    }

    @Override
    public void SetTransFont(String b)
    {
        m_TransFont = b;
    }

    @Override
    public String GetTransFont()
    {
        return m_TransFont;
    }

    @Override
    public void SetTransSize(int b)
    {
        m_TransSize = b;
    }

    @Override
    public int GetTransSize()
    {
        return m_TransSize;
    }

    @Override
    public void SetPhonetic(boolean b)
    {
        m_bPhonetic = b;
    }

    @Override
    public boolean GetPhonetic()
    {
        return m_bPhonetic;
    }

    @Override
    public void SetPhoneticFont(String b)
    {
        m_PhoneticFont = b;
    }

    @Override
    public String GetPhoneticFont()
    {
        return m_PhoneticFont;
    }

    @Override
    public void SetPhoneticSize(int b)
    {
        m_PhoneticSize = b;
    }

    @Override
    public int GetPhoneticSize()
    {
        return m_PhoneticSize;
    }

    @Override
    public void SetSample(boolean b)
    {
        m_bSample = b;
    }

    @Override
    public boolean GetSample()
    {
        return m_bSample;
    }

    @Override
    public void SetSampleFont(String b)
    {
        m_SampleFont = b;
    }

    @Override
    public String GetSampleFont()
    {
        return m_SampleFont;
    }

    @Override
    public void SetSampleSize(int b)
    {
        m_SampleSize = b;
    }

    @Override
    public int GetSampleSize()
    {
        return m_SampleSize;
    }

    @Override
    public void SetDicName(String b)
    {
        m_dicname = b;
    }
    @Override
    public String GetDicName()
    {
        return m_dicname;
    }


    boolean Search(final String _word)
    {
        boolean match = SearchWord(_word);

        // 一致でなく、かつ英語辞書の場合、活用検索を行う
        if ( !match && m_english ){

            Result lastResult = mSearchResult;	// 検索結果を一時待避

            // 活用形を変化させて検索
            String tmp = "";

            String[] tokenize = _word.split(" |\t");

            for (String token : tokenize) {

                // IRREGから検索
                String irreg = getIrreg(token);
                if (irreg!=null && irreg.length() > 0) {
                    // 結果ありならその結果に置換
                    token = irreg;
                } else {
                    int pos;
                    // 英語の語尾活用
                    // 単語末尾の s/ed/ing を削除する
                    pos = IsEnding(token, "s");
                    if (pos != -1 && token.length() > 1) {
                        token = token.substring(0, pos);
                    }
                    pos = IsEnding(token, "ed");
                    if (pos != -1 && token.length() > 2) {
                        token = token.substring(0, pos);
                    }
                    pos = IsEnding(token, "ing");
                    if (pos != -1 && token.length() > 3) {
                        token = token.substring(0, pos);
                    }
                }
                tmp += token;
                tmp += " ";
            }

            // 末尾の空白を除去
            tmp = tmp.trim();

            // 変換して元の文章と違っていれば検索
            if ( !_word.equals(tmp) ) {
                mSearchResult = new Result();
                boolean nmatch = SearchWord(tmp); // 再検索
                if (mSearchResult.getCount() > 0){
                    return nmatch;
                }else{
                    mSearchResult = lastResult;
                }
            }
        }
        return match;
    }

    // 文字列の末尾が指定文字列と一致するかチェック
    static int IsEnding(final String str, final String token)
    {
        if (str.endsWith(token)) {
            return (str.length() - token.length());
        } else {
            return -1;
        }
    }

    static String GetVariation1(char ch, boolean accent)
    {
        StringBuffer s_buf = new StringBuffer();

        s_buf.append(ch); // 小文字
        s_buf.append(Character.toUpperCase(ch)); // 大文字

        // アクセントの候補があれば追加
        if (accent) {

            for (String[] v : t_variation1) {
                if (v[0].charAt(0) == ch) {
                    s_buf.append(v[1]);
                    break;
                }
            }
        }
        return s_buf.toString();
    }

    // アクセント記号を無視した検索(辞書から見つかった候補を返す）
    String AccentIgnoreSearch(final String _word, boolean irreg, boolean accent)
    {

        ArrayList<String> l1 = new ArrayList<String>();
        ArrayList<String> l2 = new ArrayList<String>();
        ArrayList<String> p1, p2, tmp;

        p1 = l1;
        p2 = l2;

        p1.add("");

        int wordlen = _word.length();
        for (int w = 0; w < wordlen; w++) {
            // for( const TCHAR *pstr = _word;*pstr!=_T('\0') ;pstr++){
            char pstr = _word.charAt(w);

            tmp = p1;
            p1 = p2;
            p2 = tmp;

            p1.removeAll(p1);

            for (String s0 : p2) {

                String var = GetVariation1(pstr, accent);
                int varlen = var.length();
                for (int v = 0; v < varlen; v++) {
                    char p = var.charAt(v);
                    String s1 = s0 + p;

                    if (SearchForward(s1)) {
                        p1.add(s1);
//					} else if (irreg && m_irreg.Forward(s1)) {
//						p1.add(s1);
                    }
                }
            }
            if (p1.size() == 0) {
                break;
            }
        }
        // 最後までまわっていれば、マッチするものがあったということ
        String ret;
        if (p1.size() > 0) {
            ret = p1.get(0);
        } else {
            // 無ければ入力をそのまま返す
            ret = _word;
        }
        return ret;

    }

//	// 活用検索
//	// 通常検索の後に呼び出す
//	// 返値：true 活用検索で検索内容が変更された
//	// false 通常検索と同じ内容
//	boolean ConjugationSearch(String _word)
//	{
//		// 英語辞書の時のみ活用検索する
//		if (m_conjugation) {
//			// まず通常通り検索
//			boolean match = SearchWord(_word);
//			if (!match) { // 完全一致がなければ
//				boolean searched = false;
//				String str;
//				// 小文字にして検索
//				str = _word.toLowerCase(); // 小文字に変換
//
//				// 変換して元の文章と違っていれば検索
//				if (str.compareTo(_word) != 0) {
//					SearchWord(str); // 再検索
//					searched = true;
//					if (mSearchResult.getCount() > 0)
//						return true;
//				}
//
//				// 活用形を変化させて検索
//				String tmp = "";
//
//				String[] tokenize = str.split(" |\t");
//
//				// 英語の場合
//				if (m_english) {
//					for (String token : tokenize) {
//
////					if (m_accent) {
////						// アクセント記号付きで検索をかける
////						token = AccentIgnoreSearch(token);
////					}
//						// IRREGから検索
//						String irreg = getIrreg(token);
//						if (irreg!=null && irreg.length() > 0) {
//							// 結果ありならその結果に置換
//							token = irreg;
//						} else {
//							int pos;
//							// 英語の語尾活用
//							// 単語末尾の s/ed/ing を削除する
//							pos = IsEnding(token, "s");
//							if (pos != -1) {
//								token = token.substring(0, pos);
//							}
//							pos = IsEnding(token, "ed");
//							if (pos != -1) {
//								token = token.substring(0, pos);
//							}
//							pos = IsEnding(token, "ing");
//							if (pos != -1) {
//								token = token.substring(0, pos);
//							}
//						}
//						tmp += token;
//						tmp += " ";
//					}
//				}
//				// 末尾の空白を除去
//				tmp = tmp.trim();
//
//				// 変換して元の文章と違っていれば検索
//				if (str != tmp) {
//					SearchWord(tmp); // 再検索
//					searched = true;
//					if (mSearchResult.getCount() > 0)
//						return true;
//				}
//
//				// 大文字小文字・アクセントを変化させつつ順列探索
//				String accentstr = AccentIgnoreSearch(str, false, m_accent);
//				if (accentstr.compareTo(str) != 0) {
//					SearchWord(accentstr); // 検索
//					return true;
//				}
//
//				// ここまで来て見つからなければ通常検索の結果を返す
//				if (searched) {
//					match = SearchWord(_word);
//				}
//			}
//		}
//		return false;
//	}

    // 単語を検索する
    boolean SearchWord(String _word)
    {
        // 検索結果クリア
        int cnt = 0;
        mSearchResult.removeAll(mSearchResult);

        int ret = searchIndexBlock(_word);

        boolean match = false;


        boolean searchret =false;
        for(;;){
            // 最終ブロックは超えない
            if (ret < m_nindex ) {
                // 該当ブロック読み出し
                int block = getBlockNo(ret++);
                byte[] pblk = readBlockData(block);
                if (pblk != null) {
                    mAnalyze.setBuffer(pblk);
                    mAnalyze.setSearch(_word);
                    searchret = mAnalyze.searchWord();
                    // 未発見でEOBの時のみもう一回、回る
                    if ( !searchret && mAnalyze.mEob ){
                        continue;
                    }
                }
            }
            // 基本一回で抜ける
            break;
        }
        if ( searchret ){
            // 前方一致するものだけ結果に入れる
            do{
                Element res = mAnalyze.getRecord();
                if (res == null) {
                    break;
                }
                // 完全一致するかチェック
                if (res.mIndex.compareTo(_word) == 0) {
                    match = true;
                }
                res.mDic = this;
                mSearchResult.add(res);

                //blkhit = true;
                cnt++;
                // 取得最大件数超えたら打ち切り
            }while (cnt < m_searchmax && hasMoreResult(true));
        }
        return match;
    }


    // 前方一致する単語の有無を返す
    boolean SearchForward(final String _word)
    {
        int ret = searchIndexBlock(_word);

        for (int blk = 0; blk < 2; blk++) {
            // 最終ブロックは超えない
            if (ret + blk >= m_nindex) {
                break;
            }
            int block = getBlockNo(ret + blk);

            // 該当ブロック読み出し
            byte[] pblk = readBlockData(block);

            if (pblk != null) {
                String searchWord = _word;
                mAnalyze.setBuffer(pblk);
                mAnalyze.setSearch(searchWord);

                if (mAnalyze.searchWord()) {
                    return true;
                }
            }
        }
        return false;
    }

    Result GetResult()
    {
        return mSearchResult;
    }

    public Result getMoreResult()
    {
        mSearchResult.removeAll(mSearchResult);
        if (mAnalyze != null) {

            int cnt = 0;
            // 前方一致するものだけ結果に入れる
            while ( cnt < m_searchmax && hasMoreResult(true)  ){
                Element res = mAnalyze.getRecord();
                if (res == null) {
                    break;
                }
                res.mDic = this;
                mSearchResult.add(res);

                cnt++;
            };
        }
        return mSearchResult;
    }


    public boolean hasMoreResult(boolean incrementptr)
    {
        boolean result = mAnalyze.hasMoreResult(incrementptr);
        if ( !result ){
            if ( mAnalyze.isEob() ){	// EOBなら次のブロック読み出し
                int nextindex = mLastIndex + 1;
                // 最終ブロックは超えない
                if (nextindex  < m_nindex) {
                    int block = getBlockNo(nextindex);

//					Log.e( "Index","blocknum="+mLastIndex );
                    // 該当ブロック読み出し
                    byte[] pblk = readBlockData(block);

                    if (pblk != null) {
                        mAnalyze.setBuffer(pblk);
                        result = mAnalyze.hasMoreResult(incrementptr);
                    }
                }
            }
        }
        return result;
    }

    /**
     * データブロックを読み込み.生データのままキャッシュに保持しておく
     *
     * @param blkno
     * @return 読み込まれたデータブロック
     */
    byte[] readBlockData(int blkno)
    {
        // キャッシュの中にあるかチェック
        byte[] buff = mBlockCache.getBuff(blkno);

        if (buff == null) {
            //Log.e("readBlockData","cache miss" );
            buff = new byte[0x200];
            byte[] pbuf = buff;

            try {
                mSrcStream.seek(m_bodyptr + blkno * m_blocksize);

                // 1ブロック分読込(１セクタ分先読み)
                mSrcStream.read(pbuf, 0, 0x200);

                // 長さ取得
                int len = ((int)(pbuf[0]))&0xFF;
                len |= (((int)(pbuf[1]))&0xFF)<<8;

//				ByteBuffer bb = ByteBuffer.wrap(pbuf, 0, 2);
//				bb.order(ByteOrder.LITTLE_ENDIAN);
                //int len = bb.getShort();
                // ブロック長判定
                if ((len & 0x8000) != 0) { // 32bit
                    len &= 0x7FFF;
                }
                // 空きブロックチェック
                if (len > 0) {
                    // ブロック不足分読込
                    if (len * m_blocksize > 0x200) {
                        pbuf = new byte[m_blocksize * len];
                        System.arraycopy(buff, 0, pbuf, 0, 0x200);
                        mSrcStream.read(pbuf, 0x200, len * m_blocksize - 0x200);
                    }
                    // キャッシュに登録
                    mBlockCache.putBuff(blkno, pbuf);
                } else {
                    pbuf = null;
                }
                return pbuf;
            } catch (IOException e) {
                return null;
            }
        }
        return buff;
    }

    final class AnalyzeBlock
    {
        private byte[] mBuff;
        private boolean mLongfield;
        private boolean mUnicode;
        private byte[] mWord;
        private int mFoundPtr = -1;
        private int mNextPtr = -1;
        private byte[] mCompbuff = new byte[1024];
        private int mCompLen = 0;
        private ByteBuffer mBB;
        private boolean mEob=false;

        public AnalyzeBlock(boolean unicode)
        {
            mUnicode = unicode;
        }

        public	void	setBuffer(byte[] buff)
        {
            mBuff = buff;
            mLongfield = ((buff[1] & 0x80) != 0);
            mBB = ByteBuffer.wrap(buff);
            mBB.order(ByteOrder.LITTLE_ENDIAN);
            mNextPtr = 2;
            mEob=false;
            mCompLen = 0;
        }

        public	void 	setSearch( String word )
        {
            ByteBuffer __word = mEncodeCache.get(word);
            __word = encodetoByteBuffer(mMainCharset, word);
            mEncodeCache.put(word,__word);
            mWord  = new byte[__word.limit()];
            System.arraycopy(__word.array(), 0, mWord  , 0, __word.limit());
        }

        public boolean isEob()
        {
            return mEob;
        }

        /**
         * ブロックデータの中から指定語を探す
         */
        public boolean searchWord()
        {
            final byte[] _word = mWord;
            final byte[] buff = mBuff;
            final boolean longfield = mLongfield;
            final boolean unicode = mUnicode;
            final byte[] compbuff = mCompbuff;
            final int wordlen = _word.length;

            mFoundPtr = -1;

            // 訳語データ読込
            int ptr = mNextPtr;
            mNextPtr = -1;
            while (true) {
                int flen = 0;
                int retptr = ptr;
                int b;

                b = buff[ptr++];
                flen |= (b&0xFF);

                b = buff[ptr++];
                b <<= 8;
                flen |= (b&0xFF00);

                if (longfield) {
                    b = buff[ptr++];
                    b <<= 16;
                    flen |= (b&0xFF0000);

                    b = buff[ptr++];
                    b <<= 24;
                    flen |= (b&0x7F000000);
                }
                if (flen == 0) {
                    mEob = true;
                    break;
                }
                int qtr = ptr;
                ptr += flen + 1;
                if (unicode) {
                    ptr++;
                }

                // 圧縮長
                int complen = (int) buff[qtr++];
                complen &= 0xFF ;

                if (unicode) {
                    // 見出し語属性 skip
                    qtr++;
                }
                // 見出し語圧縮位置保存
                while ((compbuff[complen++] = buff[qtr++]) != 0)
                    ;

//		String test = decodetoCharBuffer(mMainCharset, compbuff, 0, complen).toString();
//		Log.e( "==========================>" , test );
                // 見出し語の方が短ければ不一致
                if (complen < wordlen) {
                    continue;
                }


                // 前方一致で比較
                boolean equal = true;
                for(int i=0;i<wordlen ;i++ ){

                    if ( compbuff[i] != _word[i] ){
                        equal = false;
                        int cc = compbuff[i];
                        cc &= 0xFF;
                        int cw = _word[i];
                        cw &= 0xFF;
                        // 超えてたら打ち切る
                        if ( cc > cw ){
                            return false;
                        }
                        break;
                    }
                }
                if ( equal ){
                    mFoundPtr = retptr;
                    mNextPtr = ptr;
                    mCompLen = complen - 1;
                    return true;
                }else{

                }
            }
            return false;
        }

        /**
         * 最後の検索結果の単語を返す
         *
         * @return
         */
        Element getRecord( )
        {
            if (mFoundPtr == -1) {
                return null;
            }
            final Element res = new Element();

            final byte[] compbuff = mCompbuff;
            res.mIndex = decodetoCharBuffer(mMainCharset, compbuff, 0, mCompLen).toString();
            // ver6対応 見出し語が、<検索インデックス><TAB><表示用文字列>の順に
            // 設定されていてるので、分割する。
            // それ以前のverではdispに空文字列を保持させる。

            final String	indexstr = res.mIndex;
            final int tab = indexstr.indexOf('\t');
            if ( tab == -1 ){
                res.mDisp = "";
            }else{
                res.mIndex = indexstr.substring(0,tab);
                res.mDisp =indexstr.substring(tab+1);
            }

            final byte[] buff = mBuff;
            final boolean longfield = mLongfield;
            final boolean unicode = mUnicode;
            byte attr = 0;

            // 訳語データ読込
            int ptr = mFoundPtr;

            if (longfield) {
                ptr += 4;
            } else {
                ptr += 2;
            }
            int qtr = ptr;

            // 圧縮長
            int complen = (int) buff[qtr++];
            complen &= 0xFF;

            if (unicode) {
                // 見出し語属性 skip
                attr = buff[qtr++];
            }
            // 見出し語 skip
            while (buff[qtr++] != 0)
                ;

            if (!unicode) {
                attr = buff[qtr++];
            }

            // 訳語
            if ((attr & 0x10) != 0) { // 拡張属性ありの時
                int trnslen = getLengthToNextZero(buff, qtr);
                res.mTrans = decodetoCharBuffer(mMainCharset, buff, qtr, trnslen).toString().replace("\r", "");
                qtr += trnslen; // 次のNULLまでスキップ

                // 拡張属性取得
                byte eatr;
                while (((eatr = buff[qtr++]) & 0x80) == 0) {
                    if ((eatr & (0x10 | 0x40)) == 0) { // バイナリOFF＆圧縮OFFの場合
                        if ((eatr & 0x0F) == 0x01) { // 用例
                            int len = getLengthToNextZero(buff, qtr);
                            res.mSample = decodetoCharBuffer(mMainCharset, buff, qtr, len).toString().replace("\r", "");
                            qtr += len; // 次のNULLまでスキップ
                        } else if ((eatr & 0x0F) == 0x02) { // 発音
                            int len = getLengthToNextZero(buff, qtr);
                            res.mPhone = decodetoCharBuffer(mPhoneCharset, buff, qtr, len).toString();
                            qtr += len; // 次のNULLまでスキップ
                        }
                    } else {
                        // バイナリ属性か圧縮属性が来たら打ち切り
                        break;
                    }
                }
            } else {
                // 残り全部が訳文
                res.mTrans = decodetoCharBuffer(mMainCharset, buff, qtr, mNextPtr - qtr).toString().replace("\r", "");
            }
            return res;
        }

        // 次の項目が検索語に前方一致するかチェックする
        public boolean hasMoreResult(boolean incrementptr)
        {
            byte[] _word;
            final byte[] buff = mBuff;
            final boolean longfield = mLongfield;
            final boolean unicode = mUnicode;
            final byte[] compbuff = mCompbuff;

            // next search
            if (mFoundPtr == -1) {
                return false;
            }
            _word = mWord;

            int wordlen = _word.length;

            // 訳語データ読込
            int ptr = mNextPtr;

            int retptr = ptr;
            int flen;
            int b;

            b = buff[ptr++];
            flen = (b&0xFF);

            b = buff[ptr++];
            b <<= 8;
            flen |= (b&0xFF00);

            if (longfield) {
                b = buff[ptr++];
                b <<= 16;
                flen |= (b&0xFF0000);

                b = buff[ptr++];
                b <<= 24;
                flen |= (b&0x7F000000);
            }
            // TRACE( _T("flen = %04X\n"),flen );
            if (flen == 0) {
                mEob = true;
                return false;
            }
            int qtr = ptr;
            ptr += flen + 1;
            if (unicode) {
                ptr++;
            }

            // 圧縮長
            int complen = (int) buff[qtr++];
            complen &= 0xFF;

            if (unicode) {
                // 見出し語属性 skip
                qtr++;
            }
            // 見出し語圧縮位置保存
            while ((compbuff[complen++] = buff[qtr++]) != 0)
                ;

            // 見出し語の方が短ければ不一致
            if (complen < wordlen) {
                return false;
            }

            // 前方一致で比較
            boolean equal = true;
            for(int i=0;i<wordlen ;i++ ){
                if ( compbuff[i] != _word[i] ){
                    equal = false;
                    int cc = compbuff[i];
                    cc &= 0xFF;
                    int cw = _word[i];
                    cw &= 0xFF;
                    // 超えてたら打ち切る
                    if ( cc > cw ){
                        return false;
                    }
                    break;
                }
            }
            if ( equal && incrementptr ){
                mFoundPtr = retptr;
                mNextPtr = ptr;
                mCompLen = complen - 1;
            }
            return equal;
        }

    }

    @Override
    public void setIrreg(HashMap<String, String> irreg)
    {
        if ( mIrreg == null ){
            mIrreg = irreg;
        }
    }

    @Override
    public String getIrreg(String key)
    {
        if ( mIrreg != null ){
            return mIrreg.get(key);
        }
        return null;
    }
}

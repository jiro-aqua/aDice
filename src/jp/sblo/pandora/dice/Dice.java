/**
 *
 */
package jp.sblo.pandora.dice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.String;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Jiro
 *
 */
final class Dice implements Idice
{
	private ArrayList<Index> mIndex;
	private HashMap<String, String> mIrreg;

	public Dice()
	{
		// constructor
		mIndex = new ArrayList<Index>();
	}

	@Override
	public IdicInfo open(String filename)
	{
		IdicInfo ret = null;
		final int headerSize = 256;
		Header header; // ヘッダー

		// 辞書の重複をチェック
		for (int i = 0; i < getDicNum(); i++) {
			// 登録済みの辞書であればエラー
			if ( getDicInfo(i).GetFilename().compareTo(filename) == 0) {
				return ret;
			}
		}

		File srcFile = new File(filename);
		try {
			FileInputStream srcStream = new FileInputStream(srcFile);
			FileChannel srcChannel = srcStream.getChannel();

			ByteBuffer headerbuff = ByteBuffer.allocate(headerSize);
			try {
				int len = srcChannel.read(headerbuff);
				srcChannel.close(); // ヘッダ読んだら、とりあえず閉じておく

				if (len == headerSize) {
					header = new Header();

					if (header.load(headerbuff) != 0) {
						// Unicode辞書 かつ ver6以上のみ許容
						if ( (	header.version &0xFF00) < 0x0600 ||		// ver6未満
								header.os != 0x20 ) {// Unicode以外
							throw new FileNotFoundException(); // bad dictionary
						}
						boolean unicode = true;
						final Index dic = new Index(filename, header.header_size + header.extheader,
								header.block_size * header.index_block, header.nindex2, header.index_blkbit,
								header.block_size, unicode);
						if (dic != null) {
							mIndex.add(dic);
							dic.setIrreg(mIrreg);
							ret = dic;
						}
					}
				}
			} catch (IOException e) {
			}

		} catch (FileNotFoundException e) {
		}

		return ret;
	}

	@Override
	public boolean isEnable(int num)
	{
		Index idx = mIndex.get(num);
		return (!idx.GetNotuse() /*&& !idx.GetIrreg()*/); // IRREGは通常検索から除外する
	}

	@Override
	public int getDicNum()
	{
		return mIndex.size();
	}


	@Override
	public void close(IdicInfo info)
	{
		mIndex.remove( info );
	}

	@Override
	public IdicInfo getDicInfo(int num)
	{
		return mIndex.get(num);
	}

	@Override
	public IdicInfo getDicInfo(String filename)
	{
		for (int i = 0; i < mIndex.size(); i++) {
			final IdicInfo di = mIndex.get(i);
			if (di.GetFilename().equals(filename)) {
				return di;
			}
		}
		return null;
	}



	@Override
	public void search(int num, String word)
	{
		Index idx = mIndex.get(num);
		if (!idx.GetNotuse() /*&& !idx.GetIrreg()*/) { // IRREGは通常検索から除外する
			idx.Search(word);
		}
	}

//	@Override
//	public boolean conjugationSearch(int num, String word)
//	{
//		boolean ret = false;
//		Index idx = mIndex.get(num);
//		if (!idx.GetNotuse() /*&& !idx.GetIrreg()*/) { // IRREGは通常検索から除外する
//			if (idx.ConjugationSearch(word)) {
//				ret = true;
//			}
//		}
//		return ret;
//	}

	@Override
	public boolean isMatch(int num)
	{
		boolean ret = false;
		Index idx = mIndex.get(num);
		if (!idx.GetNotuse() && idx.IsMatch()) {
			ret = true;
		}
		return ret;
	}

	@Override
	public IdicResult getResult(int num)
	{
		Index idx = mIndex.get(num);
		return idx.GetResult();
	}

	@Override
	public IdicResult getMoreResult(int num)
	{
		Index idx = mIndex.get(num);

		return idx.getMoreResult();
	}

	@Override
	public boolean hasMoreResult(int num)
	{
		Index idx = mIndex.get(num);

		return idx.hasMoreResult(false);
	}

//	// IPdicIrregの実装
//	@Override
//	public String GetIrreg(String word)
//	{
//		String ret = "";
//
//		for (Index idx : mIndex) {
//			if (!idx.GetNotuse() /*&& idx.GetIrreg()*/) { // IRREGから指定単語を検索する
//				ret = idx.SearchWordOnlyMatch(word); // IRREGから完全一致するものの訳語を取得
//				if (ret.length() != 0) {
//					break;
//				}
//			}
//		}
//		return ret;
//	}
//
//	@Override
//	public boolean Forward(String word)
//	{
//		boolean ret = false;
//
//		for (Index idx : mIndex) {
//			if (!idx.GetNotuse() /*&& idx.GetIrreg()*/) { // IRREGから指定単語を検索する
//				ret = idx.SearchForward(word); // IRREGから前方一致するものを確認
//				if (ret) {
//					break;
//				}
//			}
//		}
//		return ret;
//	}

	@Override
	public void	swap(IdicInfo info , int dir)
	{
		int current = mIndex.indexOf(info);
		if ( dir == 0 ){
			return;
		}else if ( dir < 0 && current > 0 ){
			mIndex.remove(info);
			mIndex.add(current-1, (Index)info);
		}else if ( dir > 0  && current<mIndex.size()-1 ){
			mIndex.remove(info);
			mIndex.add(current+1, (Index)info);
		}
	}


	@Override
	public	void setIrreg(HashMap<String, String> irreg)
	{
		mIrreg = irreg;
	}

}

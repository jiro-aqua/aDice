package jp.sblo.pandora.dice;

import java.util.HashMap;

public interface IdicInfo {
    String	GetFilename();
//	int 	GetCacheSize();
//	void 	SetCacheSize(int c);
    int 	GetSearchMax();
    void 	SetSearchMax( int m );
//	void 	SetIrreg( boolean b );
//	boolean GetIrreg( );
    void 	SetAccent( boolean b );
    boolean GetAccent( );
    void	SetEnglish( boolean b );
    boolean	GetEnglish(  );
//	void	SetThai( boolean b );
//	boolean	GetThai(  );
    void	SetNotuse( boolean b );
    boolean	GetNotuse(  );
    void	SetIndexFont( String b );
    String	GetIndexFont(  );
    void	SetIndexSize( int b );
    int		GetIndexSize(  );
    void	SetTransFont( String b );
    String	GetTransFont(  );
    void	SetTransSize( int b );
    int		GetTransSize(  );
    void	SetPhonetic( boolean b );
    boolean	GetPhonetic(  );
    void	SetPhoneticFont( String b );
    String	GetPhoneticFont(  );
    void	SetPhoneticSize( int b );
    int		GetPhoneticSize(  );
    void	SetSample( boolean b );
    boolean	GetSample(  );
    void	SetSampleFont( String b );
    String	GetSampleFont(  );
    void	SetSampleSize( int b );
    int		GetSampleSize(  );
    void	SetDicName( String b );
    String	GetDicName(  );
    void	setIrreg( HashMap<String,String> irreg );
    String	getIrreg( String key );
    public boolean readIndexBlock( IIndexCacheFile indexcache );


}

package com.vakasu11.mjspiderman;

/** Retains only m/j/other categories in a bounded cursor window, not input text. */
final class SurroundingTrigger {
    private byte[] kinds=new byte[0];
    private int offset,cursor,documentEnd=-1;
    private long context;
    private boolean known,absoluteKnown;
    void reset(){known=false;kinds=new byte[0];}
    private static byte kind(char c){char n=TextTrigger.lower(c);return (byte)(n=='m'?1:n=='j'?2:0);}
    boolean observe(CharSequence text,int selectionStart,int selectionEnd,int textOffset,long field){
        if(text==null||selectionStart!=selectionEnd||selectionEnd<0||selectionEnd>text.length() ){reset();return false;}
        boolean hasAbsolute=textOffset>=0;
        if(known&&absoluteKnown!=hasAbsolute)reset();
        absoluteKnown=hasAbsolute;if(textOffset<0)textOffset=0;
        long absoluteLong=(long)textOffset+selectionEnd;
        if(absoluteLong>Integer.MAX_VALUE){reset();return false;}
        int absolute=(int)absoluteLong;
        int end=text.length()-selectionEnd<64?textOffset+text.length():-1;
        int oldIndex=absolute-2-offset;
        boolean oldPair=oldIndex>=0&&oldIndex+1<kinds.length&&kinds[oldIndex]==1&&kinds[oldIndex+1]==2;
        boolean inOldWindow=oldIndex>=0&&oldIndex+1<kinds.length;
        boolean pair=selectionEnd>=2&&kind(text.charAt(selectionEnd-2))==1&&kind(text.charAt(selectionEnd-1))==2;
        boolean grew=documentEnd>=0&&end>=0&&end>documentEnd;
        boolean shrank=documentEnd>=0&&end>=0&&end<documentEnd;
        boolean result=known&&context==field&&pair&&!shrank
            &&((grew&&absolute>cursor)||(!oldPair&&inOldWindow));
        int begin=Math.max(0,selectionEnd-64),finish=Math.min(text.length(),selectionEnd+64);
        byte[] next=new byte[finish-begin];
        for(int i=0;i<next.length;i++)next[i]=kind(text.charAt(begin+i));
        kinds=next;offset=textOffset+begin;cursor=absolute;documentEnd=end;context=field;known=true;
        return result;
    }
}

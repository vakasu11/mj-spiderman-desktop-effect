package com.vakasu11.mjspiderman;

/** Keeps only a text fingerprint and cursor metadata, never input contents. */
public final class TextTrigger {
    private boolean known;
    private long field, hash;
    private int length, cursor;
    private boolean endedWithMj;
    public void reset(){known=false;}
    static char lower(char c){
        if(c>='\uff21'&&c<='\uff3a')return (char)('a'+c-'\uff21');
        if(c>='\uff41'&&c<='\uff5a')return (char)('a'+c-'\uff41');
        return Character.toLowerCase(c);
    }
    private static long fingerprint(CharSequence s){
        long h=0xcbf29ce484222325L;
        for(int i=0;i<s.length();i++){h^=lower(s.charAt(i));h*=0x100000001b3L;}
        return h;
    }
    private static boolean pair(CharSequence s,int i){return i>=0&&i+1<s.length()&&lower(s.charAt(i))=='m'&&lower(s.charAt(i+1))=='j';}
    private void remember(CharSequence s,int pos,long context,long nextHash){
        known=true;field=context;hash=nextHash;length=s.length();cursor=pos;endedWithMj=pair(s,pos-2);
    }
    public void prime(CharSequence text,int pos,long context){
        if(text!=null&&(!known||field!=context))remember(text,pos,context,fingerprint(text));
    }
    public boolean changed(CharSequence before,CharSequence text,int from,int added,int removed,
                           boolean password,boolean editable,long context){
        if(password||!editable||text==null)return false;
        long nextHash=fingerprint(text);
        long end=(long)from+Math.max(0,added);
        int pos=end>=0&&end<=text.length()?(int)end:text.length();
        boolean duplicate=known&&field==context&&hash==nextHash&&length==text.length();
        remember(text,pos,context,nextHash);
        if(duplicate||added<0||removed<0||from<0)return false;
        int start,limit;
        if(before!=null){
            int prefix=0,min=Math.min(before.length(),text.length());
            while(prefix<min&&lower(before.charAt(prefix))==lower(text.charAt(prefix)))prefix++;
            int suffix=0;
            while(suffix<min-prefix&&lower(before.charAt(before.length()-1-suffix))==lower(text.charAt(text.length()-1-suffix)))suffix++;
            int changedEnd=text.length()-suffix;
            if(changedEnd<=prefix)return false;
            start=Math.max(0,prefix-1);limit=Math.min(text.length(),changedEnd+1);
        }else{
            if(added<=0||end>text.length()||end<0)return false;
            start=Math.max(0,from-1);limit=(int)Math.min((long)text.length(),end+1);
        }
        for(int i=start;i+1<limit;i++)if(pair(text,i))return true;
        return false;
    }
    /** Fallback for IMEs which send selection/content updates without edit events. */
    public boolean observe(CharSequence text,int pos,boolean password,boolean editable,long context){
        if(password||!editable||text==null)return false;
        long nextHash=fingerprint(text);
        if(!known||field!=context){remember(text,pos,context,nextHash);return false;}
        boolean changed=hash!=nextHash||length!=text.length();
        boolean result=changed&&text.length()>=length&&pair(text,pos-2)
            &&!(text.length()==length&&pos==cursor&&endedWithMj);
        remember(text,pos,context,nextHash);return result;
    }
}

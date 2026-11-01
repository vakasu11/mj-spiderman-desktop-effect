package com.vakasu11.mjspiderman;

public class TextTriggerTest {
    private static int checks;
    private static void check(boolean ok){checks++;if(!ok)throw new AssertionError("Check "+checks);}
    private static boolean edit(TextTrigger t,String before,String after,int from,int added,int removed){return t.changed(before,after,from,added,removed,false,true,1);}
    public static void main(String[] args){
        TextTrigger t=new TextTrigger();
        check(!edit(t,"","m",0,1,0));check(edit(t,"m","mj",1,1,0));
        check(!edit(t,"m","mj",1,1,0));check(!edit(t,"mj","mj",0,2,2));
        check(!edit(t,"mj","mjm",0,3,2));check(edit(t,"mjm","mjmj",0,4,3));
        check(!edit(t,"mjmj","mjmj",0,4,4));
        t.reset();check(edit(t,"","mjmjm",0,5,0));
        t.reset();check(edit(t,"","MJ",0,2,0));check(!edit(t,"MJ","mj",0,2,2));
        t.reset();check(!t.changed("","mj",0,2,0,true,true,1));
        check(!t.changed("","mj",0,2,0,false,false,1));
        check(!t.changed("",null,0,2,0,false,true,1));
        check(!edit(t,"mXj","mj",1,0,1));check(!edit(t,"mj","m",1,0,1));
        check(edit(t,"m","mj",1,1,0));
        t.reset();check(edit(t,"j","mj",0,1,0));
        t.reset();check(edit(t,"mx","mj",1,1,1));
        t.reset();check(!edit(t,"你好","你好m",2,1,0));check(edit(t,"你好m","你好mj",3,1,0));
        t.reset();check(!t.changed(null,"mj",0,Integer.MAX_VALUE,0,false,true,1));
        t.reset();check(!edit(t,"","mj",-1,2,0));
        t.reset();t.prime("",0,7);check(!t.observe("m",1,false,true,7));
        check(t.observe("mj",2,false,true,7));check(!t.observe("mj",2,false,true,7));
        check(!t.observe("mjm",3,false,true,7));check(t.observe("mjmj",4,false,true,7));
        check(!t.observe("mj",2,false,true,7));check(!t.observe("mj",2,false,true,8));
        t.reset();t.prime("mj",2,7);check(!t.observe("mj",2,false,true,7));
        check(!t.observe("m",1,false,true,7));check(t.observe("mj",2,false,true,7));
        t.reset();check(edit(t,"","mj",0,2,0));check(!t.observe("mj",2,false,true,1));
        check(t.changed("","mj",0,2,0,false,true,2));
        t.reset();check(t.changed("m","mj",1,0,0,false,true,1));
        check(!t.changed("m","mj",1,0,0,false,true,1));
        t.reset();check(!t.changed("abmj","amj",1,0,1,false,true,1));
        check(t.changed("amj","amjmj",3,2,0,false,true,1));
        // Realistic composing updates replace the whole word, not one appended letter.
        t.reset();String before="";for(int n=1;n<=80;n++){
            String after=before+(n%2==1?"m":"j");
            check(edit(t,before,after,0,after.length(),before.length())==(n%2==0));
            check(!edit(t,before,after,0,after.length(),before.length()));before=after;
        }
        // Coalesced edits may contain mj before the final cursor (old matcher missed these).
        String stream="mjmjxmjmmjmjmjxmmj";t.reset();before="";
        for(int start=0;start<stream.length();){
            int end=Math.min(start+3,stream.length());String after=stream.substring(0,end);
            boolean want=false;for(int p=Math.max(0,start-1);p+1<end;p++)if(stream.substring(p,p+2).equals("mj"))want=true;
            check(edit(t,before,after,start,end-start,0)==want);before=after;start=end;
        }
        System.out.println("TextTrigger: "+checks+" checks passed");
    }
}

package com.vakasu11.mjspiderman;
public final class SurroundingTriggerTest {
    static int checks;
    static void check(boolean value){checks++;if(!value)throw new AssertionError("Surrounding check "+checks);}
    static boolean full(SurroundingTrigger t,String s,int cursor){return t.observe(s,cursor,cursor,0,1);}
    public static void main(String[] args){
        SurroundingTrigger t=new SurroundingTrigger();
        check(!full(t,"",0));check(!full(t,"m",1));check(full(t,"mj",2));check(!full(t,"mj",2));
        check(full(t,"mjmj",4));check(!full(t,"mjmj",4));
        check(!full(t,"",0));check(full(t,"mj",2));
        t.reset();check(!full(t,"already mj text",0));check(!full(t,"already mj text",10));
        t.reset();check(!full(t,"mx",2));check(full(t,"mj",2));
        check(!full(t,"MJ",2));check(!full(t,"ｍｊ",2));
        t.reset();check(!full(t,"中文ｍ",3));check(full(t,"中文ｍｊ",4));
        t.reset();check(!full(t,"j",0));check(full(t,"mjj",2));
        t.reset();check(!full(t,"mXj",3));check(!full(t,"mj",2));
        check(!t.observe("mj",0,2,0,1));check(!t.observe(null,0,0,0,1));
        check(!t.observe("m",1,1,-1,1));check(t.observe("mj",2,2,-1,1));check(!t.observe("mj",2,2,0,2));
        // A fixed-size cursor window can slide while typing; absolute offsets matter.
        t.reset();String base="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        check(!t.observe(base,62,62,0,7));
        check(t.observe(base+"mj",64,64,0,7));
        String window=(base+"mjmj").substring(2);
        check(t.observe(window,64,64,2,7));check(!t.observe(window,64,64,2,7));
        System.out.println("SurroundingTrigger: "+checks+" checks passed");
    }
}

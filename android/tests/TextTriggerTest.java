package com.vakasu11.mjspiderman;

public class TextTriggerTest {
    private static int checks = 0;
    private static void check(boolean result) { checks++; if (!result) throw new AssertionError("Check "+checks); }
    public static void main(String[] args) {
        TextTrigger t = new TextTrigger();
        check(!t.matches("m",0,1,false,true,1,1));
        check(t.matches("mj",1,1,false,true,1,2));
        check(!t.matches("mj",1,1,false,true,1,2));
        check(t.matches("mjmj",3,1,false,true,1,3));
        check(t.matches("MJ",0,2,false,true,1,4));
        check(!t.matches("mj",0,2,true,true,1,5));
        check(!t.matches("mj",0,2,false,false,1,5));
        check(!t.matches("mj",0,0,false,true,1,5));
        check(!t.matches("mj",0,Integer.MAX_VALUE,false,true,1,5));
        check(!t.matches(null,0,2,false,true,1,5));
        check(!t.matches("mj",-1,2,false,true,1,5));
        check(t.matches("mj other",0,2,false,true,1,6));
        t.reset();
        check(t.matches("mj",0,2,false,true,1,2));
        check(!t.matches("你好",0,2,false,true,1,7));
        System.out.println("TextTrigger: "+checks+" checks passed");
    }
}

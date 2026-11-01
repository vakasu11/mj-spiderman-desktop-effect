package com.vakasu11.mjspiderman;
public final class ServiceSelectionTest {
    static int checks;
    static void check(boolean value){checks++;if(!value)throw new AssertionError("Selection check "+checks);}
    public static void main(String[] args){
        String p="com.vakasu11.mjspiderman",c=p+".MjService";
        check(!ServiceSelection.contains(null,p,c));check(!ServiceSelection.contains("",p,c));
        check(ServiceSelection.contains(p+"/"+c,p,c));check(ServiceSelection.contains(p+"/.MjService",p,c));
        check(ServiceSelection.contains("other/.Service:"+p+"/.MjService",p,c));
        check(ServiceSelection.contains(p+"/.MjService:other/.Service",p,c));
        check(ServiceSelection.contains(":"+p+"/.MjService:",p,c));
        check(!ServiceSelection.contains(p+"x/.MjService",p,c));
        check(!ServiceSelection.contains(p+"/.OtherService",p,c));
        check(!ServiceSelection.contains(p,p,c));check(!ServiceSelection.contains("/"+c,p,c));
        check(!ServiceSelection.contains(p+"/",p,c));
        check(ServiceSelection.contains("  "+p+"/.MjService  ",p,c));
        check(!ServiceSelection.contains(p+"/.mjservice",p,c));
        System.out.println("ServiceSelection: "+checks+" checks passed");
    }
}

package com.vakasu11.mjspiderman;

/** Parse only our selected component; never retain the other enabled services. */
final class ServiceSelection {
    static boolean contains(String list,String packageName,String className){
        if(list==null||packageName==null||className==null)return false;
        for(String item:list.split(":")){
            String component=item.trim();int slash=component.indexOf('/');
            if(slash<1||!component.substring(0,slash).equals(packageName))continue;
            String name=component.substring(slash+1);
            if(name.startsWith("."))name=packageName+name;
            if(name.equals(className))return true;
        }
        return false;
    }
}

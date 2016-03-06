package com.sree.textbytes.jtopia;

/**
 * Created by jgecy on 2016-03-06.
 */
public class StringHelper {

    public static boolean isNullOrEmpty(String string){
        return string == null || string.isEmpty();
    }


    public static StringSplitter SPACE_SPLITTER = new StringSplitter(" ");

}

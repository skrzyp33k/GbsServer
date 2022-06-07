package com.gbsdevelopers.gbssocket;

import java.io.*;
import java.util.*;

public final class GbsMessage implements Serializable
{
    public String header;
    public Vector<String> arguments;
    public GbsMessage()
    {
        header = "";
        arguments = new Vector<String>();
    }
    public GbsMessage(String header, Vector<String> arguments)
    {
        this.header = header;
        this.arguments = arguments;
    }

    private static String removeLastChar(String s) {
        return s.substring(0, s.length() - 1);
    }

    public static String implode(Vector<String> strings, String delim) {
        StringBuilder sb = new StringBuilder();

        for (String string : strings) {
            sb.append(string);
            sb.append(delim);
        }

        String returnString = sb.toString();

        return removeLastChar(returnString);
    }

    public static Vector<String> explode(String string, String delim) {
        return new Vector<String>(Arrays.asList(string.split(delim)));
    }
}
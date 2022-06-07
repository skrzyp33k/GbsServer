package com.gbsdevelopers.gbssocket;

import java.io.*;
import java.util.*;

/**
 * This class contains information that is sent between clients and server
 */
public final class GbsMessage implements Serializable
{
    /**
     * Contains header of the message.
     * Commonly used to determine operations or to send back short response.
     */
    public String header;

    /**
     * Contains arguments of the message.
     */
    public Vector<String> arguments;

    /**
     * Default constructor. Used when we want "clean" object
     */
    public GbsMessage()
    {
        header = "";
        arguments = new Vector<String>();
    }

    /**
     * Constructor that allows to write header and arguments during construction.
     * @param header Contains header of the message.
     * @param arguments Contains arguments of the message
     */
    public GbsMessage(String header, Vector<String> arguments)
    {
        this.header = header;
        this.arguments = arguments;
    }

    /**
     * Function to remove the last character from a string. Used with implode() to remove the last semicolon.
     * @param s Input string.
     * @return String without last character.
     */
    private static String removeLastChar(String s) {
        return s.substring(0, s.length() - 1);
    }

    /**
     * Function to merge some strings into one string separated by given delimiter.
     * @param strings Vector of strings to merge.
     * @param delim Delimiter.
     * @return Merged string divided by given delimiter.
     */
    public static String implode(Vector<String> strings, String delim) {
        StringBuilder sb = new StringBuilder();

        for (String string : strings) {
            sb.append(string);
            sb.append(delim);
        }

        String returnString = sb.toString();

        return removeLastChar(returnString);
    }

    /**
     * Function to divide string separated with delimiter into Vector of strings.
     * @param string String to divide.
     * @param delim Delimiter.
     * @return Vector of strings.
     */
    public static Vector<String> explode(String string, String delim) {
        return new Vector<String>(Arrays.asList(string.split(delim)));
    }
}
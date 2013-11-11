package eu.pryds.ve;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Vector;

import android.view.ContextMenu;

public class TranslatableStringCollection {
    Vector<TranslatableString> strings;
    int headerIndex; //index in strings vector that contains po header info
    
    public TranslatableStringCollection() {
        strings = new Vector<TranslatableString>();
        headerIndex = -1;
    }
    
    public TranslatableString getString(int id) {
        return strings.get(id);
    }
    
    public int size() {
        return strings.size();
    }
    
    public void parse(File poFile) {
        Vector<String> poFileLines = new Vector<String>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(poFile));
            
            String line;
            while ((line = reader.readLine()) != null) {
                poFileLines.add(line);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace(); // TODO: Proper exception handling
        }
        
        TranslatableString str = new TranslatableString();
        
        final int MSGID = 0;
        final int MSGID_PLURAL = 1;
        final int MSGSTR = 2;
        final int MSGCTXT = 3;
        int lastWrittenMultiliner = MSGID;
        int lastWrittenMsgstrIndex = 0;
        
        for (int i = 0; i < poFileLines.size(); i++) {
            if (poFileLines.get(i) == null || poFileLines.get(i).trim().equals("")) {
                strings.add(str);
                str = new TranslatableString();
                
            } else if (poFileLines.get(i).startsWith("# ")) {
                if (str.getTranslatorComments().length() > 0)
                    str.addtoTranslatorComments("" + '\n');
                str.addtoTranslatorComments(poFileLines.get(i).substring(2).trim());
                
            } else if (poFileLines.get(i).startsWith("#.")) {
                if (str.getExtractedComments().length() > 0)
                    str.addtoExtractedComments("" + '\n');
                str.addtoExtractedComments(poFileLines.get(i).substring(2).trim());
                
            } else if (poFileLines.get(i).startsWith("#:")) {
                String[] parts = poFileLines.get(i).substring(2).trim().split(" ");
                for (int j = 0; j < parts.length; j++) {
                    str.getReferences().add(parts[j]);
                }
                
            } else if (poFileLines.get(i).startsWith("#,")) {
                String[] parts = poFileLines.get(i).substring(2).trim().split(", ");
                for (int j = 0; j < parts.length; j++) {
                    str.getFlags().add(parts[j]);
                }
                
            } else if (poFileLines.get(i).startsWith("#| msgctxt")) {
                if (str.getPreviousContext().length() > 0)
                    str.addtoPreviousContext("" + '\n');
                str.addtoPreviousContext(trimQuotes(poFileLines.get(i)
                        .substring(10)));
                
            } else if (poFileLines.get(i).startsWith("#| msgid_plural")) {
                if (str.getPreviousUntranslatedStringPlural().length() > 0)
                    str.addtoPreviousUntranslatedStringPlural("" + '\n');
                str.addtoPreviousUntranslatedStringPlural(trimQuotes(poFileLines.get(i)
                        .substring(15)));
                
            } else if (poFileLines.get(i).startsWith("#| msgid")) {
                if (str.getPreviousUntranslatedString().length() > 0)
                    str.addtoPreviousUntranslatedString("" + '\n');
                str.addtoPreviousUntranslatedString(trimQuotes(poFileLines.get(i)
                        .substring(8)));
                
            } else if (poFileLines.get(i).startsWith("msgctxt")) {
                str.setContext(trimQuotes(poFileLines.get(i).substring(7)));
                lastWrittenMultiliner = MSGCTXT;
                
            } else if (poFileLines.get(i).startsWith("msgid_plural")) {
                str.setUntranslatedStringPlural(trimQuotes(poFileLines.get(i)
                        .substring(12)));
                lastWrittenMultiliner = MSGID_PLURAL;
                
            } else if (poFileLines.get(i).startsWith("msgid")) {
                str.setUntranslatedString(trimQuotes(poFileLines.get(i)
                        .substring(5)));
                lastWrittenMultiliner = MSGID;
                
            } else if (poFileLines.get(i).startsWith("msgstr[")) {
                int indexOfSquareEndBracket = poFileLines.get(i).indexOf(']');
                String strNoStr = poFileLines.get(i).substring(7,
                        indexOfSquareEndBracket);
                int strNo = Integer.parseInt(strNoStr);
                if (strNo == 0) {
                    str.resetTranslatedString();
                }
                str.getTranslatedString().put(
                        strNo,
                        trimQuotes(poFileLines.get(i)
                                .substring(indexOfSquareEndBracket + 1)));
                lastWrittenMultiliner = MSGSTR;
                lastWrittenMsgstrIndex = strNo;
                
            } else if (poFileLines.get(i).startsWith("msgstr")) {
                str.resetTranslatedString();
                str.getTranslatedString().put(0,
                        trimQuotes(poFileLines.get(i).substring(6)));
                lastWrittenMultiliner = MSGSTR;
                lastWrittenMsgstrIndex = 0;
                
            } else if (poFileLines.get(i).startsWith("\"")) {
                if (lastWrittenMultiliner == MSGID) {
                    if (!str.getUntranslatedString().equals(""))
                        str.addtoUntranslatedString("" + '\n');
                    str.addtoUntranslatedString(trimQuotes(poFileLines.get(i)));
                    
                } else if (lastWrittenMultiliner == MSGID_PLURAL) {
                    if (!str.getUntranslatedStringPlural().equals(""))
                        str.addtoUntranslatedStringPlural("" + '\n');
                    str.addtoUntranslatedStringPlural(trimQuotes(poFileLines.get(i)));
                    
                } else if (lastWrittenMultiliner == MSGSTR) {
                    String existingData = str.getTranslatedString().get(
                            lastWrittenMsgstrIndex);
                    if (existingData.equals(""))
                        str.getTranslatedString().put(lastWrittenMsgstrIndex,
                                trimQuotes(poFileLines.get(i)));
                    else
                        str.getTranslatedString().put(
                                lastWrittenMsgstrIndex,
                                existingData + '\n'
                                        + trimQuotes(poFileLines.get(i)));
                } else if (lastWrittenMultiliner == MSGCTXT) {
                    if (!str.getContext().equals(""))
                        str.addtoContext("" + '\n');
                    str.addtoContext(trimQuotes(poFileLines.get(i)));
                }
                
            } else {
                System.out.println("Unexpected line " + (i + 1) + ": "
                        + poFileLines.get(i));
            }
        }
        strings.add(str);
        
        // Check for and remove completely empty TranslatableStrings
        for (int i = strings.size()-1; i >= 0; i--) {
            if (strings.get(i).isEmpty())
                strings.remove(i);
        }
        
        // Find index of first occurrence of empty untranslated string
        // and mark that as header info
        for (int i = 0; i < strings.size(); i++) {
            if (strings.get(i).containsHeaderInfo()) {
                headerIndex = i;
                break;
            }
        }
    }
    
    public String[] toPoFile() {
        if (headerIndex == -1) { // if there was no header entry, create one now
            strings.add(0, new TranslatableString());
            strings.get(0).initiateHeaderInfo();
        } else { // otherwise, update existing header entry
            strings.get(headerIndex); // TODO
        }
        
        
        Vector<String> outputLines = new Vector<String>();
        
        for (int i = 0; i < strings.size(); i++) {
            if (i != 0)
                outputLines.add("");
            
            if (!strings.get(i).getTranslatorComments().equals("")) {
                String[] transCommLines = strings.get(i)
                        .getTranslatorComments().split("\n");
                for (int j = 0; j < transCommLines.length; j++)
                    outputLines.add("#  " + transCommLines[j]);
            }
            
            if (!strings.get(i).getExtractedComments().equals("")) {
                String[] extrCommLines = strings.get(i).getExtractedComments()
                        .split("\n");
                for (int j = 0; j < extrCommLines.length; j++)
                    outputLines.add("#. " + extrCommLines[j]);
            }
            
            final int LENGTH_OF_LINES_MINUS_SHORT_PREFIX = 80 - "#  ".length(); // 77
            if (strings.get(i).getReferences().size() > 0) {
                StringBuffer refstr = new StringBuffer();
                for (int j = 0; j < strings.get(i).getReferences().size(); j++) {
                    if (j != 0)
                        refstr.append(" ");
                    refstr.append(strings.get(i).getReferences().get(j));
                }
                String[] refWrapped = wordWrapToArray(refstr.toString(),
                        LENGTH_OF_LINES_MINUS_SHORT_PREFIX);
                for (int j = 0; j < refWrapped.length; j++)
                    outputLines.add("#: " + refWrapped[j].trim());
            }
            
            if (strings.get(i).getFlags().size() > 0) {
                StringBuffer flagsstr = new StringBuffer();
                for (int j = 0; j < strings.get(i).getFlags().size(); j++) {
                    if (j != 0)
                        flagsstr.append(", ");
                    flagsstr.append(strings.get(i).getFlags().get(j));
                }
                String[] flagsWrapped = wordWrapToArray(flagsstr.toString(),
                        LENGTH_OF_LINES_MINUS_SHORT_PREFIX);
                for (int j = 0; j < flagsWrapped.length; j++)
                    outputLines.add("#, " + flagsWrapped[j]);
            }
            
            if (!strings.get(i).getPreviousContext().equals("")) {
                String[] prevContextLines = strings.get(i).getPreviousContext()
                        .split("\n");
                for (int j = 0; j < prevContextLines.length; j++)
                    outputLines.add("#| msgctxt \"" + prevContextLines[j]
                            + "\"");
            }
            
            if (!strings.get(i).getPreviousUntranslatedString().equals("")) {
                String[] prevUntrStrWrapped = strings.get(i)
                        .getPreviousUntranslatedString().split("\n");
                for (int j = 0; j < prevUntrStrWrapped.length; j++)
                    outputLines.add("#| msgid \"" + prevUntrStrWrapped[j]
                            + "\"");
            }
            
            if (!strings.get(i).getPreviousUntranslatedStringPlural()
                    .equals("")) {
                String[] prevUntrStrPlurWrapped = strings.get(i)
                        .getPreviousUntranslatedStringPlural().split("\n");
                for (int j = 0; j < prevUntrStrPlurWrapped.length; j++)
                    outputLines.add("#| msgid_plural \""
                            + prevUntrStrPlurWrapped[j] + "\"");
            }
            
            if (!strings.get(i).getContext().equals("")) {
                writeMultilinesTo(outputLines, "msgctxt ", strings.get(i)
                        .getContext());
            }
            
            if (!strings.get(i).getUntranslatedString().equals("")) {
                writeMultilinesTo(outputLines, "msgid ", strings.get(i)
                        .getUntranslatedString());
            }
            
            if (!strings.get(i).getUntranslatedStringPlural().equals("")) {
                writeMultilinesTo(outputLines, "msgid_plural ", strings.get(i)
                        .getUntranslatedStringPlural());
            }
            
            if (strings.get(i).getTranslatedString().size() > 0) {
                if (strings.get(i).getTranslatedString().size() == 1) {
                    writeMultilinesTo(outputLines, "msgstr ", strings.get(i)
                            .getTranslatedString().get(0));
                } else {
                    for (int j = 0; j < strings.get(i).getTranslatedString()
                            .size(); j++)
                        writeMultilinesTo(outputLines, "msgstr[" + j + "] ",
                                strings.get(i).getTranslatedString().get(j));
                }
            }
        }
        return outputLines.toArray(new String[] {});
    }
    
    private static String trimQuotes(String str) {
        return str.trim().replaceAll("^\"|\"$", "");
    }
    
    public static String[] wordWrapToArray(String input, int width) {
        return wordWrap(input, width).split("\n");
    }
    
    /**
     * Word-wraps a long lined string to the given max-width.
     * <p>
     * If there are already newlines in the input string, the input is split at
     * these newlines, and each substring is fed to the private method
     * {@link #wordWrapOneLine(String, int)}, after which all (now wrapped)
     * substrings are re-joined, with a newline in-between each of them.
     * 
     * @param input
     *            a string to be word-wrapped
     * @param width
     *            maximum length of each line, in characters
     * @return new string containing word-wrapped version of input
     * 
     * @see #wordWrapOneLine(String, int)
     */
    public static String wordWrap(String input, int width) {
        String[] inputLines = input.split("\n");
        StringBuffer output = new StringBuffer();
        for (int i = 0; i < inputLines.length; i++) {
            if (i != 0)
                output.append("\n");
            output.append(wordWrapOneLine(inputLines[i], width));
        }
        return output.toString();
    }
    
    private static String wordWrapOneLine(String input, int width) {
        if (input.length() <= width) {
            return input;
        } else {
            int lastSpaceIndex = input.lastIndexOf(" ", width);
            if (lastSpaceIndex == -1)
                lastSpaceIndex = width;
            
            String output1 = input.substring(0, lastSpaceIndex).trim() + " "
                    + '\n';
            String output2 = input.substring(lastSpaceIndex).trim();
            input = null;
            return output1 + wordWrapOneLine(output2, width);
        }
    }
    
    private static void writeMultilinesTo(Vector<String> outputLines,
            String prefix, String writeString) {
        final int LINE_WIDTH = 80;
        if (writeString.length() > LINE_WIDTH - prefix.length()) {
            String[] wrappedLines = wordWrapToArray(writeString, LINE_WIDTH
                    - "\"\"".length());
            outputLines.add(prefix + "\"\"");
            for (int i = 0; i < wrappedLines.length; i++)
                outputLines.add("\"" + wrappedLines[i] + "\"");
        } else {
            outputLines.add(prefix + "\"" + writeString + "\"");
        }
    }
    
    //System.out.println("Version name (string): " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
}

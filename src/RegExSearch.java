import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * This program accepts a regular expression and path of a text file as command-line arguments in this order. It then
 * finds strings matching the regular expression line-by-line and prints results.
 *
 * @author Salman Khan
 */
public class RegExSearch {
    private static List<List<String>> matches;

    public static void main(String[] args) {
        try {
            String regEx = standardizeRangeFormat(args[0]);
            Scanner inFile = new Scanner(new File(args[1]));

            matches = new ArrayList<>();

            while (inFile.hasNext()) {
                String line = inFile.nextLine();
                List<String> matchesInLine = new ArrayList<>();

                // check every combination of consecutive characters from line
                for (int i = 0; i < line.length(); i++)
                    for (int j = i; j < line.length(); j++)
                        if (!matchesInLine.contains(line.substring(i, j + 1)) &&
                                isMatch(regEx, line.substring(i, j + 1))) matchesInLine.add(line.substring(i, j + 1));

                matches.add(matchesInLine);
            }

            printMatches();
        } catch (Exception e) {
            System.out.println("\nAn error occurred.");
        }
    }

    /**
     * Converts *, + and ? into {} format, infinity is represented by _
     * @param regEx a regular expression
     * @return updated regular expression
     */
    private static String standardizeRangeFormat(String regEx) {
        regEx = regEx.replace("*", "{0,_}");
        regEx = regEx.replace("+", "{1,_}");
        regEx = regEx.replace("?", "{0,1}");
        regEx = regEx.replace("{,", "{0,");
        regEx = regEx.replace(",}", ",_}");

        return regEx;
    }

    /**
     * Calls isMatchHelper() with received arguments and returns interpretation of returned value
     * @param regEx a regular expression in standardized format
     * @param str the string to match
     * @return true if str matches regEx; false otherwise
     */
    private static boolean isMatch(String regEx, String str) {
        int result = isMatchHelper(regEx, str);

        return result == str.length();
    }

    /**
     * Compares received str with received regEx
     * @param regEx a regular expression in standardized format
     * @param str the string to match
     * @return number of initial characters in str that match regEx; -1 if it does not match
     */
    private static int isMatchHelper(String regEx, String str) {
        List<List<String>> regExChunks = getChunks(regEx);

        int strIndex = 0;

        for (List<String> orChunks : regExChunks) {
            if (strIndex > str.length()) return -1;

            boolean matchingChunkFound = false;

            int maxStrIndex = strIndex;

            for (String thisChunk : orChunks) {
                int thisChunkStrIndex = strIndex;

                int minRepeat = 1;
                int maxRepeat = 1;

                // extract int values of a and b from {a,b} and store them in minRepeat and maxRepeat respectively
                // if this chunk ends with }
                if (thisChunk.charAt(thisChunk.length() - 1) == '}') {
                    minRepeat = Integer.parseInt(thisChunk.substring(thisChunk.lastIndexOf('{') + 1,
                            thisChunk.lastIndexOf(',')));

                    String maxRepeatStr = thisChunk.substring(thisChunk.lastIndexOf(',') + 1,
                            thisChunk.length() - 1);

                    maxRepeat = maxRepeatStr.equals("_") ? -1 : Integer.parseInt(maxRepeatStr);

                    thisChunk = thisChunk.substring(0, thisChunk.lastIndexOf('{'));
                }

                // find matching chunk type from (...), [...], ., normal char
                if (thisChunk.charAt(0) == '(') {
                    int repeatCount = 0;

                    // get number of times this chunk repeats consecutively in str starting at current index
                    while (true) {
                        if (maxRepeat != -1 && repeatCount == maxRepeat) break;

                        int matchedCharCount = isMatchHelper(thisChunk.substring(1, thisChunk.length() - 1),
                                str.substring(thisChunkStrIndex));

                        if (matchedCharCount != -1) {
                            thisChunkStrIndex += matchedCharCount;

                            repeatCount++;
                        } else {
                            break;
                        }
                    }

                    if (repeatCount >= minRepeat) matchingChunkFound = true;
                } else if (thisChunk.charAt(0) == '[') {
                    int repeatCount = 0;

                    // get number of times this chunk repeats consecutively
                    while (true) {
                        if (thisChunkStrIndex >= str.length() || (maxRepeat != -1 && repeatCount == maxRepeat)) break;

                        int oldRepeatCount = repeatCount;

                        // compare every char between [...] in this chunk with char at current index in str
                        for (int i = 1; i < thisChunk.length() - 1; i++) {
                            if (thisChunk.charAt(i) == str.charAt(thisChunkStrIndex)) {
                                thisChunkStrIndex++;
                                repeatCount++;

                                break;
                            }
                        }

                        if (repeatCount == oldRepeatCount) break;
                    }

                    if (repeatCount >= minRepeat) matchingChunkFound = true;
                } else if (thisChunk.charAt(0) == '.') {
                    int charsLeft = str.length() - thisChunkStrIndex;

                    // accept next upto maxRepeat number of chars if at least minRepeat number of chars are left
                    if (charsLeft >= minRepeat) {
                        if (maxRepeat == -1) {
                            thisChunkStrIndex += charsLeft;
                        } else {
                            thisChunkStrIndex += Math.min(charsLeft, maxRepeat);
                        }

                        matchingChunkFound = true;
                    }
                } else {
                    int repeatCount = 0;

                    // get number of times this char repeats consecutively in str starting at current index
                    while (true) {
                        if (thisChunkStrIndex >= str.length() || (maxRepeat != -1 && repeatCount == maxRepeat)) break;

                        if (thisChunk.charAt(0) == str.charAt(thisChunkStrIndex)) {
                            thisChunkStrIndex++;
                            repeatCount++;
                        } else {
                            break;
                        }
                    }

                    if (repeatCount >= minRepeat) matchingChunkFound = true;
                }

                if (thisChunkStrIndex > maxStrIndex) maxStrIndex = thisChunkStrIndex;
            }

            strIndex = maxStrIndex;

            if (!matchingChunkFound) return -1;
        }

        return strIndex;
    }

    /**
     * Converts/divides received regEx into chunks; also expands ranges in []
     * @param regEx a regular expression in standardized format
     * @return regEx in chunks form
     */
    private static List<List<String>> getChunks(String regEx) {
        List<List<String>> toReturn = new ArrayList<>();
        List<String> orChunks = new ArrayList<>();

        int chunkStIndex = 0;

        while (chunkStIndex < regEx.length()) {
            int chunkEndIndex = chunkStIndex + getChunkEndIndex(regEx.substring(chunkStIndex));

            String toAdd = regEx.substring(chunkStIndex, chunkEndIndex + 1);

            // expand ranges (e.g. '[a-c]')
            if (toAdd.charAt(0) == '[') {
                StringBuilder expandedRange = new StringBuilder();

                for (int i = 1; i < toAdd.length() - 2; i++) {
                    if (toAdd.charAt(i) == '-') {
                        for (int j = toAdd.charAt(i - 1) + 1; j < toAdd.charAt(i + 1); j++)
                            expandedRange.append((char) j);
                    }
                }

                toAdd = toAdd.replace("]", expandedRange.append("]"));
                toAdd = toAdd.replace("-", "");
            }

            chunkStIndex = chunkEndIndex + 1;

            // append {...} to the chunk toAdd if one exists immediately after it
            if (chunkStIndex < regEx.length() && regEx.charAt(chunkStIndex) == '{') {
                orChunks.add(toAdd + regEx.substring(chunkStIndex, regEx.indexOf('}', chunkStIndex) + 1));

                chunkStIndex += 5;
            } else {
                orChunks.add(toAdd);
            }

            // if a disjunction '|' exists between two chunks, add them to the same orChunks array
            // otherwise, create a new array for next index of toReturn array of arrays
            if (chunkStIndex < regEx.length() && regEx.charAt(chunkStIndex) == '|') {
                chunkStIndex++;
            } else {
                toReturn.add(orChunks);

                orChunks = new ArrayList<>();
            }
        }

        return toReturn;
    }

    /**
     * Finds end of first chunk in received regEx
     * @param regEx a regular expression in standardized format
     * @return index of chunk's ending character
     */
    private static int getChunkEndIndex(String regEx) {
        char stChar = regEx.charAt(0);
        char endChar;

        // determine ending char
        if (stChar == '(') {
            endChar = ')';
        } else if (stChar == '[') {
            endChar = ']';
        } else {
            return 0;
        }

        int nestedEndsLeft = 0;

        // find ending char's index
        for (int i = 1; i < regEx.length(); i++) {
            if (regEx.charAt(i) == stChar) nestedEndsLeft++;

            if (regEx.charAt(i) == endChar) {
                if (nestedEndsLeft != 0) {
                    nestedEndsLeft--;
                } else {
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * Prints matched strings line-by-line stored in global variable matches
     */
    private static void printMatches() {
        for (int i = 0; i < matches.size(); i++) {
            if (matches.get(i).size() == 0) continue;

            System.out.printf("Line %d: ", i + 1);

            for (String thisMatch : matches.get(i)) System.out.printf("\"%s\" ", thisMatch);

            System.out.print("\n");
        }
    }
}
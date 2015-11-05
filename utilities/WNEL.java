/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.cru.creativity.utilities;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 *
 * @author antonis
 */
public class WNEL {

    private String wnFile;

    public WNEL(String file) {
        this.wnFile = file;
    }

    public ArrayList<String> getWordsInAllSynsets(String word) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(wnFile), "UTF8"));
        String currentSynset = null;
        ArrayList<String> synsetWords = new ArrayList<String>();
        //System.out.println(word);
        while ((currentSynset = reader.readLine()) != null) {
            currentSynset = currentSynset.toLowerCase();

            if (currentSynset.startsWith("#")) {
                continue;
            }
            if (currentSynset.contains(word.toLowerCase())) {
                StringTokenizer tokenizer = new StringTokenizer(currentSynset, ";");
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken();
                    if (!token.equals(word.toLowerCase())) {
                        synsetWords.add(token.toString());
                    }
                }
            }
        }
        if (synsetWords.isEmpty()) {
            return new ArrayList<String>();
        }
        return synsetWords;
    }

    public int getSynsetCount(String word) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(wnFile), "UTF8"));
        String currentLine = null;
        int cnt = 0;

        while ((currentLine = reader.readLine()) != null) {
            currentLine = currentLine.toLowerCase();
            if (currentLine.startsWith("#")) {
                continue;
            }
            StringTokenizer tokenizer = new StringTokenizer(currentLine, ";");
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                if (token.equals(word.toLowerCase())) {
                    cnt++;
                    break;
                }
            }
        }
        return cnt;
    }

    public String[] getTermsOfLargestSynset(String word) throws FileNotFoundException, UnsupportedEncodingException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(wnFile), "UTF8"));
        String currentLine = null;
        String terms = "";
        int max = 0;
        while ((currentLine = reader.readLine()) != null) {
            currentLine = currentLine.toLowerCase();
            if (currentLine.startsWith("#")) {
                continue;
            }
            if (currentLine.contains(word)) {
                StringTokenizer tokenizer = new StringTokenizer(currentLine, ";");
                if (tokenizer.countTokens() > max) {
                    max = tokenizer.countTokens();
                    while (tokenizer.hasMoreTokens()) {
                        String token = tokenizer.nextToken();
                        if (!token.equals(word.toLowerCase())) {
                            terms += ":" + token;
                        }
                    }
                }

            }
        }
        return terms.split(":");
    }

    public double getDistance(String w1, String w2) {
        double d = 1.0;
        String currentLine = null;
        boolean found = false;

        int cnt = 0;
        int pos1 = 0, pos2 = 0;
        boolean found1 = false, found2 = false;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(wnFile), "UTF8"));
            while ((currentLine = reader.readLine()) != null) {
                cnt++;
                if (found) {
                    break;
                }
                currentLine = currentLine.toLowerCase();
                if (currentLine.startsWith("#")) {
                    continue;
                }
                if (currentLine.contains(w1) && currentLine.contains(w2)) {
                    d = 0.0;
                    found = true;
                    break;
                } else if (currentLine.contains(w1)) {
                    found1 = true;
                    pos1 = cnt;
                } else if (currentLine.contains(w2)) {
                    found2 = true;
                    pos2 = cnt;
                }
                if (found1 && found2) {
                    d = Math.abs(pos1 - pos2);
                    // System.out.println(d);
                    found = true;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!found) {
            return 1.0;
        }
        d = d / cnt;
        return d;
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.cru.creativity.utilities;

import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.WordNetDatabase;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import rita.wordnet.RiWordnet;

/**
 *
 * @author antonis
 */
public class WNAccess {

    private RiWordnet wn;
    private WordNetDatabase database;

    public WNAccess(String database_dir) {
        wn = new RiWordnet();
        database = WordNetDatabase.getFileInstance();

        System.setProperty("wordnet.database.dir", database_dir);
    }

    public WNAccess() {
        wn = new RiWordnet();
        database = WordNetDatabase.getFileInstance();

        System.setProperty("wordnet.database.dir", "C:\\WordNet\\2.1\\dict\\");
    }

    public String getCommonPos(String word) {
        String pos = wn.getBestPos(word);
        return pos;
    }

    public String[] getPos(String word) {
        String[] pos = wn.getPos(word);
        return pos;
    }

    public String[] getWordsInSynsets(String word) {
        String pos = wn.getBestPos(word);
        if (pos == null) {
            return null;
        }
        String[] synsets = wn.getAllSynsets(word, this.getCommonPos(word));
        return synsets;
    }

    public String[] getWordsInAllSynsets(String word) {
        /*String pos = wn.getBestPos(word);
         if (pos == null) {
         System.out.println("den vrhke tetoio");
         return null;
         }
         return wn.getAllSynsets(word, pos);*/

        Synset[] a = database.getSynsets(word);
        if (a.length == 0 || a == null) {
            return null;
        }
        String b = "";
        for (int j = 0; j < a.length; j++) {
            String[] l = a[j].getWordForms();
            for (int k = 0; k < l.length; k++) {
                b += ":" + l[k];
            }
        }
        return b.split(":");
    }

    public String[] getTermsOfLargestSynset(String word) {
        Synset[] a = database.getSynsets(word);
        int max = a[0].getWordForms().length;
        String[] terms = a[0].getWordForms();
        for (int i = 1; i < a.length; i++) {
            String[] b = a[i].getWordForms();
            if (b.length > max) {
                max = b.length;
                terms = b;
            }
        }
        return terms;
    }

    public int getCount(String word) {
        return database.getSynsets(word)[0].getTagCount(word);
    }

    public int getSynsetCount(String word) {
        String pos = this.getCommonPos(word);
        if (pos == null) {
            return 0;
        }
        String[] synsets = wn.getSynset(word, pos, true);
        if (synsets == null) {
            return 0;
        }
        return synsets.length;
    }

    public double getDistance(String w1, String w2) {
        double d = 0.0;
        boolean common = false;

        String p1[] = wn.getPos(w1);
        String p2[] = wn.getPos(w2);

        if (p1 == null || p2 == null) {
            d = 1.0;
            return d;
        }

        for (int i = 0; i < p1.length; i++) {
            for (int j = 0; j < p2.length; j++) {
                if (p1[i].equals(p2[j])) {
                    common = true;//System.out.println("both words are " + p1[i]);
                    if (wn.getDistance(w1, w2, p1[i]) > d) {
                        d = wn.getDistance(w1, w2, p1[i]);
                        // System.out.println("no"+d);
                    }
                }
            }
        }
        if (!common) {
            d = 1.0;
        }

        return d;
    }

    public String stem(String word) {
        String pos = getCommonPos(word);
        if (!pos.isEmpty()) {
            String[] g = wn.getStems(word, pos);
            if (g.length > 0) {
                return g[0];
            }
        }
        return "";
    }

    public HashSet<String> getHyponymsAndHypernyms(String word) {
        //find the stems of the word, and get the hyponyms&hypernyms of each stem
        HashSet<String> s = new HashSet<String>();
        String pos = getCommonPos(word);
        try {
            if (!pos.isEmpty()) {
                String[] g = wn.getStems(word, pos);
                for (String h : g) {
                    List<String> p = Arrays.asList(wn.getAllHyponyms(h, getCommonPos(word)));
                    if (!p.isEmpty()) {
                        s.addAll(p);
                    }
                    p = Arrays.asList(wn.getAllHypernyms(h, getCommonPos(word)));
                    if (!p.isEmpty()) {
                        s.addAll(p);
                    }

                }
            }
            return s;
        } catch (Exception e) {
            //System.out.println(e);
            return s;
        }
    }

    public HashSet<String> getMeronyms(String word) {
        HashSet<String> s = new HashSet<String>();
        String pos = getCommonPos(word);
        try {
            if (!pos.isEmpty()) {
                String[] g = wn.getStems(word, pos);
                for (String h : g) {
                    String[] l = wn.getAllMeronyms(h, getCommonPos(word));
                    if (l.length > 0) {
                        s.addAll(Arrays.asList(l));
                    }
                }
            }
            return s;
        } catch (Exception e) {
            //   System.out.println(e);
            return s;
        }
    }
}

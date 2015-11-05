/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.cru.creativity.reasoning.semantic;

import gr.demokritos.iit.cru.creativity.utilities.Connect;
import gr.demokritos.iit.cru.creativity.utilities.WNAccess;
import gr.demokritos.iit.cru.creativity.utilities.WNDE;
import gr.demokritos.iit.cru.creativity.utilities.WNEL;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;

/**
 *
 * @author Giorgos
 */
public class RandomWordGenerator {

    // private int top;
    private String language;
    private WNAccess wn;
    private WNDE wnde;
    private WNEL wnel;
    private InfoSummarization inf;
    private Set<String> stop;
    private Set<String> off;
    private Class stemCLass;

    public RandomWordGenerator(Connect c) throws ClassNotFoundException, SQLException {
        this.language = c.getLanguage();
        this.stop = new HashSet<String>();
        this.off = new HashSet<String>();
        this.wn = c.getWn();//define english wordnet independently of language
        if (this.language.equalsIgnoreCase("en")) {
            this.stop = c.getStop();
            this.stemCLass = c.getStemCLass();
            this.off = c.getOff();
            this.inf = new InfoSummarization(c);
        } else if (this.language.equalsIgnoreCase("de")) {
            this.wnde = c.getWnde();
            this.stop = c.getStop();
            this.stemCLass = c.getStemCLass();
            this.inf = new InfoSummarization(c);
        } else if (this.language.equalsIgnoreCase("el")) {
            this.wnel = c.getWnel();
            this.stop = c.getStop();
            this.inf = new InfoSummarization(c);
        }
    }

    //for every string in the seed phrase ,return a string from hoping its synsets at wordnet 
    public String selectRandomWord(String seed, int distance) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException {

        boolean CompactForm = false;//take all the words that correspond to a stem, not just the most frequent

        seed = seed.toLowerCase().replace("'", "");
        //if the phrase is more than 3 words, do info summarization
        String delims = "[ .,;?!():\"]+";
        if (seed.split(delims).length > 3) {
            String terms = "";
            //termsTf=<words corresponding to a stem, stems tf>
            HashMap<ArrayList<String>, Double> termsTf = inf.TopTerms(seed, CompactForm);
            for (ArrayList<String> stems : termsTf.keySet()) {
                for (int j = 0; j < stems.size(); j++) {
                    terms += " " + stems.get(j);
                }
            }
            seed = terms;
        }
        String token = null;
        //tokenize the seed
        StringTokenizer tokenizer = new StringTokenizer(seed);
        Random myRand = new Random();
        myRand.setSeed(System.currentTimeMillis());
        HashMap<String, Double> TermsCPoints = new HashMap<String, Double>();
        int max = 0;
        while (tokenizer.hasMoreTokens()) {
            token = tokenizer.nextToken().trim();
            ArrayList<String> candidates = null;
            //find candidates for each token
            if (language.equals("en") && (!this.stop.contains(token)) && (!this.off.contains(token))) {
                candidates = this.computeTermSetEn(token, distance);
            } else if (language.equals("de") && (!this.stop.contains(token)) && (!this.off.contains(token))) {
                candidates = this.computeTermSetDe(token, distance);
            } else if ((!this.stop.contains(token)) && (!this.off.contains(token))) {
                candidates = this.computeTermSetEl(token, distance);
            }
            if ((!this.stop.contains(token)) && (!this.off.contains(token))) {
                //if candidates were not found, return the same word with zero points
                if (candidates == null || (candidates.size() - 1) <= 0) {
                    TermsCPoints.put(token, 0.0);
                } else {
                    //the last element is the depth of the synset
                    //the others are the words of the synset 
                    int depth = Integer.parseInt(candidates.get(candidates.size() - 1));
                    candidates.remove(candidates.size() - 1);
                    //keep max depth found
                    if (depth > max) {
                        max = depth;
                    }
                    //take the word from the synset randomly
                    String chosen = candidates.get(myRand.nextInt(candidates.size()));
                    double dist = 0;//now doesnt return anything
                    TermsCPoints.put(chosen, dist * depth / max);
                }
            }
        }

        String phrase = "";
        for (Map.Entry<String, Double> entry : TermsCPoints.entrySet()) {
            phrase += "," + entry.getKey();//+ " : " + entry.getValue() / max;
        }

        phrase = phrase.trim();
        if (phrase.startsWith(",")) {
            phrase = phrase.replaceFirst(",", "");
        }
        if (phrase.endsWith(",")) {
            phrase = phrase.substring(0, phrase.length() - 1);
        }
        phrase = phrase.trim();
        return phrase;
    }

    public ArrayList<String> computeTermSetEn(String start, int depth) {

        String currentWord = start;
        //words that have been already found between the hops
        ArrayList<String> examined = new ArrayList<String>();
        //get the terms in the synsets of the start word
        String[] terms = wn.getWordsInAllSynsets(currentWord);
        if (terms == null) {
            return null;
        }
        //make a list with the candidate terms and their respective synsets' length
        HashMap<String, Double> map = new HashMap<String, Double>();
        for (int i = 0; i < terms.length; i++) {
            double a = (double) wn.getSynsetCount(terms[i]);
            //the synsets mustn't have the start word again
            if (!terms[i].equalsIgnoreCase(start) && !terms[i].equalsIgnoreCase("")) {
                map.put(terms[i], a);
            }
        }
        //sort descending to the number of synsets that each word has
        //in order to look for the words with most synsets first
        //  int randomIndex = 0;
        LinkedHashMap<String, Double> sorted = inf.sortHashMapByValues(map);
        do {
            //search for results in depth for the top word
            //if the top word doesn't go in this depth
            //look for the next word
            examined.add(start);
            int count = 0;
            for (String cand : sorted.keySet()) {
                int max;
                do {
                    max = 0;
                    if (count == depth - 1) {
                        //if we reached the desired depth-1, take the largest synset of this candidate
                        String[] candidates = wn.getTermsOfLargestSynset(cand);
                        ArrayList<String> a = new ArrayList();
                        for (int k = 0; k < candidates.length; k++) {
                            a.add(candidates[k]);
                        }
                        //send also the depth that the synset was at
                        a.add(Integer.toString(depth));
                        return a;
                    } else {
                        //keep those who have been examined in order not to get them again in this graph treversal
                        //take the candidates' terms from synsets
                        String[] candidates = wn.getWordsInAllSynsets(cand);
                        //take the candidate with most synsets
                        if (candidates == null || candidates.length == 0) {
                            break;
                        }
                        for (int i = 0; i < candidates.length; i++) {
                            //if we haven't examined it yet and has more synset than max at this depth
                            //put him as the best candidate
                            if (wn.getSynsetCount(candidates[i]) > max && !examined.contains(candidates[i])) {
                                max = wn.getSynsetCount(candidates[i]);
                                cand = candidates[i];
                            }
                        }
                    }
                    examined.add(cand);
                    count++;
                    //until there is no candidate with more than 0 synsets
                } while (max != 0);
                currentWord = cand;
                count = 0;
                //than take the next best initial candidate
            }
            //if there can be no word found in that depth from
            //every possible synset term of the start word
            //look for a new word at depth-1
            depth--;
        } while (depth >= 0);
        //if it didn't find anything from depth to 0
        return null;
    }

    public ArrayList<String> computeTermSetDe(String start, int depth) throws IOException, SQLException {
        //   int currentDepth = depth;
        String currentWord = start;
        //get the terms in the synsets of the start word
        ArrayList<String> temp = wnde.getWordsInAllSynsets(currentWord);
        //temp.add("1");//standard weight
        //return temp;

        //words that have been already found between the hops
        ArrayList<String> examined = new ArrayList<String>();

        HashSet<String> terms = new HashSet<String>(temp);
        if (terms.isEmpty()) {
            return new ArrayList<String>();
        }
        //make a list with the candidate terms and the respective synsets length
        HashMap<String, Double> map = new HashMap<String, Double>();
        for (String s : terms) {
            double a = (double) wnde.getSynsetCount(s);
            if (!s.equalsIgnoreCase(start) && !s.equalsIgnoreCase(" ")) {
                map.put(s, a);
            }
        }
         //sorted descending to the number of synsets that each word is in
        //in order to look for the most synsets first
        if (map.isEmpty()) {
            return null;
        }
        do {
         //search for results in depth for the top word
            //if the top word doesn't go in this depth
            //look for the next word
            examined.add(start);
            int count = 0;
            LinkedHashMap<String, Double> sorted = inf.sortHashMapByValues(map);

            for (String cand : sorted.keySet()) {
                int max = 0;
                do {
                    // System.out.println("candidate " + cand + " synsets " + max);
                    max = 0;
                    if (count == depth - 1) {
                        //if we reached the desired depth-1, take the largest synset of this candidate
                        String[] candidates = wnde.getTermsOfLargestSynset(cand);
                        ArrayList<String> a = new ArrayList();
                        for (int k = 0; k < candidates.length; k++) {
                            a.add(candidates[k]);
                        }
                        a.add(Integer.toString(depth));
                        return a;
                    } else {
                        //keep those who have been examined in order not to get them again
                        examined.add(cand);
                        //take the candidates' terms from synsets
                        ArrayList<String> tempCand = wnde.getWordsInAllSynsets(cand);
                        HashSet<String> candidates = new HashSet<String>(tempCand);
         // HashSet<String> candidates = wnde.getWordsInAllSynsets(cand);
                        //take the candidate with most synsets
                        if (candidates == null) {
                            continue;
                        }
                        for (String s : candidates) {
                            //if we haven't examined it yet and has more synset than max
                            if (wnde.getSynsetCount(s) > max && !examined.contains(s) && s.length() > 3) {//meaningfull only for words bigger than 3
                                max = wnde.getSynsetCount(s);
                                cand = s;
                            }
                        }

                    }
                    count++;
                    //until the synset found is 0
                } while (max != 0);
                currentWord = cand;
                count = 0;
                //than take the next best initial candidate
            }
         //if there can be no word found in that depth from
            //every possible synset term of the start word
            //look for it at depth-1
            examined.clear();
            depth--;
        } while (depth >= 0);
        //if it didn't find anything from 0 to depth
        return null;
    }

    public ArrayList<String> computeTermSetEl(String start, int depth) throws IOException, SQLException {

        String currentWord = start;
        //words that have been already found between the hops
        ArrayList<String> examined = new ArrayList<String>();

        //get the terms in the synsets of the start word
        ArrayList<String> temp = wnel.getWordsInAllSynsets(currentWord);
        HashSet<String> terms = new HashSet<String>(temp);
        //HashSet<String> terms = wnel.getWordsInAllSynsets(currentWord);
        if (terms.equals(null)) {
            return new ArrayList<String>();
        }
        if (terms.isEmpty()) {
            return new ArrayList<String>();
        }
        //make a list with the candidate terms and the respective synsets length
        HashMap<String, Double> map = new HashMap<String, Double>();
        for (String s : terms) {
            double a = (double) wnel.getSynsetCount(s);
            if (!s.equalsIgnoreCase(start) && !s.equalsIgnoreCase(" ")) {
                map.put(s, a);
            }
        }

        //int randomIndex = 0;
        if (map.isEmpty()) {
            return new ArrayList<String>();
        }
        do {
            //search for results in depth for the top word
            //if the top word doesn't go in this depth
            //look for the next word
            examined.add(start);
            int count = 0;
            //sorted descending to the number of synsets that each word is in
            //in order to look for the most synsets first
            LinkedHashMap<String, Double> sorted = inf.sortHashMapByValues(map);

            for (String cand : sorted.keySet()) {
                int max = 0;
                do {
                    //  System.out.println("candidate " + cand + " synsets " + max);
                    max = 0;
                    if (count == depth - 1) {
                        //if we reached the desired depth-1, take the largest synset of this candidate
                        String[] candidates = wnel.getTermsOfLargestSynset(cand);
                        ArrayList<String> a = new ArrayList();
                        for (int k = 0; k < candidates.length; k++) {
                            a.add(candidates[k]);
                        }
                        a.add(Integer.toString(depth));
                        return a;
                    } else {
                        //keep those who have been examined in order not to get them again
                        examined.add(cand);
                        //take the candidates' terms from synsets
                        ArrayList<String> tempCand = wnel.getWordsInAllSynsets(cand);
                        HashSet<String> candidates = new HashSet<String>(tempCand);
                        // HashSet<String> candidates = wnel.getWordsInAllSynsets(cand);
                        //take the candidate with most synsets
                        if (candidates == null) {
                            continue;
                        }
                        for (String s : candidates) {
                            //if we haven't examined it yet and has more synset than max
                            if (wnel.getSynsetCount(s) > max && !examined.contains(s) && s.length() > 3) {
                                max = wnel.getSynsetCount(s);
                                cand = s;
                            }
                        }

                    }
                    count++;
                    //until the synset found is 0
                } while (max != 0);
                currentWord = cand;
                count = 0;
                //than take the next best initial candidate
            }
            //if there can be no word found in that depth from
            //every possible synset term of the start word
            //look for it at depth-1
            examined.clear();
            depth--;
        } while (depth >= 0);
        //if it didn't find anything from 0 to depth
        return new ArrayList<String>();
    }

}

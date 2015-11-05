/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.cru.creativity.reasoning.semantic;

import gr.demokritos.iit.cru.creativity.utilities.Connect;
import eu.c2learn.tokenizers.NGramTokenizer;
import gr.demokritos.iit.cru.creativity.utilities.WNAccess;
import gr.demokritos.iit.cru.creativity.utilities.WNDE;
import gr.demokritos.iit.cru.creativity.utilities.WNEL;
import java.io.IOException;
import static java.lang.Math.log;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Giorgos
 */
public class InfoSummarization {

    private String language;
    private Set<String> stop;
    private Set<String> off;
    private Class stemCLass;
    private WNAccess wn;
    private WNDE wnde;
    private WNEL wnel;

    public InfoSummarization(Connect c) throws ClassNotFoundException, SQLException {

        this.language = c.getLanguage();
        this.stop = new HashSet<String>();
        this.off = new HashSet<String>();
        if (this.language.equalsIgnoreCase("en")) {
            this.wn = c.getWn();
            this.stop = c.getStop();
            this.stemCLass = c.getStemCLass();
            this.off = c.getOff();
        } else if (this.language.equalsIgnoreCase("de")) {
            this.wnde = c.getWnde();
            this.stop = c.getStop();
            this.stemCLass = c.getStemCLass();
        } else if (this.language.equalsIgnoreCase("el")) {
            this.wnel = c.getWnel();
            this.stop = c.getStop();
        }
    }

    public Set<String> getStop() {
        Set<String> st = this.stop;
        st.addAll(this.off);
        return st;
    }

    //top terms of web pages based on tf-idf

    public LinkedHashMap<ArrayList<String>, Double> TopTermsBing(Set<String> pages, boolean CompactForm) throws InstantiationException, IllegalAccessException, IOException, SQLException {
        int SizeOfTop = 50;
        String delims = "[{}\\[\\] .,;?!():\"]+";///todo add [] to the online
        //one hashamp for stem-idfs
        HashMap<String, Double> idfs = new HashMap<String, Double>();
        //one hashmap for stems-pageFreq
        HashMap<String, Integer> pageFreq = new HashMap<String, Integer>();
        //one hashmap for stem-tfs
        HashMap<String, Double> tfs = new HashMap<String, Double>();
        //one hashmap for terms-frequency
        HashMap<String, Integer> termFreq = new HashMap<String, Integer>();
        //one hashmap for terms-stems
        HashMap<String, String> termStem = new HashMap<String, String>();
        int nGramOrder = 1;
        Set<String> stopWordSet = stop;
        stopWordSet.addAll(off);
        //final hashmap with terms: weight
        LinkedHashMap<ArrayList<String>, Double> TagCloud = new LinkedHashMap<ArrayList<String>, Double>();
        if (language.equals("el")) {
            for (String p : pages) {
                String[] tokenList = p.toLowerCase().split(delims);
                termFreq = new HashMap<String, Integer>(); //initialize for each page
                for (int t = 0; t < tokenList.length; t++) {
                    if (tokenList[t].length() <= 1 || tokenList[t].matches(delims)) {
                        break;
                    }
                    String term = tokenList[t];
                    if (termFreq.containsKey(term)) {
                        termFreq.put(term, termFreq.get(term) + 1);
                    } else {
                        termFreq.put(term, 1);
                    }
                }
                for (Map.Entry<String, Integer> freq : termFreq.entrySet()) {
                    String key = freq.getKey();
                    double val = freq.getValue();
                    //add to tfs hashmap the freqs for this page 
                    if (tfs.containsKey(freq.getKey())) {
                        tfs.put(key, tfs.get(key) + log(val + 1));
                    } else {
                        tfs.put(key, log(val + 1));
                    }
                    //add to the page freq of the term, +1
                    if (pageFreq.containsKey(key)) {
                        pageFreq.put(key, pageFreq.get(key) + 1);
                    } else {
                        pageFreq.put(key, 1);
                    }
                }
            }
            //the idfs derive from page frequency of a term
            for (Map.Entry<String, Integer> freq : pageFreq.entrySet()) {
                idfs.put(freq.getKey(), log(pages.size() / freq.getValue()));//if it parses only one page, there will be all 0
            }
            HashMap<String, Double> termsWeights = new HashMap<String, Double>();
            for (Map.Entry<String, Double> entry : tfs.entrySet()) {
                String key = entry.getKey().toString();
                if (idfs.containsKey(key)) {
                    //tf*idf/|p|
                    termsWeights.put(key, (entry.getValue() * idfs.get(key)) / pages.size());
                }
            }
            LinkedHashMap<String, Double> sorted = sortHashMapByValues(termsWeights);
            //find best terms
            Iterator sortKey = sorted.keySet().iterator();
            int i = 0;
            //keep the top SizeOfTop stems from the sorted list of stems
            //for the first SizeOfTop stems
            while (sortKey.hasNext() && i < SizeOfTop) {
                String term = sortKey.next().toString();
                if (this.wnel.getSynsetCount(term) > 0) {
                    ArrayList<String> temp = new ArrayList();
                    temp.add(term);
                    TagCloud.put(temp, sorted.get(term));
                    i++;
                }
            }
        } else {
            NGramTokenizer tokenizer = new NGramTokenizer(true, stopWordSet, nGramOrder);
            snowball.SnowballStemmer stemmer = (snowball.SnowballStemmer) this.stemCLass.newInstance();
            //tokenize each page
            for (String p : pages) {
                List<String> tokenList = tokenizer.tokenize(p);
                //take the stem of each word and store 
                //1) the frequency of the term in all documents
                //2) the terms and their correspondig stems
                //3) the frequency of the stem in each document
                //4) the sum of 3) for every stem for all documents
                //5) the document frequensy of a stem
                //6) make 5) idf
                //stemFreq for this page
                HashMap<String, Integer> stemFreq = new HashMap<String, Integer>();
                for (int t = 0; t < tokenList.size(); t++) {
                    if (tokenList.get(t).length() <= 1 || tokenList.get(t).matches(delims)) {
                        break;
                    }

                    String term = tokenList.get(t);
                    stemmer.setCurrent(term);
                    stemmer.stem();
                    String stem = stemmer.getCurrent();
                    //chek if stopword doesn't contain the term and the stem
                    if (!stopWordSet.contains(term) && !stopWordSet.contains(stem)) {
                        termStem.put(term, stem);
                        //count the frequency of the stem in this page
                        if (stemFreq.containsKey(stem)) {
                            stemFreq.put(stem, stemFreq.get(stem) + 1);
                        } else {
                            stemFreq.put(stem, 1);
                        }

                        //count the frequency of the term in all pages
                        if (termFreq.containsKey(term)) {
                            termFreq.put(term, termFreq.get(term) + 1);
                        } else {
                            termFreq.put(term, 1);
                        }
                    }
                }
                ///after the end of stemming
                for (Map.Entry<String, Integer> freq : stemFreq.entrySet()) {
                    String key = freq.getKey();
                    double val = freq.getValue();
                    //add to tfs hashmap the freqs for this page 
                    if (tfs.containsKey(freq.getKey())) {
                        tfs.put(key, tfs.get(key) + log(val + 1));
                    } else {
                        tfs.put(key, log(val + 1));
                    }
                    //add to the page freq of the stem, +1
                    if (pageFreq.containsKey(key)) {
                        pageFreq.put(key, pageFreq.get(key) + 1);
                        //System.out.print(key+" "+pageFreq.get(key));
                    } else {
                        pageFreq.put(key, 1);
                    }
                }
            }
            ///turn pageFreq into idf
            for (Map.Entry<String, Integer> freq : pageFreq.entrySet()) {
                idfs.put(freq.getKey(), log(pages.size() / freq.getValue()));//if it parses only one page, there will be all 0
            }
            //the stem and their weights
            HashMap<String, Double> stemsWeights = new HashMap<String, Double>();
            for (Map.Entry<String, Double> entry : tfs.entrySet()) {
                String key = entry.getKey().toString();
                if (idfs.containsKey(key)) {
                    //tf*idf/|p|
                    stemsWeights.put(key, (entry.getValue() * idfs.get(key)) / pages.size());
                }
            }
            LinkedHashMap<String, Double> sorted = sortHashMapByValues(stemsWeights);
            //find best stems
            Iterator sortKey = sorted.keySet().iterator();
            int i = 0;
            //keep the top SizeOfTop stems from the sorted list of stems
            //for the first SizeOfTop stems
            while (sortKey.hasNext() && i < SizeOfTop) {
                String stem = sortKey.next().toString();
                Iterator keyIt = termStem.keySet().iterator();
                //seperate for the two languages
                if ((language.equalsIgnoreCase("en") && this.wn.getPos(stem).length > 0) || (language.equalsIgnoreCase("de") && this.wnde.getSynsetCount(stem) > 0)) {
                    //System.out.println(stem + " " + this.wn.getPos(stem)[0]);
                    //take all the corresponding terms of this stem
                    ArrayList<String> cor = new ArrayList<String>();
                    while (keyIt.hasNext()) {
                        String word = keyIt.next().toString();
                        //if stem is the same as the value in termStem map, the key is the corresponding term
                        if (termStem.get(word).equalsIgnoreCase(stem)) {
                            cor.add(word);
                        }
                    }
                    ArrayList<String> terms = new ArrayList<String>();
                    if (CompactForm) {
                        //for each of the dominant stems, take the most frequenced corresponding term
                        int max = 0;
                        String bestCandidate = "";
                        //take the most frequent term from the corresponding ones
                        for (int p = 0; p < cor.size(); p++) {
                            //System.out.println(cor.get(p));
                            int candFreq = termFreq.get(cor.get(p));
                            if (candFreq > max) {
                                max = candFreq;
                                bestCandidate = cor.get(p);
                            }
                        }
                        if (!bestCandidate.equalsIgnoreCase(" ") && !bestCandidate.equalsIgnoreCase(null) && !bestCandidate.isEmpty()) {
                            //keep the others terms hashed
                            bestCandidate = bestCandidate.concat("{");
                            for (int p = 0; p < cor.size(); p++) {
                                bestCandidate = bestCandidate.concat(cor.get(p));
                                if (p != cor.size() - 1) {
                                    bestCandidate = bestCandidate.concat(",");
                                }
                            }
                            bestCandidate = bestCandidate.concat("}");
                            terms.add(bestCandidate);
                        }
                    } else {
                        //else put all the terms of the particular stem
                        terms = cor;
                    }
                    TagCloud.put(terms, sorted.get(stem));
                    i++;
                }
            }
        }
        return TagCloud;
    }

    //top terms of a story based on stem tf
    public LinkedHashMap<ArrayList<String>, Double> TopTerms(String phrase, boolean CompactForm) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException, SQLException {
        String delims = "[{}\\[\\] .,;?!():\"]+";
        // System.out.println(phrase);
        String[] tokens;
        if (this.language.equalsIgnoreCase("de")) {
            tokens = phrase.split(delims);
        } else {
            tokens = phrase.toLowerCase().split(delims);
        }
        int SizeOfTop = 0;
        if (this.language.equalsIgnoreCase("de")) {
            SizeOfTop = (int) Math.ceil(0.2 * tokens.length);
        } else {
            SizeOfTop = (int) Math.ceil(0.3 * tokens.length);
        }
//split the phrase and keep the tokens
        LinkedHashMap<ArrayList<String>, Double> top = new LinkedHashMap<ArrayList<String>, Double>();
        //there is no stemming in greek
        if (this.language.equalsIgnoreCase("el")) {
            HashMap<String, Integer> termFreq = new HashMap<String, Integer>();
            for (int i = 0; i < tokens.length; i++) {
                if (!stop.contains(tokens[i]) && !off.contains(tokens[i]) && tokens[i] != null && tokens[i].length() > 1) {
                    if (termFreq.containsKey(tokens[i])) {
                        termFreq.put(tokens[i], termFreq.get(tokens[i]) + 1);
                    } else {
                        termFreq.put(tokens[i], 1);
                    }
                }
            }
            //make tf out of frequency
            HashMap<String, Double> tfDict = new HashMap<String, Double>();
            for (Map.Entry<String, Integer> freq : termFreq.entrySet()) {
                tfDict.put(freq.getKey(), log(freq.getValue() + 1));
            }
            if (SizeOfTop == 0) {
                //sizeoftop==0 means we get the full tag cloud of all the terms, not just the first ones
                for (String key : tfDict.keySet()) {
                    //arraylist has only one element, the key, because there is no stemming
                    ArrayList<String> temp = new ArrayList();
                    // if (!key.equalsIgnoreCase("")) {
                    temp.add(key);
                    //}
                    top.put(temp, tfDict.get(key));
                }

                return top;
            }
            //descending sort the dict hashmap by frequency
            LinkedHashMap<String, Double> sorted = sortHashMapByValues(tfDict);
            Iterator sortKey = sorted.keySet().iterator();
            int i = 0;
            //final hashmap with stem:term
            //keep the top SizeOfTop terms from the sorted list of stems
            while (sortKey.hasNext() && i < SizeOfTop) {
                String term = sortKey.next().toString();
                //System.out.println("term " + term+" "+this.wnel.getSynsetCount(term));

                // if (this.wnel.getSynsetCount(term) > 0) {//for now
                ArrayList<String> temp = new ArrayList();
                temp.add(term);
                top.put(temp, sorted.get(term));
                i++;
                //    }
            }
            return top;
        } else {
            snowball.SnowballStemmer stemmer = (snowball.SnowballStemmer) this.stemCLass.newInstance();
            //one hashmap for terms-stems
            HashMap<String, String> termStem = new HashMap<String, String>();
            //one hashmap for terms-frequency
            HashMap<String, Integer> termFreq = new HashMap<String, Integer>();
            //one for stems-frequenc
            HashMap<String, Integer> stemFreq = new HashMap<String, Integer>();
            for (int i = 0; i < tokens.length; i++) {
                //don't keep stopwords
                //System.out.println(tokens[i]);
                if (!stop.contains(tokens[i]) && !off.contains(tokens[i]) && tokens[i] != null && tokens[i].length() > 1) {
                    stemmer.setCurrent(tokens[i]);
                    stemmer.stem();
                    String stem = stemmer.getCurrent();
                    //tally the term with the corresponding stem
                    termStem.put(tokens[i], stem);
                    //count the frequency of the stem
                    if (stemFreq.containsKey(stem)) {
                        stemFreq.put(stem, stemFreq.get(stem) + 1);
                    } else {
                        stemFreq.put(stem, 1);
                    }
                    //same for term frequency
                    if (termFreq.containsKey(tokens[i])) {
                        termFreq.put(tokens[i], termFreq.get(tokens[i]) + 1);
                    } else {
                        termFreq.put(tokens[i], 1);
                    }
                }
            }
            //make frequency ->tf=log(f+1)
            HashMap<String, Double> tfDict = new HashMap<String, Double>();
            //if all the words were removed return empty
            if (stemFreq.isEmpty()) {
                return top;
            }
            for (Map.Entry<String, Integer> freq : stemFreq.entrySet()) {
                tfDict.put(freq.getKey(), log(freq.getValue() + 1));
            }
            //descending sort the dict hashmap by frequency
            LinkedHashMap<String, Double> sorted = sortHashMapByValues(tfDict);
            Iterator sortKey = sorted.keySet().iterator();
            int i = 0;
            //final hashmap with terms:tf
            //keep the top SizeOfTop stems from the sorted list of stems
            if (SizeOfTop == 0) {
                //sizeoftop==0 means we get the full tag cloud of all the terms, not just the first ones
                SizeOfTop = sorted.size();
            }
            //for the first SizeOfTop stems
            while (sortKey.hasNext() && i < SizeOfTop) {
                String stem = sortKey.next().toString();
                if ((language.equalsIgnoreCase("en") && this.wn.getPos(stem).length > 0) || (language.equalsIgnoreCase("de") && this.wnde.getSynsetCount(stem) > 0)) {
                    Iterator keyIt = termStem.keySet().iterator();
                    //take all the corresponding terms of this stem
                    ArrayList<String> cor = new ArrayList<String>();
                    while (keyIt.hasNext()) {
                        String word = keyIt.next().toString();
                        //if stem is the same as the value in termStem map, the key is the corresponding term
                        if (termStem.get(word).equalsIgnoreCase(stem)) {
                            cor.add(word);
                        }
                    }
                    ArrayList<String> term = new ArrayList<String>();
                    //if compact is true
                    if (CompactForm) {
                        //for each of the dominant stems, take the most frequenced corresponding term
                        int max = 0;
                        String bestCandidate = "";
                        //take the most frequent term from the corresponding ones
                        for (int p = 0; p < cor.size(); p++) {
                            int cand = termFreq.get(cor.get(p));
                            if (cand > max) {
                                max = cand;
                                bestCandidate = cor.get(p);
                            }
                        }
                        if (!bestCandidate.equalsIgnoreCase(" ") && !bestCandidate.equalsIgnoreCase(null) && !bestCandidate.isEmpty()) {
                            //keep the others terms hashed
                            bestCandidate = bestCandidate.concat("{");
                            for (int p = 0; p < cor.size(); p++) {
                                bestCandidate = bestCandidate.concat(cor.get(p));
                                if (p != cor.size() - 1) {
                                    bestCandidate = bestCandidate.concat(",");
                                }
                            }
                            bestCandidate = bestCandidate.concat("}");
                            term.add(bestCandidate);
                        }
                    } else {
                        //else put all the terms of the particular stem
                        term = cor;
                    }
                    //System.out.println(stem + " " + bestCandidate);
                    //System.out.println(term + " " + sorted.get(stem));
                    top.put(term, (Double) sorted.get(stem));
                    i++;
                }
            }
            return top;
        }
    }

    public LinkedHashMap<String, Double> sortHashMapByValues(HashMap<String, Double> passedMap) {
        List mapKeys = new ArrayList(passedMap.keySet());
        List mapValues = new ArrayList(passedMap.values());
        //sort the value set sepparately
        Collections.sort(mapValues, Collections.reverseOrder());
        //Collections.sort(mapKeys);
        LinkedHashMap<String, Double> sortedMap = new LinkedHashMap();
        Iterator valueIt = mapValues.iterator();
        while (valueIt.hasNext()) {
            Object val = valueIt.next();
            Iterator keyIt = mapKeys.iterator();
            //find the key that corresponds to the sorted value
            while (keyIt.hasNext()) {
                Object key = keyIt.next();
                String comp1 = passedMap.get(key).toString();
                String comp2 = val.toString();
                //tally that key to this value in the sorted hashmap
                if (comp1.equals(comp2)) {
                    passedMap.remove(key);
                    mapKeys.remove(key);
                    sortedMap.put((String) key, (Double) val);
                    break;
                }
            }
        }
        return sortedMap;
    }
}

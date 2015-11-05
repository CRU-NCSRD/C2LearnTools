/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.cru.creativity.reasoning.semantic;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import gr.demokritos.iit.cru.creativity.utilities.Connect;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 *
 * @author Giwrgos
 */
public class CompetitiveThinkingSpaces {

    public static void main(String[] args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, Exception {
        System.out.println(CompetitiveThinkingSpaces("Oral storytelling is perhaps the earliest method for sharing narratives. During most people's childhoods, narratives are used to guide them on proper behavior, cultural history, formation of a communal identity, and values, as especially studied in anthropology today among traditional indigenous peoples.[3] Narratives may also be nested within other narratives, such as narratives told by an unreliable narrator (a character) typically found in noir fiction genre.", 3, "en"));
    }

    public static ArrayList<String> CompetitiveThinkingSpaces(String story, int noOfClusters, String language) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException, IOException, Exception {

        ArrayList<String> tokensList = new ArrayList<String>();
        Connect c = new Connect(language);
        InfoSummarization inf = new InfoSummarization(c);
        LinkedHashMap<ArrayList<String>, Double> temp = inf.TopTerms(story, true);
        for (ArrayList<String> stems : temp.keySet()) {
            for (int j = 0; j < stems.size(); j++) {
                //for every stem, put each of its corresponding terms to tagCloud with the stem's tf 
                tokensList.add(stems.get(j).split("\\{")[0] + ";" + temp.get(stems));
            }
        }
        c.CloseConnection();

        //String clusters = properties.getProperty("clusters");
        ArrayList<String> clustersList = new ArrayList<String>();
        KeyphraseClustering kc = new KeyphraseClustering(tokensList, noOfClusters, language);
        Connect m = new Connect(language);
        clustersList = kc.getClusters();
        int dom = 0;
        for (int i = 0; i < clustersList.size(); i++) {
            if (clustersList.get(i).split(";").length > tokensList.size() / 2) {
                dom = i;
            }
        }
        String[] words = clustersList.get(dom).subSequence(clustersList.get(dom).indexOf(";") + 1, clustersList.get(dom).length()).toString().split(";");//keep the rest of the words in the big cluster
        clustersList.set(dom, clustersList.get(dom).split(";")[0] + ";");//first cluster is its first word
        int noWords = (words.length + clustersList.size()) / clustersList.size();
        HashMap<String, Double> tags = new HashMap<String, Double>();
        for (String s : words) {//for all the candidate words
            for (int i = 0; i < clustersList.size(); i++) {
                tags.put(s + "-" + i, m.getDistance(s, clustersList.get(i).split(";")[0]));//semlev(words, clustersList));//store their difference between the words and each of the clusters
                
            }
        }
        Comparator<String> valueComparator = Ordering.natural().onResultOf(Functions.forMap(tags)).compound(Ordering.natural());
        ImmutableSortedMap<String, Double> es = ImmutableSortedMap.copyOf(tags, valueComparator);

        ArrayList<String> examined = new ArrayList<String>();
        ///sort tags ascending with guava
        for (String o : es.keySet()) {
            String[] g = o.split("-");
            int pointer = Integer.parseInt(g[1]);
            if (clustersList.get(pointer).split(";").length > noWords || examined.contains(g[0])) {
                    ///if the cluster has already as much words as it should have, 
                //or if the word has already been stored into a cluster
                continue; //continue with the next minimum difference in the list
            }
            examined.add(g[0]);
            clustersList.set(pointer, clustersList.get(pointer) + g[0] + ";");
        }

        if (examined.size() < words.length) {// if some words have not been set to clusters
            //put them in the first cluster
            for (String h : words) {
                if (!examined.contains(h)) {
                    clustersList.set(0, clustersList.get(0) + h + ";");
                }
            }
        }
        m.CloseConnection();
        return clustersList;
    }
}

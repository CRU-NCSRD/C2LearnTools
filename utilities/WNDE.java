/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.cru.creativity.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 *
 * @author antonis
 */
public class WNDE {

    // private String wnFile;
    private Connection c;

    public WNDE(Connection conn) {
        this.c = conn;
    }

    public void insertData(File fin) throws SQLException, FileNotFoundException, IOException {
        
        BufferedReader br = new BufferedReader(new FileReader(fin));
        Statement statement = c.createStatement();
        String line = null;
        while ((line = br.readLine()) != null) {
            if (!line.startsWith("#")) {
                String q = "insert into temp (synset) values ('" + line.replace("'", "") + "')";
                statement.executeUpdate(q);
            }
        }

        br.close();
    }

    public ArrayList<String> getWordsInAllSynsets(String word) throws IOException, SQLException {

        //take the synonym words of word
        Statement statement = c.createStatement();
        String q = "SELECT synset FROM c2learn.thesaurus_de where synset LIKE '%;" + word.toLowerCase() + ";%' or synset LIKE '%" + word.toLowerCase() + ";%' or synset LIKE '%;" + word.toLowerCase() + "%'"; //like '%subj%' and levenstein(Best_Entity_literalString,'" + subj + "')<=" + d;
        //String currentSynset = null;

        ResultSet rs;
        rs = statement.executeQuery(q);
        ArrayList<String> s = new ArrayList<String>();
        //int count = 0;
        while (rs.next() && s.size() < 50) { //keep the 50 first words, for complexity purposes
            String[] t = rs.getString("synset").trim().split(";");

            if (t.length > 0) {
                for (String h : t) {
                    s.add(h);
                }
            }
        }
        return s;
    }

  
    //get the count of the synsets that a word has
    public int getSynsetCount(String word) throws IOException, SQLException {
        Statement statement = c.createStatement();
        String q = "SELECT count(*) as c FROM c2learn.thesaurus_de where synset LIKE '%;" + word.toLowerCase() + ";%' or synset LIKE '%" + word.toLowerCase() + ";%' or synset LIKE '%;" + word.toLowerCase() + "%'"; //like '%subj%' and levenstein(Best_Entity_literalString,'" + subj + "')<=" + d;
        // System.out.println(q);
        String currentSynset = null;
        int t = 0;
        ResultSet rs;
        rs = statement.executeQuery(q);
        while (rs.next()) {
            t = rs.getInt("c");
        }
        return t;
     
    }

    public String[] getTermsOfLargestSynset(String word) throws FileNotFoundException, UnsupportedEncodingException, IOException, SQLException {
        Statement statement = c.createStatement();
        String q = "SELECT synset FROM c2learn.thesaurus_de where synset LIKE '%;" + word.toLowerCase() + ";%' or synset LIKE '%" + word.toLowerCase() + ";%' or synset LIKE '%;" + word.toLowerCase() + "%'"; //like '%subj%' and levenstein(Best_Entity_literalString,'" + subj + "')<=" + d;
        String currentSynset = null;
        int max = 0;
        ResultSet rs;
        String terms = "";
        rs = statement.executeQuery(q);
        HashSet<String> s = new HashSet<String>();
        while (rs.next()) {
            String t = rs.getString("synset").trim();
            if (t.split(";").length > max) {
                terms = t;
                max = t.split(";").length;
            }
        }
        return terms.split(";");
     
    }

    public double getDistance(String w1, String w2) throws SQLException {
        Statement statement = c.createStatement();
        int SynsetsOfw1 = 0;
        int SynsetsOfw2 = 0;
        int depth = 0;

       String q = "SELECT count(*) as c FROM c2learn.thesaurus_de where synset LIKE '%;" + w1 + ";%' or synset LIKE '%" + w1 + ";%' or synset LIKE '%;" + w1 + "%'"; //like '%subj%' and levenstein(Best_Entity_literalString,'" + subj + "')<=" + d;
        ResultSet rs = statement.executeQuery(q);
        while (rs.next()) {
            SynsetsOfw1 = rs.getInt("c");
            if (SynsetsOfw1 == 0) {
                return 1;//word not found
            }
        }

        //keep all synsets of the second word
        q = "SELECT synset FROM c2learn.thesaurus_de where synset LIKE '%;" + w2 + ";%' or synset LIKE '%" + w2 + ";%' or synset LIKE '%;" + w2 + "%'";
        
        rs = statement.executeQuery(q);
        HashSet<String> s = new HashSet<String>();
        SynsetsOfw2 = 0;
        while (rs.next() && s.size() < 50) { //<50 for computational reasons
            String[] t = rs.getString("synset").trim().split(";");
            SynsetsOfw2++;
            if (t.length > 0) {
                for (String h : t) {
                    if (h.equalsIgnoreCase(w1)) {// if w1 is a direct synonim
                        return 0.0;
                    }
                    s.add(h);
                }
            }
        }
        if (SynsetsOfw2 == 0) {
            return 1;//word not found
        }
       // return abs(SynsetsOfw1-SynsetsOfw2)/max(SynsetsOfw1,SynsetsOfw2);*/
        
        //search through all words in synsets of w2 to find synsets that contain w1, recursively in depth
        boolean found = false;
        ArrayList<String> examined = new ArrayList<String>();
        while (!found) {
            HashSet<String> sNew = new HashSet<String>();
            depth++;
            //for each word, take its synsets and check if w1 is in them
            for (String h : s) {
                q = "SELECT synset FROM c2learn.thesaurus_de where synset LIKE '%;" + h + ";%' or synset LIKE '%" + h + ";%' or synset LIKE '%;" + h + "%'";
                rs = statement.executeQuery(q);
                while (rs.next()) {//<50 for computational reasons &&sNew.size()<50
                    String t = rs.getString("synset");
                    //   System.out.println(t+" "+depth);
                    if (examined.contains(t)) {
                        continue;
                    }
                    if (t.contains(w1)) {
                        //if it is return the formula
                        return depth / (double) (depth + SynsetsOfw1 + SynsetsOfw2);
                    }
                    //add to new synonim sets, every word
                    sNew.addAll(Arrays.asList(t.split(";")));
                    examined.add(t);
                }
            }
            //keep the new , increased set of words
            s = sNew;
              if(sNew==s){
                  return 1;
              }
        }
        return 1;//return 1 if the word is not found
    }
}

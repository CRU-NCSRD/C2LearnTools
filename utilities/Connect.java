/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.cru.creativity.utilities;

import gr.demokritos.iit.cru.creativity.utilities.WNAccess;
import gr.demokritos.iit.cru.creativity.utilities.WNDE;
import gr.demokritos.iit.cru.creativity.utilities.WNEL;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.apache.commons.lang3.StringUtils.getLevenshteinDistance;

/**
 *
 * @author George
 */
public class Connect {

    private String language;
    private WNAccess wn;
    private WNDE wnde;
    private WNEL wnel;
    private Set<String> stop;
    private Set<String> off;

    public String getEnglish_thes() {
        return english_thes;
    }

    public void setEnglish_thes(String english_thes) {
        this.english_thes = english_thes;
    }

    public String getGerman_thes() {
        return german_thes;
    }

    public void setGerman_thes(String german_thes) {
        this.german_thes = german_thes;
    }

    public String getGreek_thes() {
        return greek_thes;
    }

    public void setGreek_thes(String greek_thes) {
        this.greek_thes = greek_thes;
    }
    private Class stemCLass;
    private Connection conn;
    private String english_thes;
    private String german_thes;
    private String greek_thes;
    private String bingAppId;
    
    public Connection getConn() {
        return conn;
    }

    public void setConn(Connection conn) {
        this.conn = conn;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public WNAccess getWn() {
        return wn;
    }

    public void setWn(WNAccess wn) {
        this.wn = wn;
    }

    public WNDE getWnde() {
        return wnde;
    }

    public void setWnde(WNDE wnde) {
        this.wnde = wnde;
    }

    public WNEL getWnel() {
        return wnel;
    }

    public void setWnel(WNEL wnel) {
        this.wnel = wnel;
    }

    public Set<String> getStop() {
        return stop;
    }

    public void setStop(Set<String> stop) {
        this.stop = stop;
    }

    public Set<String> getOff() {
        return off;
    }

    public void setOff(Set<String> off) {
        this.off = off;
    }

    public Class getStemCLass() {
        return stemCLass;
    }

    public void setStemCLass(Class stemCLass) {
        this.stemCLass = stemCLass;
    }

    public void CloseConnection() {
        try {
            this.conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(Connect.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Connect(String lang) throws ClassNotFoundException, SQLException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Properties properties = new Properties();
        try {
           properties.load(classLoader.getResourceAsStream(".." + File.separator + ".." + File.separator + "webservices.properties"));

         } catch (IOException ex) {
            Logger.getLogger(Connect.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        Class.forName("com.mysql.jdbc.Driver");
        String connection_string = properties.getProperty("connection");//"jdbc:mysql://127.0.0.1:3306/c2learn";
        this.conn = DriverManager.getConnection(connection_string, properties.getProperty("user"), properties.getProperty("password"));// "root" "pass"

        this.language = lang;
        String database_dir = properties.getProperty("wordnet.database.dir");
        this.wn = new WNAccess(database_dir);

        this.wnde = new WNDE(this.conn);//this.conn

        String thesaurus_el = properties.getProperty("openthesaurus_EL");
        this.wnel = new WNEL(thesaurus_el);
        this.english_thes = properties.getProperty("english_thes");
        this.german_thes = properties.getProperty("german_thes");
        this.greek_thes = properties.getProperty("greek_thes");
        this.stop = new HashSet<String>();
        this.off = new HashSet<String>();
        this.bingAppId=properties.getProperty("bingAppId");
        
        String stopWordsEN = properties.getProperty("stopWordsFile_EN");
        String stopWordsFile = stopWordsEN;
        //define stopWords based on the language
        if (language.equalsIgnoreCase("en")) {
            this.stemCLass = Class.forName("snowball.ext.englishStemmer");
            //take the offensive words file
            String offensive = properties.getProperty("offensive");
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(offensive), "UTF8"));
                String offWord = new String();
                while ((offWord = br.readLine()) != null) {
                    if (offWord.trim().compareToIgnoreCase("") == 0) {
                        continue;
                    }
                    this.off.add(offWord.trim());
                }
                br.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (language.equalsIgnoreCase("de")) {
            this.stemCLass = Class.forName("snowball.ext.dutchStemmer");
            String stopWordsDE = properties.getProperty("stopWordsFile_DE");
            stopWordsFile = stopWordsDE;
        } else if (language.equalsIgnoreCase("el")) {
            //there is no stemming in greek
            String stopWordsGR = properties.getProperty("stopWordsFile_GR");
            stopWordsFile = stopWordsGR;
        }
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(stopWordsFile), "UTF8"));
            String stopWord = new String();
            while ((stopWord = br.readLine()) != null) {
                if (stopWord.trim().compareToIgnoreCase("") == 0) {
                    continue;
                }
                this.stop.add(stopWord.trim());
            }
            br.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String getBingAppId() {
        return bingAppId;
    }

    public void setBingAppId(String bingAppId) {
        this.bingAppId = bingAppId;
    }

    public double getDistance(String s1, String s2) throws SQLException {
        double sem = 0.0;
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();
        if (this.language.equalsIgnoreCase("en")) {
            sem = 0.75 * wn.getDistance(s1, s2);
        } else if (this.language.equalsIgnoreCase("de")) {
            sem = 0.75 * wnde.getDistance(s1, s2);
        } else if (this.language.equalsIgnoreCase("el")) {
            sem = 0.75 * wnel.getDistance(s1, s2);
        }
        if (sem == 0.75) {
            return 1;
        }
        double lev = 0.25 * getLevenshteinDistance(s1, s2) / ((s1.length() + s2.length()) / 2);
        return sem + lev;
    }
}

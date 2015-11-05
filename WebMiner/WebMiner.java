/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.cru.creativity.reasoning.semantic;

import com.google.gson.Gson;
import eu.c2learn.crawlers.BingCrawler;
import eu.c2learn.tokenizers.HTMLPages;
import gr.demokritos.iit.cru.creativity.utilities.Connect;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 *
 * @author Giwrgos
 */
public class WebMiner {

    public static void main(String[] args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, Exception {
        System.out.println(WebMiner("dog", 3, "en", true));
    }

    public static String WebMiner(String seed, int difficulty, String language, boolean compactForm) throws ClassNotFoundException, SQLException, IOException, InstantiationException, IllegalAccessException {
        Gson gson = new Gson();
        Connect c = new Connect(language);
        RandomWordGenerator r = new RandomWordGenerator(c);
        String randomPhrase = r.selectRandomWord(seed, difficulty).replace(",", " ");
        InfoSummarization inf = new InfoSummarization(c);
        LinkedHashMap<String, Double> TagCloud = new LinkedHashMap<String, Double>();

        Set<String> pages = new HashSet<String>();
        ArrayList<String> urls = new ArrayList<String>();
        ArrayList<String> urls_temp = new ArrayList<String>();
        if (language.equalsIgnoreCase("en")) {
            if (randomPhrase.length() == 0) {
                randomPhrase = seed;
            }
            String bingAppId = c.getBingAppId();
            BingCrawler bc = new BingCrawler(bingAppId, language);
            urls_temp = bc.crawl(randomPhrase);
            int url_loop = 0;
            while ((url_loop < 5) && (url_loop < urls_temp.size())) {
                urls.add(urls_temp.get(url_loop));
                url_loop++;
            }
        } else if (language.equalsIgnoreCase("el")) {
           String bingAppId = c.getBingAppId();
            BingCrawler bc = new BingCrawler(bingAppId, language);
            urls_temp = bc.crawl(randomPhrase);
            int url_loop = 0;
            while ((url_loop < 5) && (url_loop < urls_temp.size())) {
                urls.add(urls_temp.get(url_loop));
                url_loop++;
            }
        } else if (language.equalsIgnoreCase("de")) {//keep only the first word of the random phrase for search
            if (randomPhrase.length() == 0) {
                randomPhrase = seed;
            }
            urls_temp = HTMLUtilities.linkExtractor("http://www.fragfinn.de/kinderliste/suche?start=0&query=" + randomPhrase.split(" ")[0], "UTF-8", 0);
            
            for (String url : urls_temp) {
                urls.add(StringEscapeUtils.unescapeHtml4(url));
                if (urls.size() == 5) {
                    break;
                }
            }
        }
        String delims = "[{} .,;?!():\"]+";

        String[] words = randomPhrase.split(",");
        String[] user_keywords = seed.split(delims);
        if (urls.size() > 0) {
            ExecutorService threadPool = Executors.newFixedThreadPool(urls.size());
            for (String url : urls) {
                threadPool.submit(new HTMLPages(url, pages, language)); //stopWordSet, tokensHashMap,language));
                // threadPool.submit(HTMLTokenizer());
            }
            threadPool.shutdown();
            while (!threadPool.isTerminated()) {

            }
            

            LinkedHashMap<ArrayList<String>, Double> temp = inf.TopTermsBing(pages, compactForm);
            HashMap<String, Double> temp2 = new HashMap<String, Double>();
            for (ArrayList<String> stems : temp.keySet()) {
                for (int j = 0; j < stems.size(); j++) {
                    String s = stems.get(j).split("\\{")[0];
                    s = s.replace(",", " ");
                    s = s.trim();

                    boolean wordnet = true;
                    //if term is not one of the initial random phrase
                    for (int i = 0; i < words.length; i++) {
                        if (s.equalsIgnoreCase(words[i])) {
                            wordnet = false;
                        }
                    }
                    //and if it 's not in the initial words of user
                    for (int i = 0; i < user_keywords.length; i++) {
                        if (s.equalsIgnoreCase(user_keywords[i])) {
                            wordnet = false;
                        }
                    }
                    //in german or greek, ignore english words from search english words
                    if (language.equalsIgnoreCase("de") || language.equalsIgnoreCase("el")) {
                        if (c.getWn().getCommonPos(s) != null) {
                            continue;
                        }
                    }
                    //return it with its stem's weight
                    if (wordnet) {
                        //for every stem, put each of its corresponding terms to tagCloud with the stem's tf
                        temp2.put(stems.get(j), temp.get(stems));
                    }
                }
            }
            TagCloud = inf.sortHashMapByValues(temp2);
            threadPool.shutdownNow();
        }
        String json = gson.toJson(TagCloud);
        c.CloseConnection();
        return json;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.cru.creativity.reasoning.semantic;

import com.google.gson.Gson;
import gr.demokritos.iit.cru.creativity.utilities.Connect;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 *
 * @author Giwrgos
 */
public class CloudOfThoughts {

    public static void main(String[] args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, Exception {
        System.out.println(CloudOfThoughts("Oral storytelling is perhaps the earliest method for sharing narratives. During most people's childhoods, narratives are used to guide them on proper behavior, cultural history, formation of a communal identity, and values, as especially studied in anthropology today among traditional indigenous peoples.[3] Narratives may also be nested within other narratives, such as narratives told by an unreliable narrator (a character) typically found in noir fiction genre.", false, "en"));
    }

    public static String CloudOfThoughts(String story, Boolean compactForm, String language) throws ClassNotFoundException, SQLException, IOException, InstantiationException, IllegalAccessException {
        Connect c = new Connect(language);
        InfoSummarization inf = new InfoSummarization(c);
        LinkedHashMap<ArrayList<String>, Double> temp = inf.TopTerms(story, compactForm);
        Gson gson = new Gson();
        String json = gson.toJson(temp);
        c.CloseConnection();
        return json;
    }
}

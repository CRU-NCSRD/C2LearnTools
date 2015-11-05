/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.cru.creativity.reasoning.semantic;

import gr.demokritos.iit.cru.creativity.utilities.Connect;
import gr.demokritos.iit.cru.creativity.utilities.WNAccess;
import gr.demokritos.iit.cru.creativity.utilities.WNDE;
import gr.demokritos.iit.cru.creativity.utilities.WNEL;
import gr.demokritos.iit.cru.creativity.utilities.WordNetDeDistance;
import gr.demokritos.iit.cru.creativity.utilities.WordNetENDistance;
import gr.demokritos.iit.cru.creativity.utilities.WordNetElDistance;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import static java.lang.Math.ceil;
import static java.lang.Math.floor;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.clusterers.EM;
import weka.clusterers.HierarchicalClusterer;
import weka.clusterers.SimpleKMeans;
import weka.clusterers.XMeans;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 *
 * @author antonis
 */
public class KeyphraseClustering {

    private ArrayList<String> keys;
    private String language;
    private int clusters;
    private WordNetENDistance wd;
    private WordNetDeDistance wdde;
    private WordNetElDistance wdel;
    private Instances data;

    public KeyphraseClustering(ArrayList<String> k, int numberOfClust, String lang) throws ClassNotFoundException, SQLException{
        this.language = lang;
        Connect c = new Connect(this.language);
        this.wd = new WordNetENDistance();
        this.wdde = new WordNetDeDistance(c.getWnde());
        this.wdel = new WordNetElDistance(c.getWnel());

        this.keys = new ArrayList<String>();
        for (int i = 0; i < k.size(); i++) {
            String[] tokenLine = k.get(i).split(";");
            String key = tokenLine[0];
            this.keys.add(key);
        }
        Attribute words = new Attribute("words", (FastVector) null);
        FastVector fvWekaAttributes = new FastVector();
        fvWekaAttributes.addElement(words);
        this.data = new Instances("words", fvWekaAttributes, 0);

        double sum = 0.0;
        for (String s : this.keys) {
            //keep the sum of the semantic distance between all the words
            for (String p : this.keys) {
                if (!p.equalsIgnoreCase(s)) {
                    sum += c.getDistance(s, p);
                }
            }
            //create new instance for every key and add it to the data
            Instance inst = new Instance(1);
            inst.setValue(this.data.attribute(0), s);
            this.data.add(inst);
        }
        if (numberOfClust == 0) {
            //sum += keys.size();
            int numerator = (int) ceil(sum);
            int clust = (int) ceil(numerator / (double)this.keys.size()); //CHANGE
            this.clusters = clust;
        } else {
            this.clusters = numberOfClust;
        }
    }

    public ArrayList<String> getClusters() throws Exception {
        System.out.println("Clustering......");
        // int[] clusters_size = new int[clusters];
        HierarchicalClusterer cl = new HierarchicalClusterer();

        cl.setNumClusters(this.clusters);
        if (language.equals("en")) {
            cl.setDistanceFunction(wd);
        } else if (language.equals("de")) {
            cl.setDistanceFunction(wdde);
        } else if (language.equals("el")) {
            cl.setDistanceFunction(wdel);
        }
        cl.buildClusterer(data);
        ArrayList<String> clustersList = new ArrayList<String>();
        for (int i = 0; i < cl.numberOfClusters(); i++) {
            clustersList.add("");
        }

        for (int j = 0; j < data.numInstances(); j++) {
            String clusterLine = data.instance(j).stringValue(0);

            int clust = cl.clusterInstance(data.instance(j));
            clustersList.set(clust, clustersList.get(clust).concat(clusterLine + ";"));
        }
        return clustersList;
    }
}

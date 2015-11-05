/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.cru.creativity.utilities;

import java.sql.SQLException;
import java.util.Enumeration;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.apache.commons.lang3.StringUtils.getLevenshteinDistance;
import weka.core.DistanceFunction;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.neighboursearch.PerformanceStats;

/**
 *
 * @author antonis
 */
public class WordNetElDistance implements Callable, DistanceFunction {

    private WNEL wn;

    public WordNetElDistance(WNEL wnel) {
        this.wn = wnel;
    }

    @Override
    public Object call() throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setInstances(Instances i) {

    }

    @Override
    public Instances getInstances() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setAttributeIndices(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getAttributeIndices() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setInvertSelection(boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getInvertSelection() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double distance(Instance instnc, Instance instnc1) {
        String s1 = instnc.stringValue(0);
        String s2 = instnc1.stringValue(0);
        double lev = 0.25 * getLevenshteinDistance(s1, s2) / ((s1.length() + s2.length()) / 2);
        double sem = 0;
        sem = 0.75 * wn.getDistance(s1, s2);
        double d = sem + lev;
        return d;
    }

    @Override
    public double distance(Instance instnc, Instance instnc1, PerformanceStats ps) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double distance(Instance instnc, Instance instnc1, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double distance(Instance instnc, Instance instnc1, double d, PerformanceStats ps) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void postProcessDistances(double[] doubles) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void update(Instance instnc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Enumeration listOptions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setOptions(String[] strings) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String[] getOptions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void clean() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.cru.creativity.reasoning.semantic;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

/**
 *
 * @author PK
 */
public class HTMLUtilities {
    
    public static ArrayList linkExtractor(String location,String encoding,int engine){
       
        NodeClassFilter filter = new NodeClassFilter (LinkTag.class);
        Parser parser;
        NodeList list=null;
        
        try {
            parser = new Parser(location);
            parser.setEncoding(encoding);
            list = parser.extractAllNodesThatMatch (filter);
        } catch (ParserException ex) {
            Logger.getLogger(HTMLUtilities.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        ArrayList linklist = new ArrayList();
        if(engine==0){
            for (int i = 0; i < list.size (); i++){               
                String link=list.elementAt(i).toPlainTextString();
                if(!link.startsWith("http://")) continue;
                linklist.add(link);                
            }
            
        }else if(engine==1){
            for (int i = 0; i < list.size (); i++){
                LinkTag extracted = (LinkTag)list.elementAt(i);
                if(!extracted.isHTTPLikeLink()) continue;
                String extractedLink = extracted.extractLink().replaceAll("&", "&");
                extractedLink = extractedLink.trim();
                if(extractedLink.length() == 0) continue; 
                if(extractedLink.startsWith("#")) continue;
                if(extractedLink.matches("(?i)^javascript:.*"))continue;
                if(!extractedLink.startsWith("http://")) continue;
                linklist.add(extractedLink);                
            } 
            
        }
               
        return linklist;     
    }
    
    
    
}

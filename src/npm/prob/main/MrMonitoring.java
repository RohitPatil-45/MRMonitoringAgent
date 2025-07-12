/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package npm.prob.main;

import npm.prob.dao.MrSatelHscLog;
import npm.prob.dao.MrStateUpdate;

/**
 *
 * @author Kratos
 */
public class MrMonitoring {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        try {
            Thread t2 = null;
            t2 = new Thread(new NodeStatusLatencyMonitoring());
            t2.start();
        } catch (Exception e) {
            System.out.println("Exception NodeProbMonitoring:" + e);
        }
        
        try {
            Thread t2 = null;
            t2 = new Thread(new MrStateUpdate());
            t2.start();
        } catch (Exception e) {
            System.out.println("Exception MrStateUpdate:" + e);
        }
        
        
        try {
            Thread t2 = null;
            t2 = new Thread(new MrSatelHscLog());
            t2.start();
        } catch (Exception e) {
            System.out.println("Exception MrSatelHscLog:" + e);
        }
    }
    
}

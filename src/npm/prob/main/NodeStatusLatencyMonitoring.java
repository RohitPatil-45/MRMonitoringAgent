/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package npm.prob.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import npm.prob.dao.DatabaseHelper;
import npm.prob.model.MasterRadioModel;
import npm.prob.model.MrSatelHscModel;

import npm.prob.model.NodeStausModel;

//Features
//1) Hisotry of 5 paramter
//2) update live 5 parameter
//Threshol
//3) Threshold log
//4) Thrwshold update
//5) Event log- Threshol
//Status
//1)update status -client radio id(when change)
//2)Maintain Log -client radio status
//3) Event log- -client radio status
/**
 *
 * @author NPM
 */
public class NodeStatusLatencyMonitoring implements Runnable {

    public static HashMap<String, MasterRadioModel> mapNodeData2 = null;

    public static ArrayList<NodeStausModel> mrStatusUpdateList = null;
    public static ArrayList<NodeStausModel> mrStatusUpdateListTemp = null;
    public static ArrayList<MrSatelHscModel> mrSatelHscLogList = null;
    public static ArrayList<MrSatelHscModel> mrSatelHscLogListTemp = null;

    public static HashMap masterRadioStatusMap = null;

    public static HashMap masterRadioStateMap = null;

    public void run() {

        mrStatusUpdateList = new ArrayList<>();
        mrStatusUpdateListTemp = new ArrayList<>();

        mrSatelHscLogList = new ArrayList<>();
        mrSatelHscLogListTemp = new ArrayList<>();

        masterRadioStatusMap = new HashMap<String, String>();
        masterRadioStateMap = new HashMap<String, String>();
        
        DatabaseHelper helper = new DatabaseHelper();
        mapNodeData2 = helper.getNodeData();
        System.out.println(mapNodeData2.size() + ":NodeProbMonitoring:" + mapNodeData2);
        Iterator<Map.Entry<String, MasterRadioModel>> itr = mapNodeData2.entrySet().iterator();

        ExecutorService executor = null;
        Runnable worker = null;
        executor = Executors.newFixedThreadPool(mapNodeData2.size());

        while (itr.hasNext()) {
            try {

                Map.Entry<String, MasterRadioModel> entry = itr.next();
                MasterRadioModel mrmodel = entry.getValue();

                String hvid = mrmodel.getHvid();

                worker = null;
                worker = new MasterRadioPHPMon(mrmodel);
                executor.execute(worker);

            } catch (Exception e) {
                System.err.println("Exceptionn: " + e.getMessage());
            }
        }

//        while (itr.hasNext()) {
        //            try {
        //                Map.Entry<String, ClientRadioModel> entry = itr.next();
        //                // System.out.println("Key = " + entry.getKey()
        //                //         + ", Value = " + entry.getValue());
        //                m = m + 1;
        //                inner_list.add(entry.getKey());
        //                if (m % 1 == 0) {
        //                    outer_list.add(inner_list.toString());
        //                    inner_list.clear();
        //                }
        //            } catch (Exception e) {
        //                System.out.println("Exception:" + e);
        //            }
        //
        //        }
        //
        //        if ((inner_list.size()) != 0) {
        //            outer_list.add(inner_list);
        //        }
        //        System.out.println("outer list:" + outer_list);
        //        System.out.println("Thread size:" + outer_list.size());
        //        int pool_sizee5 = outer_list.size();
        //        System.out.println("Thread Pool Size " + pool_sizee5);
        //
        //        ExecutorService executor = null;
        //        Runnable worker = null;
        //        executor = null;
        //        executor = Executors.newFixedThreadPool(pool_sizee5);
        //        Iterator out_itr = outer_list.iterator();
        //        int thread_count = 0;
        //
        //        while (out_itr.hasNext()) {
        //            String a = out_itr.next().toString();
        //            String b = a.substring(1, a.length() - 1);
        //            List<String> myList = null;
        //            myList = new ArrayList<String>(Arrays.asList(b.split(",")));
        //            // System.out.println("list1:" + myList);
        //            try {
        //                thread_count++;
        //                System.out.println("Thread Count:" + thread_count);
        //                
        //                worker = null;
        //                worker = new NodeMon(myList);
        //                executor.execute(worker);
        //                Thread.sleep(500);
        //                //System.out.println(thread_count + "th Thread started ");
        //            } catch (Exception e) {
        //                System.err.println("Exceptionn: " + e.getMessage());
        //            }
        //
        //        }
    }

}

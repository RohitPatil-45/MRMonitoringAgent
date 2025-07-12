/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package npm.prob.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import npm.prob.datasource.Datasource;
import npm.prob.main.NodeStatusLatencyMonitoring;

/**
 *
 * @author Kratos
 */
public class MrSatelHscLog implements Runnable{
    Connection connection = null;
    PreparedStatement preparedStatement = null;
    String sql = null;

    //  PreparedStatement preparedStatement2 = null;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void run() {

        
        System.out.println("Start MrSatelHscLog Log");
        while (true) {

            LocalDateTime currentDateTime1 = LocalDateTime.now();
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@(info) MrSatelHscLog 1:" + currentDateTime1.format(formatter));

            int count5 = 0;
            try {
                Thread.sleep(12000);
            } catch (Exception e) {
                //System.out.println("Thread Sleep Exception " + e);
            }
            try {
                NodeStatusLatencyMonitoring.mrSatelHscLogListTemp.clear();
                NodeStatusLatencyMonitoring.mrSatelHscLogListTemp.addAll(NodeStatusLatencyMonitoring.mrSatelHscLogList);
                NodeStatusLatencyMonitoring.mrSatelHscLogList.clear();
                // System.out.println("batch latency insert=" + BranchICMPPacket.latency_list_temp.size());
            } catch (Exception e) {
                System.out.println("Exception in batch insert=" + e);
            }
            LocalDateTime currentDateTime2 = LocalDateTime.now();
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@(info) mrSatelHscLogList 2:" + currentDateTime2.format(formatter));

            try {
                connection = Datasource.getConnection();
               
                sql = "INSERT INTO mr_satel_hsc_100 (brainbox_IP, hvid, timestamp, status) VALUES (?,?,?,?)";
                
                preparedStatement = connection.prepareStatement(sql);
               
                LocalDateTime currentDateTime3 = LocalDateTime.now();
                System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@(info) mrSatelHscLogList 3:" + currentDateTime3.format(formatter));
                connection.setAutoCommit(false);
                for (int i = 0; i < NodeStatusLatencyMonitoring.mrSatelHscLogListTemp.size(); i++) {
                    count5 = count5 + 1;
                    try {

                        preparedStatement.setString(1, NodeStatusLatencyMonitoring.mrSatelHscLogListTemp.get(i).getBrainbox_IP());
                        preparedStatement.setString(2, NodeStatusLatencyMonitoring.mrSatelHscLogListTemp.get(i).getHvid());
                        preparedStatement.setTimestamp(3, NodeStatusLatencyMonitoring.mrSatelHscLogListTemp.get(i).getTimestamp());
                        preparedStatement.setString(4, NodeStatusLatencyMonitoring.mrSatelHscLogListTemp.get(i).getStatus());
                        
                        preparedStatement.addBatch();
                     
                        if (count5 % 1000 == 0) {
                            //  System.out.println(count5 + "match count5:");
                            LocalDateTime currentDateTime4 = LocalDateTime.now();
                            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@(info) mrSatelHscLogList 4:" + currentDateTime4.format(formatter));

                            preparedStatement.executeBatch();
                            //System.out.println(count5 + "Insert Branch COunt:" + count.length);
                            LocalDateTime currentDateTime5 = LocalDateTime.now();
                            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@(info) mrSatelHscLogList 5:" + currentDateTime5.format(formatter));
                            preparedStatement = null;
                            preparedStatement = connection.prepareStatement(sql);
                        }
                    } catch (Exception e) {
                        System.out.println("Exception in insert mrSatelHscLogList log=" + e);
                    }
                }
                preparedStatement.executeBatch();
                connection.commit();
                //System.out.println("@@Insert device_status_latency_history count:" + count.length);
                LocalDateTime currentDateTime6 = LocalDateTime.now();
                System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@(info) mrSatelHscLogList 6:" + currentDateTime6.format(formatter));

//                int[] count2 = preparedStatement2.executeBatch();
//                System.out.println("#Insert status Latency history:" + count2.length);
            } catch (Exception exp) {
                System.out.println("Exception Batch mrSatelHscLogList:" + exp);
            } finally {

                try {
                    if (preparedStatement != null) {
                        preparedStatement.close();
                    }
//                     if (preparedStatement2 != null) {
//                        preparedStatement2.close();
//                    }
                    if (connection != null) {
                        connection.close();
                    }

                } catch (Exception ep) {
                    System.out.println("*&&&&&&&&" + ep);
                }
            }

        }

    }
}

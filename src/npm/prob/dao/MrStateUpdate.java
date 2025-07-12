/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package npm.prob.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import npm.prob.datasource.Datasource;
import npm.prob.main.NodeStatusLatencyMonitoring;

/**
 *
 * @author Kratos
 */
public class MrStateUpdate implements Runnable{
    
    Connection connection = null;
    PreparedStatement preparedStatement = null;
    PreparedStatement preparedStatement2 = null;
    ResultSet resultSet = null;
    String sql = null;

    public void run() {
        //System.out.println("Start LatencyUpdate");
        System.out.println("Start mr_status_standby_main update");
        while (true) {
            try {
                Thread.sleep(4000);
            } catch (Exception expp) {
                System.out.println("Exception In Thread Sleep BatchUpdate :" + expp);
            }

            try {
                NodeStatusLatencyMonitoring.mrStatusUpdateListTemp.clear();
                NodeStatusLatencyMonitoring.mrStatusUpdateListTemp.addAll(NodeStatusLatencyMonitoring.mrStatusUpdateList);
                NodeStatusLatencyMonitoring.mrStatusUpdateList.clear();
            } catch (Exception e) {
                System.out.println("Excpetion in try catch mr_status_standby_main=" + e);
            }

            try {
                connection = Datasource.getConnection();

                sql = "UPDATE mr_status_standby_main SET status=?,EventTimestamp=? WHERE Hvid=?";
                preparedStatement = connection.prepareStatement(sql);
                for (int i = 0; i < NodeStatusLatencyMonitoring.mrStatusUpdateListTemp.size(); i++) {
                    try {
                        preparedStatement.setString(1, NodeStatusLatencyMonitoring.mrStatusUpdateListTemp.get(i).getState());
                        preparedStatement.setTimestamp(2, NodeStatusLatencyMonitoring.mrStatusUpdateListTemp.get(i).getEventTime());
                        preparedStatement.setString(3, NodeStatusLatencyMonitoring.mrStatusUpdateListTemp.get(i).getHvid());
                       
                        preparedStatement.addBatch();
                      //  System.out.println("updated IP :" + NodeStatusLatencyMonitoring.latency_update_temp.get(i).getDevice_ip());
                       // System.out.println("updated values  :" + NodeStatusLatencyMonitoring.latency_update_temp.get(i).getAvg_response());
                       // if (i == 1000) {
                            int[] count = preparedStatement.executeBatch();

                            System.out.println("UPDATE mr_status_standby_main inside: " + count.length);
                            preparedStatement = null;
                            preparedStatement = connection.prepareStatement(sql);
                       // }

                        //System.out.println(dateFormat.format(in_startdate));
                    } catch (Exception e) {
                        System.out.println("Exception in mr_status_standby_main=" + e);
                    }
                }
                int[] count = preparedStatement.executeBatch();

               System.out.println("##UPDATE mr_status_standby_main parameters Count: " + count.length);
            } catch (Exception exp) {
                System.out.println("--$$$$$Exception In Batch Update " + exp);
            } finally {
                try {

                    if (preparedStatement != null) {
                        preparedStatement.close();
                    }
                    if (connection != null) {
                        connection.close();
                    }

                } catch (Exception ep) {
                    System.out.println("Exception1111Insweertr in update==== " + ep);
                }
            }

        }

    }
    
}

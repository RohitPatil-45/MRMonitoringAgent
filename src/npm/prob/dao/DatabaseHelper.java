/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package npm.prob.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;

import npm.prob.datasource.Datasource;
import npm.prob.main.NodeStatusLatencyMonitoring;
import static npm.prob.main.NodeStatusLatencyMonitoring.masterRadioStatusMap;
import npm.prob.model.MasterRadioModel;

/**
 *
 * @author NPM
 */
public class DatabaseHelper {

    public static void main(String[] args) {
        DatabaseHelper helper = new DatabaseHelper();
        helper.getNodeData();
    }

    public HashMap<String, MasterRadioModel> getNodeData() {
        Connection connection = null;
        Statement st1 = null;
        ResultSet rs = null;

        Statement st2 = null;
        ResultSet rs2 = null;

        PreparedStatement preparedStatement = null;
        PreparedStatement pstInsert = null;

        HashMap<String, MasterRadioModel> mapNodeData = new HashMap();

        try {
            connection = Datasource.getConnection();
            st1 = connection.createStatement();

            // String query = "select node.DEVICE_IP,node.DEVICE_NAME,node.DEVICE_TYPE,node.GROUP_NAME,node.COMPANY,node.LOCATION,node.DISTRICT,node.STATE,node.ZONE,parm.LATENCY_HISTORY,parm.LATENCY_THRESHOLD,mon.NODE_STATUS FROM ADD_NODE node JOIN NODE_PARAMETER parm ON node.DEVICE_IP=parm.DEVICE_IP JOIN node_monitoring mon ON node.DEVICE_IP=mon.NODE_IP  WHERE parm.MONITORING='yes' ORDER BY node.ID ";
            // String query = "select node.DEVICE_IP,node.DEVICE_NAME,node.DEVICE_TYPE,node.GROUP_NAME,node.COMPANY,node.LOCATION,node.DISTRICT,node.STATE,node.ZONE,parm.LATENCY_HISTORY,parm.LATENCY_THRESHOLD,mon.NODE_STATUS FROM ADD_NODE node JOIN NODE_PARAMETER parm ON node.DEVICE_IP=parm.DEVICE_IP JOIN node_monitoring mon ON node.DEVICE_IP=mon.NODE_IP  WHERE parm.MONITORING='yes' ORDER BY node.ID ";
            String query = "SELECT hvid, hvnamn, hvnamn2, hvmanagementadr, hvservicemode, hvhostnamn, hvserienr FROM netadmin.hv WHERE hvfab = 23 and hvid = '19036'";

            rs = st1.executeQuery(query);
            while (rs.next()) {
                //NodeMasterModel node = new NodeMasterModel();
                MasterRadioModel hv = new MasterRadioModel();

                String hvid = rs.getString(1);

                hv.setHvid(rs.getString(1));
                hv.setHvnamn(rs.getString(2));
                hv.setHvnamn2(rs.getString(3));
                hv.setHvmanagementadr(rs.getString(4));
                hv.setHvservicemode(rs.getString(5));
                hv.setHvhostnamn(rs.getString(6));
                hv.setHvserienr(rs.getString(7));

                mapNodeData.put(hvid, hv);

                try {

                    String queryText = "SELECT Hvid, status, current_status FROM mr_status_standby_main WHERE hvid = ?";
                    preparedStatement = connection.prepareStatement(queryText);
                    preparedStatement.setString(1, hvid);
                    rs2 = preparedStatement.executeQuery();
                    while (rs2.next()) {
                        NodeStatusLatencyMonitoring.masterRadioStateMap.put(rs2.getString(1), rs2.getString(2));
                        NodeStatusLatencyMonitoring.masterRadioStatusMap.put(hvid, rs2.getString(3));
                    }
//                    if(!rs2.next()) {
//
//                        String query2 = "insert into mr_status_standby_main (Hvid, status, EventTimestamp) values (?,?,?)";
//                        pstInsert = connection.prepareStatement(query2);
//                        pstInsert.setString(1, hvid);
//                        pstInsert.setString(2, "1");
//                        pstInsert.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
//                        pstInsert.executeUpdate();
//
//                    }

                } catch (Exception e) {
                    System.out.println("Exception in insertion in mr_status_standby_main:" + e);
                } finally {
                    if (preparedStatement != null) {
                        try {
                            preparedStatement.close();
                        } catch (SQLException e) {
                            //System.out.println(e.getMessage());
                        }
                    }
                    if (rs2 != null) {
                        try {
                            rs2.close();
                        } catch (SQLException e) {
                            //System.out.println(e.getMessage());
                        }
                    }
                    if (pstInsert != null) {
                        try {
                            pstInsert.close();
                        } catch (SQLException e) {
                            //System.out.println(e.getMessage());
                        }
                    }

                }

            }

        } catch (Exception ex) {
            System.out.println("Exception node read:" + ex);
        } finally {
            if (st1 != null) {
                try {
                    st1.close();
                } catch (SQLException e) {
                    //System.out.println(e.getMessage());
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    //System.out.println(e.getMessage());
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    //System.out.println(e.getMessage());
                }
            }
        }

        System.out.println("Node Map Data:" + mapNodeData);

        //System.out.println("StatusMap:" + NodeStatusLatencyMonitoring.deviceStatusMap);
        return mapNodeData;
    }

    public void insertIntoEventLog(String deviceID, String deviceName, String eventMsg, int severity, String serviceName, Timestamp evenTimestamp, String netadmin_msg, String isAffected, String problem, String serviceId, String deviceType) {
        PreparedStatement preparedStatement1 = null;
        PreparedStatement preparedStatement2 = null;
        Connection connection = null;
        try {
            connection = Datasource.getConnection();
            preparedStatement1 = connection.prepareStatement("INSERT INTO event_log (device_id, device_name, service_name, event_msg, netadmin_msg, severity,"
                    + " event_timestamp, acknowledgement_status, isAffected, Problem_Clear, Service_ID, Device_Type) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
            preparedStatement1.setString(1, deviceID);
            preparedStatement1.setString(2, deviceName);
            preparedStatement1.setString(3, serviceName);
            preparedStatement1.setString(4, eventMsg);
            preparedStatement1.setString(5, netadmin_msg);
            preparedStatement1.setInt(6, severity);
            preparedStatement1.setTimestamp(7, evenTimestamp);
            preparedStatement1.setBoolean(8, false);
            preparedStatement1.setString(9, isAffected);
            preparedStatement1.setString(10, problem);
            preparedStatement1.setString(11, serviceId);
            preparedStatement1.setString(12, deviceType);
            preparedStatement1.executeUpdate();

        } catch (Exception e) {
            System.out.println(deviceID + "inserting in Mars_radio_health_history Exception:" + e);
        } finally {
            try {
                if (preparedStatement1 != null) {
                    preparedStatement1.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (Exception exp) {
                System.out.println("excep:" + exp);
            }
        }

        try {
            if ("Cleared".equalsIgnoreCase(problem)) {

                String updateQuery = "UPDATE event_log\n"
                        + "SET\n"
                        + "    Cleared_event_timestamp = ?,\n"
                        // + "    netadmin_msg = ?,\n"
                        + "netadmin_msg = CONCAT(netadmin_msg, ' => ', ?),\n"
                        + "    isAffected = ?\n"
                        + "WHERE\n"
                        + "    ID = (\n"
                        + "        SELECT id_alias.ID\n"
                        + "        FROM (\n"
                        + "            SELECT ID\n"
                        + "            FROM event_log\n"
                        + "            WHERE service_id = ?\n"
                        + "              AND device_id = ?\n"
                        + "            AND isaffected = '1' ORDER BY ID DESC\n"
                        + "            LIMIT 1\n"
                        + "        ) AS id_alias\n"
                        + "    )\n"
                        + ";";

                connection = Datasource.getConnection();

                preparedStatement2 = connection.prepareStatement(updateQuery);
                preparedStatement2.setTimestamp(1, evenTimestamp);

                preparedStatement2.setString(2, netadmin_msg); // To Do
                preparedStatement2.setString(3, "0");
                preparedStatement2.setString(4, serviceId);
                preparedStatement2.setString(5, deviceID);

                preparedStatement2.executeUpdate();
            }
        } catch (Exception e) {
            System.out.println("Exception in update mr = " + e);
        } finally {
            try {
                if (preparedStatement2 != null) {
                    preparedStatement2.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (Exception exp) {
                System.out.println("excep:" + exp);
            }
        }

    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package npm.prob.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;
import npm.prob.dao.DatabaseHelper;
import npm.prob.datasource.Datasource;
import npm.prob.model.MasterRadioModel;
import npm.prob.model.MrSatelHscModel;
import npm.prob.model.NodeStausModel;
import org.json.JSONObject;

/**
 *
 * @author Kratos
 */
public class MasterRadioPHPMon implements Runnable {

    MasterRadioModel radio = null;
    DatabaseHelper db = new DatabaseHelper();

    MasterRadioPHPMon(MasterRadioModel m) {
        this.radio = m;
    }

    @Override
    public void run() {
        boolean simulation = true;
        while (true) {

            String hvid = radio.getHvid();
            String deviceID = radio.getHvmanagementadr() + "_" + radio.getHvnamn2();
            String deviceName = radio.getHvnamn();
            String isAffected = "0";
            String problem = "problem";
            String device_ip = hvid;
            String eventMsg1 = "";
            String netadminMsg = "";

            StringBuilder outputBuilder = new StringBuilder();

            ProcessBuilder builder = null;

            if (simulation) {
                builder = new ProcessBuilder("php", "C:Simulation\\MRMonitoring.php", radio.getHvserienr());
            } else {
                builder = new ProcessBuilder("php", "C:Canaris\\MasterRadio\\MRMonitoring.php", radio.getHvserienr());
            }

            Timestamp event_time = new Timestamp(System.currentTimeMillis());

            try {

                Process process = builder.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    System.out.println("output json = " + output.toString());
                    JSONObject json = new JSONObject(output.toString());

                    int modemState = 0;
                    String router_status = "";

                    try {

                        modemState = json.getInt("modemState");
                        router_status = json.getString("modemStatus");

                        System.out.println("modem State for : " + radio.getHvnamn() + " is " + modemState);
                        System.out.println("modem Status for : " + radio.getHvnamn() + " is " + router_status);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Exption Output_JSon :" + e);
                        modemState = 0;
                        router_status = "Down";
                    }
                    try {
                        //Master radio main standy status update

                        NodeStausModel mr = new NodeStausModel();
                        mr.setHvid(hvid);
                        mr.setState(String.valueOf(modemState));
                        mr.setEventTime(event_time);
                        NodeStatusLatencyMonitoring.mrStatusUpdateList.add(mr);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //MR State HSC Log
                    try {
                        String modemOldState = NodeStatusLatencyMonitoring.masterRadioStateMap.get(hvid).toString();
                        System.out.println("old modem state for hvid : " + hvid + " = " + modemOldState);
                        if (Integer.valueOf(modemOldState) != modemState) {
                            MrSatelHscModel mod = new MrSatelHscModel();
                            mod.setBrainbox_IP(radio.getHvmanagementadr());
                            mod.setHvid(hvid);
                            mod.setStatus(String.valueOf(modemState));
                            mod.setTimestamp(event_time);
                            NodeStatusLatencyMonitoring.mrSatelHscLogList.add(mod);
                            NodeStatusLatencyMonitoring.masterRadioStateMap.put(hvid, modemState);

                            eventMsg1 = String.valueOf(modemState).equalsIgnoreCase("2") ? "master radio : " + deviceName + " change from Main to Standby" : "master radio : " + deviceName + " change in Main Mode";
                            netadminMsg = String.valueOf(modemState).equalsIgnoreCase("2") ? "master radio : " + deviceName + " change from Main to Standby" : "master radio : " + deviceName + " change in Main Mode";

                            isAffected = String.valueOf(modemState).equalsIgnoreCase("2") ? "1" : "0";
                            problem = String.valueOf(modemState).equalsIgnoreCase("1") ? "1" : "0";
                            db.insertIntoEventLog(deviceID, deviceName, eventMsg1, 0, "PING ICMP", event_time, netadminMsg, isAffected, problem);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //Status master radio up down monitoring
                    String router_status_xml = NodeStatusLatencyMonitoring.masterRadioStatusMap.get(hvid).toString();

//                    System.out.println("router_status_xml:" + router_status_xml);
                    if (router_status == null || router_status_xml == null || router_status.equals(router_status_xml)) {
                        //  //System.out.println("********************Not Change Router Status****************");
                    } else if (router_status_xml.equals("Up") && router_status.equals("Down")) {
                        System.out.println("1st down:" + device_ip);
                        NodeStatusLatencyMonitoring.masterRadioStatusMap.put(device_ip, "Down1");

                    } else if (router_status_xml.equals("Down1") && router_status.equals("Down")) {
                        System.out.println("up to warrning:" + device_ip);
                        NodeStatusLatencyMonitoring.masterRadioStatusMap.put(device_ip, "Down2");
                        //updateDeviceStatus(device_ip, "Warning", event_time);
//                                try {
//                                    Thread.sleep(2000);
//                                } catch (Exception e) {
//                                    //System.out.println("e:" + e);
//                                }
                    } else if (router_status_xml.equals("Down2") && router_status.equals("Down")) {
                        System.out.println("@@$$Down Device:" + device_ip);

                        NodeStatusLatencyMonitoring.masterRadioStatusMap.put(device_ip, "Down3");
                        updateDeviceStatus(device_ip, "Down", event_time);
                        deviceStatusLog(device_ip, "Down", event_time);

                        //TO DO: insert into event Log
                        eventMsg1 = "master radio : " + deviceName + " is Down";
                        netadminMsg = "master radio : " + deviceName + " - Got no reply";
                        isAffected = "1";

                        db.insertIntoEventLog(deviceID, deviceName, eventMsg1, 4, "PING ICMP", event_time, netadminMsg, isAffected, problem);

                    } else if (router_status_xml.equals("Down3") && router_status.equals("Down")) {
                        //    //System.out.println("%%%%%..Skip Down condition ");
                    } else if (router_status_xml.equals("Down3") && router_status.equals("Up")) {
                        System.out.println("Down to Up");
                        NodeStatusLatencyMonitoring.masterRadioStatusMap.put(device_ip, "Up");
                        updateDeviceStatus(device_ip, "Up", event_time);
                        deviceStatusLog(device_ip, "Up", event_time);
                        eventMsg1 = "master radio : " + deviceName + " is Up";
                        netadminMsg = "master radio : " + deviceName + "pl - 0";
                        isAffected = "0";
                        problem = "Cleared";
                        db.insertIntoEventLog(deviceID, deviceName, eventMsg1, 0, "PING ICMP", event_time, netadminMsg, isAffected, problem);
//                            try {
//                                StatusChangeDiff t22 = null;
//                                t22 = new StatusChangeDiff();
//                                t22.insertStatusDiff(device_ip, event_time);
//                            } catch (Exception e) {
//                                System.out.println("Uptime Thread Exception:" + e);
//                            }

                    } else if (router_status_xml.equals("Down1") && router_status.equals("Up")) {
                        System.out.println("1st down then Up:" + device_ip);
                        NodeStatusLatencyMonitoring.masterRadioStatusMap.put(device_ip, "Up");
                        updateDeviceStatus(device_ip, "Up", event_time);

                    } else if (router_status_xml.equals("Down2") && router_status.equals("Up")) {
                        System.out.println("2nd down Warning then Up:" + device_ip);;
                        NodeStatusLatencyMonitoring.masterRadioStatusMap.put(device_ip, "Up");
                        updateDeviceStatus(device_ip, "Up", event_time);

                    } else if (router_status_xml.equals("Down") && router_status.equals("Up")) {

                        NodeStatusLatencyMonitoring.masterRadioStatusMap.put(device_ip, "Up");
                        updateDeviceStatus(device_ip, "Up", event_time);
                        deviceStatusLog(device_ip, "Up", event_time);
                        eventMsg1 = "master radio : " + deviceName + " is up";
                        netadminMsg = "master radio : " + deviceName + "pl - 0";
                        isAffected = "0";
                        problem = "Cleared";
                        db.insertIntoEventLog(deviceID, deviceName, eventMsg1, 0, "PING ICMP", event_time, netadminMsg, isAffected, problem);

//                            try {
//                                StatusChangeDiff t22 = null;
//                                t22 = new StatusChangeDiff();
//                                t22.insertStatusDiff(device_ip, event_time);
//                            } catch (Exception e) {
//                                System.out.println("Uptime Thread Exception:" + e);
//                            }
                    } else if (router_status_xml.equals("Warning") && router_status.equals("Up")) {
                        NodeStatusLatencyMonitoring.masterRadioStatusMap.put(device_ip, "Up");
                        updateDeviceStatus(device_ip, "Up", event_time);
                        System.out.println("1st down then Up:" + device_ip);
                    } else if (router_status_xml.equals("Warning") && router_status.equals("Down")) {
                        NodeStatusLatencyMonitoring.masterRadioStatusMap.put(device_ip, "Down");
                        updateDeviceStatus(device_ip, "Down", event_time);
                    } else {
                        //System.out.println(router_ipadress + "Else Condition*********************************** old:" + router_status_xml + ":New:" + router_status);
                    }
                    //status Monmmonitoring end

                } else {
                    System.err.println("PHP script failed.");
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

            try {
//                Thread.sleep(60000);
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                Logger.getLogger(MasterRadioPHPMon.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    public void updateDeviceStatus(String device_ip, String device_status, Timestamp eventTime) {
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = Datasource.getConnection();
            pst = con.prepareStatement("UPDATE mr_status_standby_main SET current_status=? WHERE Hvid=?");
            pst.setString(1, device_status);
            pst.setString(2, device_ip);

            pst.executeUpdate();
        } catch (Exception e) {
            System.out.println("UPDATE mr_status_standby_main exception normal:" + e);
        } finally {
            try {
                if (pst != null) {
                    pst.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (Exception exp) {
                System.out.println("update mr_status_standby_main log exp:" + exp);
            }
        }
    }

    public void deviceStatusLog(String device_ip, String device_status, Timestamp eventTime) {
        Connection con = null;
        PreparedStatement pst = null;
        try {
            con = Datasource.getConnection();
            pst = con.prepareStatement("insert into MR_Status_report (Hvid,Status,EventTime) "
                    + "values (?,?,?)");
            pst.setString(1, device_ip);
            pst.setString(2, device_status);
            pst.setTimestamp(3, eventTime);

            pst.executeUpdate();
        } catch (Exception e) {
            System.out.println("insert MR_Status_report exception normal:" + e);
        } finally {
            try {
                if (pst != null) {
                    pst.close();
                }
                if (con != null) {
                    con.close();
                }
            } catch (Exception exp) {
                System.out.println("insert mars_status_report log exp:" + exp);
            }
        }
    }
}

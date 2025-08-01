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
import npm.prob.model.EventLog;
import npm.prob.model.MasterRadioModel;
import npm.prob.model.MrSatelHscModel;
import npm.prob.model.NodeStausModel;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 *
 * @author Kratos
 */
public class MasterRadioPHPMon implements Runnable {

    MasterRadioModel radio = null;
    DatabaseHelper db = new DatabaseHelper();
    boolean simulation = true;
    String deviceType = "MASTER_RADIO";

    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000;

    private final ObjectMapper mapper = new ObjectMapper();

    MasterRadioPHPMon(MasterRadioModel m) {
        this.radio = m;
    }

    @Override
    public void run() {

        while (true) {

            String hvid = radio.getHvid();
            String deviceID = radio.getHvserienr();
            String deviceName = radio.getHvnamn();
            String isAffected = "0";
            String problem = "problem";
            String device_ip = hvid;
            String eventMsg1 = "";
            String netadminMsg = "";
            String serviceId = "";

            StringBuilder outputBuilder = new StringBuilder();

            ProcessBuilder builder = null;

            if (simulation) {
                builder = new ProcessBuilder("php", "C:Simulation\\MR\\" + deviceName + ".php", radio.getHvserienr());
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
                            netadminMsg = String.valueOf(modemState).equalsIgnoreCase("2") ? "status = " + modemState + "Active modem is STANDBY" : "status = " + modemState + " Active modem is MAIN";

                            isAffected = String.valueOf(modemState).equalsIgnoreCase("2") ? "1" : "0";
                            problem = String.valueOf(modemState).equalsIgnoreCase("1") ? "Cleared" : "Problem";
                            serviceId = "mr_state";
                            //db.insertIntoEventLog(deviceID, deviceName, eventMsg1, 0, "Satel HSC100 monitoring", event_time, netadminMsg, isAffected, problem, serviceId, deviceType);
                            sendEventLogToApi(deviceID, deviceName, eventMsg1, 0, "Satel HSC100 monitoring", event_time, netadminMsg, isAffected, problem, serviceId, deviceType, 0);
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
                        netadminMsg = eventMsg1;
                        isAffected = "1";
                        serviceId = "mr_status";
                        //db.insertIntoEventLog(deviceID, deviceName, eventMsg1, 4, "MR Status", event_time, netadminMsg, isAffected, problem, serviceId, deviceType);
                        sendEventLogToApi(deviceID, deviceName, eventMsg1, 4, "MR Status", event_time, netadminMsg, isAffected, problem, serviceId, deviceType,0);

                    } else if (router_status_xml.equals("Down3") && router_status.equals("Down")) {
                        //    //System.out.println("%%%%%..Skip Down condition ");
                    } else if (router_status_xml.equals("Down3") && router_status.equals("Up")) {
                        System.out.println("Down to Up");
                        NodeStatusLatencyMonitoring.masterRadioStatusMap.put(device_ip, "Up");
                        updateDeviceStatus(device_ip, "Up", event_time);
                        deviceStatusLog(device_ip, "Up", event_time);
                        eventMsg1 = "master radio : " + deviceName + " is Up";
                        netadminMsg = eventMsg1;
                        isAffected = "0";
                        problem = "Cleared";
                        serviceId = "mr_status";
//                        db.insertIntoEventLog(deviceID, deviceName, eventMsg1, 0, "MR Status", event_time, netadminMsg, isAffected, problem, serviceId, deviceType);
                        sendEventLogToApi(deviceID, deviceName, eventMsg1, 0, "MR Status", event_time, netadminMsg, isAffected, problem, serviceId, deviceType,0);
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
                        netadminMsg = eventMsg1;
                        isAffected = "0";
                        problem = "Cleared";
                        serviceId = "mr_status";
//                        db.insertIntoEventLog(deviceID, deviceName, eventMsg1, 0, "MR Status", event_time, netadminMsg, isAffected, problem, serviceId, deviceType);
                        sendEventLogToApi(deviceID, deviceName, eventMsg1, 0, "MR Status", event_time, netadminMsg, isAffected, problem, serviceId, deviceType,0);

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
            pst = con.prepareStatement("UPDATE mr_status_standby_main SET current_status=?, EventTimestamp=?, current_status_Generated_Time=?, current_status_Cleared_Time=? WHERE Hvid=?");
            pst.setString(1, device_status);
            pst.setTimestamp(2, eventTime);
            pst.setTimestamp(3, device_status.equalsIgnoreCase("Down") ? eventTime : null);
            pst.setTimestamp(4, device_status.equalsIgnoreCase("Up") ? eventTime : null);
            pst.setString(5, device_ip);

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

    public void sendEventLogToApi(String deviceID, String deviceName, String eventMsg, int severity, String serviceName, Timestamp evenTimestamp,
            String netadmin_msg, String isAffected, String problem, String serviceId, String deviceType, int attempt) {
        EventLog log = new EventLog();
        log.setDeviceId(deviceID);
        log.setDeviceName(deviceName);
        log.setEventMsg(eventMsg);
        log.setSeverity(String.valueOf(severity));
        log.setServiceName(serviceName);
        log.setEventTimestamp(evenTimestamp);
        log.setNetadminMsg(netadmin_msg);
        log.setIsaffected(Integer.valueOf(isAffected));
        log.setProblemClear(problem);
        log.setServiceID(serviceId);
        log.setDeviceType(deviceType);
        
        System.out.println("service id = "+serviceId);
        System.out.println("sAffected = "+isAffected);

        CloseableHttpClient httpClient = HttpClients.createDefault();
        ObjectMapper mapper = new ObjectMapper();

        try {
            String json = mapper.writeValueAsString(log);
            HttpPost request = new HttpPost("http://localhost:8083/api/event/log"); // adjust host/port
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(json));

            CloseableHttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode >= 200 && statusCode < 300) {
                System.out.println("Log sent successfully: " + statusCode);
            } else {
                System.err.println("Failed to send log, status: " + statusCode);
                retryIfNeeded(log, attempt);
            }

            response.close();
        } catch (IOException e) {
            System.err.println("Exception while sending log: " + e.getMessage());
            retryIfNeeded(log, attempt);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void retryIfNeeded(EventLog log, int attempt) {
        if (attempt < MAX_RETRIES) {
            System.out.println("Retrying sendEventLogToApi... Attempt " + (attempt + 1));
            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); // Preserve interrupt status
                return;
            }

            // Retry the API call with incremented attempt count
            sendEventLogToApi(
                    log.getDeviceId(),
                    log.getDeviceName(),
                    log.getEventMsg(),
                    Integer.valueOf(log.getSeverity()),
                    log.getServiceName(),
                    log.getEventTimestamp(),
                    log.getNetadminMsg(),
                    log.getIsaffected().toString(),
                    log.getProblemClear(),
                    log.getServiceID(),
                    log.getDeviceType(),
                    attempt + 1
            );
        } else {
            System.err.println("Max retries reached. Dropping event log.");
        }
    }

}

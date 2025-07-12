package npm.prob.model;

import java.sql.Timestamp;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author VIVEK
 */
public class NodeStausModel {

    private String hvid;

    private String state;
    
    private Timestamp eventTime;

    public String getHvid() {
        return hvid;
    }

    public void setHvid(String hvid) {
        this.hvid = hvid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }


    public Timestamp getEventTime() {
        return eventTime;
    }

    public void setEventTime(Timestamp eventTime) {
        this.eventTime = eventTime;
    }
    
    

}

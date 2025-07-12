package npm.prob.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class MrSatelHscModel implements Serializable {

    private static final long serialVersionUID = -2264642949863409860L;

    private String hvid;
    private String brainbox_IP;
    private String status;
    private Timestamp timestamp;

    public String getHvid() {
        return hvid;
    }

    public void setHvid(String hvid) {
        this.hvid = hvid;
    }

    public String getBrainbox_IP() {
        return brainbox_IP;
    }

    public void setBrainbox_IP(String brainbox_IP) {
        this.brainbox_IP = brainbox_IP;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
    
    
    

}

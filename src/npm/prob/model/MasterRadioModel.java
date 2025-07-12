/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package npm.prob.model;

import java.io.Serializable;

/**
 *
 * @author Kratos
 */
public class MasterRadioModel implements Serializable{
    
     private static final long serialVersionUID = -2264642949863409860L;
     
     private String hvid;
     
     private String hvnamn;
     
     private String hvnamn2;
     
     private String hvmanagementadr;
     
     private String hvservicemode;
     
     private String hvhostnamn;
     
      private String hvserienr;

    public String getHvid() {
        return hvid;
    }

    public void setHvid(String hvid) {
        this.hvid = hvid;
    }

    public String getHvnamn() {
        return hvnamn;
    }

    public void setHvnamn(String hvnamn) {
        this.hvnamn = hvnamn;
    }

    public String getHvnamn2() {
        return hvnamn2;
    }

    public void setHvnamn2(String hvnamn2) {
        this.hvnamn2 = hvnamn2;
    }

    public String getHvservicemode() {
        return hvservicemode;
    }
   

    public void setHvservicemode(String hvservicemode) {
        this.hvservicemode = hvservicemode;
    }

    public String getHvmanagementadr() {
        return hvmanagementadr;
    }

    public void setHvmanagementadr(String hvmanagementadr) {
        this.hvmanagementadr = hvmanagementadr;
    }

    public String getHvhostnamn() {
        return hvhostnamn;
    }

    public void setHvhostnamn(String hvhostnamn) {
        this.hvhostnamn = hvhostnamn;
    }

    public String getHvserienr() {
        return hvserienr;
    }

    public void setHvserienr(String hvserienr) {
        this.hvserienr = hvserienr;
    }
     
    
 
     
}

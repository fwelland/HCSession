package com.fhw;

import com.google.gson.Gson;
import com.sun.xml.rpc.tools.wscompile.UsageIf;
import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

@Named
@SessionScoped
public class UserBean 
    implements Serializable
{
    private static final long serialVersionUID = -4705648524130750355L;
    private String loginName;
    private String motd;

    public UserBean()
    {
        
    }

    @PostConstruct
    private void init()
    {
        System.err.println("UserBean being post constructed..."); 
        loginName="default"; 
    }
        
    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getMotd() {
        return motd;
    }

    public void setMotd(String motd) {
        this.motd = motd; 
   }
   
    public String toJSON()
    {
       String guts; 
       Gson gson = new Gson();
       guts = gson.toJson(this);
       return(guts);         
    }
   
   public void updateFromJSON(String jsonString)
   {
       Gson gson = new Gson();
       UserBean ub = gson.fromJson(jsonString, UserBean.class); 
       loginName = ub.loginName;
       motd = ub.motd; 
   }
}

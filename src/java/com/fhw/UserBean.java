package com.fhw;

import com.google.gson.Gson;
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
    private long touchTime; 

    public UserBean()
    {
        
    }

    @PostConstruct
    private void init()
    {
        loginName="default"; 
        touchTime = System.currentTimeMillis();
    }
        
    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName)
    {
        System.err.println("UB.setLoginName called with : " + loginName);
        this.loginName = loginName;
        touchTime = System.currentTimeMillis();
    }

    public String getMotd()
    {
        return motd;
    }

    public void setMotd(String motd)
    {
        this.motd = motd; 
        touchTime = System.currentTimeMillis();
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

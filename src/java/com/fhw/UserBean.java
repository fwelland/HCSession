package com.fhw;

import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

@Named
@SessionScoped
public class UserBean 
    implements Serializable
{
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
    
}

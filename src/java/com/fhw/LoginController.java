package com.fhw;
import javax.annotation.PostConstruct;
import javax.inject.Named;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@Named(value = "loginController")
@RequestScoped
public class LoginController
{

    @Inject
    private UserBean userBean; 
    
    public LoginController()
    {
    }
    
    @PostConstruct
    private void init()
    {
    }
           
    public void save()
    {
        System.err.println("LC.save:  " + userBean.toJSON()); 
    }    
    
    public String getUserJSON()
    {
        return(userBean.toJSON()); 
    }
    
    public void setUserJSON(String json)
    {
        userBean.updateFromJSON(json);
    }
}

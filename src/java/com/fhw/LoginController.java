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
        System.err.println("post construction of LoginController");
    }
    
    
    public void startSession()
    {
        System.err.println("startSession pressed"); 
        
        
    }
    
    public void save()
    {
        System.err.println("nothing really to do"); 
                
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

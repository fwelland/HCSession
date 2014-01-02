package com.fhw;
import javax.annotation.PostConstruct;
import javax.inject.Named;
import javax.enterprise.context.RequestScoped;

@Named(value = "loginController")
@RequestScoped
public class LoginController
{

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
    
    
    
}

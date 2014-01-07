package com.fhw;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebFilter  (
                filterName = "SessionFailoverFilter", 
                urlPatterns = {"/*"},
                dispatcherTypes = {DispatcherType.ASYNC, DispatcherType.REQUEST}, 
                displayName = "HA Session Filter"
            )
public class SessionFailoverFilter implements Filter
{
    private static final String SESSION_COOKIE_NAME = "BAPHA.sessionID";
    private FilterConfig filterConfig = null;
    private boolean sessionCookieSecure = false;
    private boolean sessionCookieHttpOnly = false;
    private String sessionCookieDomain;
    private HazelcastInstance hazelcastInstance;
    private final String clusterMapName = "bap-hz-sessions";

    @Inject 
    private UserBean userBean; 
    
    private static final ConcurrentMap<String, String> mapSessions = new ConcurrentHashMap();

    public SessionFailoverFilter()
    {
    }

    private void doBeforeProcessing(ServletRequest request, ServletResponse response)
            throws IOException, ServletException
    {
        String haSessionID = getHASessionId((HttpServletRequest) request);
        if (null != haSessionID)
        {
            applySession((HttpServletRequest) request, haSessionID, (HttpServletResponse) response);
        }
        else
        {
            haSessionID = generateSessionId();
            makeSession((HttpServletRequest) request, haSessionID, (HttpServletResponse) response);
        }
    }

    private void doAfterProcessing(ServletRequest request, ServletResponse response)
            throws IOException, ServletException
    {
        String sessionId = getHASessionId( (HttpServletRequest) request ); 
        if(null != sessionId)
        {
            if(null != userBean )
            {
                String sessVal = userBean.toJSON(); 
                IMap<String, String> hcSessions = hazelcastInstance.getMap(clusterMapName);
                hcSessions.put(sessionId,sessVal); 
            }
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException
    {
        doBeforeProcessing(request, response);
        Throwable problem = null;
        try
        {
            chain.doFilter(request, response);
        }
        catch (Throwable t)
        {
            // If an exception is thrown somewhere down the filter chain,
            // we still want to execute our after processing, and then
            // rethrow the problem after that.
            problem = t;
        }
        doAfterProcessing(request, response);
        // If there was a problem, we want to rethrow it if it is
        // a known type, otherwise log it.
        if (problem != null)
        {
            if (problem instanceof ServletException)
            {
                throw (ServletException) problem;
            }
            if (problem instanceof IOException)
            {
                throw (IOException) problem;
            }
        }
    }

    @Override
    public void destroy()
    {
    }

    private URL getConfigURL() 
        throws ServletException
    {
        String configLocation = "/WEB-INF/hazelcast-client.xml";
        URL configUrl = null;
        try
        {
            configUrl = filterConfig.getServletContext().getResource(configLocation);
        }
        catch (MalformedURLException ignore)
        {
        }
        if (configUrl == null)
        {
            throw new ServletException("Could not load configuration '" + configLocation + "'");
        }
        return configUrl;
    }

    private void loadHazelCast()
            throws IOException, ServletException
    {
        URL confUrl = getConfigURL(); 
        ClientConfig cConf = new XmlClientConfigBuilder(confUrl).build();
        hazelcastInstance = HazelcastClient.newHazelcastClient(cConf); 
    }

    @Override
    public void init(final FilterConfig fConfig)
    {
        this.filterConfig = fConfig;
        sessionCookieDomain = filterConfig.getInitParameter("cookie-domain");
        String cookieSecure = filterConfig.getInitParameter("cookie-secure");
        if (cookieSecure != null)
        {
            sessionCookieSecure = Boolean.valueOf(cookieSecure);
        }
        String cookieHttpOnly = filterConfig.getInitParameter("cookie-http-only");
        if (cookieHttpOnly != null)
        {
            sessionCookieHttpOnly = Boolean.valueOf(cookieHttpOnly);
        }
        try
        {
            loadHazelCast();
        }
        catch(IOException|ServletException e)
        {
            throw new RuntimeException(e.getMessage(), e); 
        }
    }

    public void log(String msg)
    {
        filterConfig.getServletContext().log(msg);
    }

    private static synchronized String generateSessionId()
    {
        final String id = UUID.randomUUID().toString();
        final StringBuilder sb = new StringBuilder("BAP");
        final char[] chars = id.toCharArray();
        for (final char c : chars)
        {
            if (c != '-')
            {
                if (Character.isLetter(c))
                {
                    sb.append(Character.toUpperCase(c));
                }
                else
                {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    private void applySession(final HttpServletRequest req, final String sessionId, final HttpServletResponse res)
    {
        IMap<String, String> hcSessions = hazelcastInstance.getMap(clusterMapName);
        String sessVal = hcSessions.get(sessionId); 
        if(null != userBean)
        {
            userBean.updateFromJSON(sessVal);
        }        
    }

    private void makeSession(final HttpServletRequest req, final String sessionId, final HttpServletResponse res)
    {
        final Cookie sessionCookie = new Cookie(SESSION_COOKIE_NAME, sessionId);
        String path = req.getContextPath();
        if ("".equals(path))
        {
            path = "/";
        }
        sessionCookie.setPath(path);
        sessionCookie.setMaxAge(-1);
        if (sessionCookieDomain != null)
        {
            sessionCookie.setDomain(sessionCookieDomain);
        }
        try
        {
            sessionCookie.setHttpOnly(sessionCookieHttpOnly);
        }
        catch (NoSuchMethodError e)
        {
            // must be servlet spec before 3.0, don't worry about it!
        }        
        if(null != userBean)
        {
            String sessVal = userBean.toJSON(); 
            IMap<String, String> hcSessions = hazelcastInstance.getMap(clusterMapName);
            hcSessions.put(sessionId,sessVal); 
            sessionCookie.setSecure(sessionCookieSecure);
            res.addCookie(sessionCookie);     
            log("Added a new BAP HZ session cookie [" + sessionId + "] with value " + sessVal);            
        }                
    }

    private String getHASessionId(final HttpServletRequest req)
    {
        final Cookie[] cookies = req.getCookies();
        String sessId = null;
        if (cookies != null)
        {
            for (final Cookie cookie : cookies)
            {
                final String name = cookie.getName();
                final String value = cookie.getValue();
                if (name.equalsIgnoreCase(SESSION_COOKIE_NAME))
                {
                    sessId = value;
                    break;
                }
            }
        }
        return sessId;
    }
}
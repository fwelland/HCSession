package com.fhw;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebFilter(filterName = "SessionFailoverFilter", urlPatterns =
{
    "/*"
})
public class SessionFailoverFilter implements Filter
{

    private static final boolean debug = true;
    private static final String SESSION_COOKIE_NAME = "BAPHA.sessionID"; 
    
    private FilterConfig filterConfig = null;
    private ServletContext servletContext;
    private boolean sessionCookieSecure = false;
    private boolean sessionCookieHttpOnly = false;
    private String sessionCookieDomain; 

    public SessionFailoverFilter()
    {
    }

    private void doBeforeProcessing(ServletRequest request, ServletResponse response)
            throws IOException, ServletException
    {
        if (debug)
        {
            log("SessionFailoverFilter:DoBeforeProcessing");
        }

	// Write code here to process the request and/or response before
        // the rest of the filter chain is invoked.
        // For example, a logging filter might log items on the request object,
        // such as the parameters.
	/*
         for (Enumeration en = request.getParameterNames(); en.hasMoreElements(); ) {
         String name = (String)en.nextElement();
         String values[] = request.getParameterValues(name);
         int n = values.length;
         StringBuffer buf = new StringBuffer();
         buf.append(name);
         buf.append("=");
         for(int i=0; i < n; i++) {
         buf.append(values[i]);
         if (i < n-1)
         buf.append(",");
         }
         log(buf.toString());
         }
         */
    }

    private void doAfterProcessing(ServletRequest request, ServletResponse response)
            throws IOException, ServletException
    {
        if (debug)
        {
            log("SessionFailoverFilter:DoAfterProcessing");
        }

	// Write code here to process the request and/or response after
        // the rest of the filter chain is invoked.
        // For example, a logging filter might log the attributes on the
        // request object after the request has been processed. 
	/*
         for (Enumeration en = request.getAttributeNames(); en.hasMoreElements(); ) {
         String name = (String)en.nextElement();
         Object value = request.getAttribute(name);
         log("attribute: " + name + "=" + value.toString());

         }
         */
        // For example, a filter might append something to the response.
	/*
         PrintWriter respOut = new PrintWriter(response.getWriter());
         respOut.println("<P><B>This has been appended by an intrusive filter.</B>");
         */
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException
    {

        if (debug)
        {
            log("SessionFailoverFilter:doFilter()");
        }

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
            t.printStackTrace();
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
            sendProcessingError(problem, response);
        }
    }


    @Override
    public void destroy()
    {
    }

    @Override
    public void init(final FilterConfig fConfig)
    {
        this.filterConfig = fConfig;
        servletContext = filterConfig.getServletContext();
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
    }

    /**
     * Return a String representation of this object.
     */
    @Override
    public String toString()
    {
        if (filterConfig == null)
        {
            return ("SessionFailoverFilter()");
        }
        StringBuffer sb = new StringBuffer("SessionFailoverFilter(");
        sb.append(filterConfig);
        sb.append(")");
        return (sb.toString());
    }

    private void sendProcessingError(Throwable t, ServletResponse response)
    {
        String stackTrace = getStackTrace(t);

        if (stackTrace != null && !stackTrace.equals(""))
        {
            try
            {
                response.setContentType("text/html");
                PrintStream ps = new PrintStream(response.getOutputStream());
                PrintWriter pw = new PrintWriter(ps);
                pw.print("<html>\n<head>\n<title>Error</title>\n</head>\n<body>\n"); //NOI18N

                // PENDING! Localize this for next official release
                pw.print("<h1>The resource did not process correctly</h1>\n<pre>\n");
                pw.print(stackTrace);
                pw.print("</pre></body>\n</html>"); //NOI18N
                pw.close();
                ps.close();
                response.getOutputStream().close();
            }
            catch (Exception ex)
            {
            }
        }
        else
        {
            try
            {
                PrintStream ps = new PrintStream(response.getOutputStream());
                t.printStackTrace(ps);
                ps.close();
                response.getOutputStream().close();
            }
            catch (Exception ex)
            {
            }
        }
    }

    public static String getStackTrace(Throwable t)
    {
        String stackTrace = null;
        try
        {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.close();
            sw.close();
            stackTrace = sw.getBuffer().toString();
        }
        catch (Exception ex)
        {
        }
        return stackTrace;
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

    private void addSessionCookie(final HttpServletRequest req, final String sessionId, final HttpServletResponse res)
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
        sessionCookie.setSecure(sessionCookieSecure);        
        res.addCookie(sessionCookie);
    }

    private String getSessionCookie(final HttpServletRequest req)
    {
        final Cookie[] cookies = req.getCookies();
        if (cookies != null)
        {
            for (final Cookie cookie : cookies)
            {
                final String name = cookie.getName();
                final String value = cookie.getValue();
                if (name.equalsIgnoreCase(SESSION_COOKIE_NAME))
                {
                    return value;
                }
            }
        }
        return null;
    }    
    
    
}

//@SuppressWarnings("deprecation")
//public class WebFilter implements Filter {
//
//    private static final ILogger logger = Logger.getLogger(WebFilter.class);
//
//    private static final LocalCacheEntry NULL_ENTRY = new LocalCacheEntry();
//
//    private static final String HAZELCAST_REQUEST = "*hazelcast-request";
//
//    private static final String HAZELCAST_SESSION_COOKIE_NAME = "hazelcast.sessionId";
//
//    static final String HAZELCAST_SESSION_ATTRIBUTE_SEPARATOR = "::hz::";
//
//    private static final ConcurrentMap<String, String> mapOriginalSessions = new ConcurrentHashMap<String, String>(1000);
//
//    private static final ConcurrentMap<String, HazelcastHttpSession> mapSessions = new ConcurrentHashMap<String, HazelcastHttpSession>(1000);
//
//    private HazelcastInstance hazelcastInstance;
//
//    private String clusterMapName = "none";
//
//    private String sessionCookieName = HAZELCAST_SESSION_COOKIE_NAME;
//
//    private String sessionCookieDomain = null;
//
//    private boolean sessionCookieSecure = false;
//
//    private boolean sessionCookieHttpOnly = false;
//
//    private boolean stickySession = true;
//
//    private boolean shutdownOnDestroy = true;
//
//    private boolean deferredWrite = false;
//
//    private Properties properties;
//
//    protected ServletContext servletContext;
//
//    protected FilterConfig filterConfig;
//
//    public WebFilter() {
//    }
//
//    public WebFilter(Properties properties) {
//        this();
//        this.properties = properties;
//    }
//
//    public final void init(final FilterConfig config) throws ServletException {
//        filterConfig = config;
//        servletContext = config.getServletContext();
//        initInstance();
//        String mapName = getParam("map-name");
//        if (mapName != null) {
//            clusterMapName = mapName;
//        } else {
//            clusterMapName = "_web_" + servletContext.getServletContextName();
//        }
//        try {
//            Config hzConfig = hazelcastInstance.getConfig();
//            String sessionTTL = getParam("session-ttl-seconds");
//            if (sessionTTL != null) {
//                MapConfig mapConfig = hzConfig.getMapConfig(clusterMapName);
//                mapConfig.setTimeToLiveSeconds(Integer.valueOf(sessionTTL));
//                hzConfig.addMapConfig(mapConfig);
//            }
//        } catch (UnsupportedOperationException ignored) {
//            // client cannot access Config.
//        }
//        String cookieName = getParam("cookie-name");
//        if (cookieName != null) {
//            sessionCookieName = cookieName;
//        }
//        String cookieDomain = getParam("cookie-domain");
//        if (cookieDomain != null) {
//            sessionCookieDomain = cookieDomain;
//        }
//        String cookieSecure = getParam("cookie-secure");
//        if (cookieSecure != null) {
//            sessionCookieSecure = Boolean.valueOf(cookieSecure);
//        }
//        String cookieHttpOnly = getParam("cookie-http-only");
//        if (cookieHttpOnly != null) {
//            sessionCookieHttpOnly = Boolean.valueOf(cookieHttpOnly);
//        }
//        String stickySessionParam = getParam("sticky-session");
//        if (stickySessionParam != null) {
//            stickySession = Boolean.valueOf(stickySessionParam);
//        }
//        String shutdownOnDestroyParam = getParam("shutdown-on-destroy");
//        if (shutdownOnDestroyParam != null) {
//            shutdownOnDestroy = Boolean.valueOf(shutdownOnDestroyParam);
//        }
//        String deferredWriteParam = getParam("deferred-write");
//        if (deferredWriteParam != null) {
//            deferredWrite = Boolean.parseBoolean(deferredWriteParam);
//        }
//        if (!stickySession) {
//            getClusterMap().addEntryListener(new EntryListener<String, Object>() {
//                public void entryAdded(EntryEvent<String, Object> entryEvent) {
//                }
//
//                public void entryRemoved(EntryEvent<String, Object> entryEvent) {
//                    if (entryEvent.getMember() == null || // client events has no owner member
//                            !entryEvent.getMember().localMember()) {
//                        removeSessionLocally(entryEvent.getKey());
//                    }
//                }
//
//                public void entryUpdated(EntryEvent<String, Object> entryEvent) {
//                }
//
//                public void entryEvicted(EntryEvent<String, Object> entryEvent) {
//                    entryRemoved(entryEvent);
//                }
//            }, false);
//        }
//
//        if(logger.isLoggable(Level.FINEST)){
//            logger.finest("sticky:" + stickySession + ", shutdown-on-destroy: " + shutdownOnDestroy
//                    + ", map-name: " + clusterMapName);
//        }
//    }
//
//    private void initInstance() throws ServletException {
//        if (properties == null) {
//            properties = new Properties();
//        }
//        setProperty(CONFIG_LOCATION);
//        setProperty(INSTANCE_NAME);
//        setProperty(USE_CLIENT);
//        setProperty(CLIENT_CONFIG_LOCATION);
//        hazelcastInstance = getInstance(properties);
//    }
//
//    private void setProperty(String propertyName) {
//        String value = getParam(propertyName);
//        if (value != null) {
//            properties.setProperty(propertyName, value);
//        }
//    }
//
//    private void removeSessionLocally(String sessionId) {
//        HazelcastHttpSession hazelSession = mapSessions.remove(sessionId);
//        if (hazelSession != null) {
//            mapOriginalSessions.remove(hazelSession.originalSession.getId());
//            if(logger.isLoggable(Level.FINEST)){
//                logger.finest("Destroying session locally " + hazelSession);
//            }
//            hazelSession.destroy();
//        }
//    }
//
//    static void destroyOriginalSession(HttpSession originalSession) {
//        String hazelcastSessionId = mapOriginalSessions.remove(originalSession.getId());
//        if (hazelcastSessionId != null) {
//            HazelcastHttpSession hazelSession = mapSessions.remove(hazelcastSessionId);
//            if (hazelSession != null) {
//                hazelSession.webFilter.destroySession(hazelSession, false);
//            }
//        }
//    }
//
//       private String extractAttributeKey(String key) {
//        return key.substring(key.indexOf(HAZELCAST_SESSION_ATTRIBUTE_SEPARATOR) + HAZELCAST_SESSION_ATTRIBUTE_SEPARATOR.length());
//    }
//
//    private HazelcastHttpSession createNewSession(RequestWrapper requestWrapper, String existingSessionId) {
//        String id = existingSessionId != null ? existingSessionId : generateSessionId();
//        if (requestWrapper.getOriginalSession(false) != null) {
//            logger.finest("Original session exists!!!");
//        }
//        HttpSession originalSession = requestWrapper.getOriginalSession(true);
//        HazelcastHttpSession hazelcastSession = new HazelcastHttpSession(WebFilter.this, id, originalSession, deferredWrite);
//        mapSessions.put(hazelcastSession.getId(), hazelcastSession);
//        String oldHazelcastSessionId = mapOriginalSessions.put(originalSession.getId(), hazelcastSession.getId());
//        if (oldHazelcastSessionId != null) {
//            if(logger.isFinestEnabled()){
//                logger.finest("!!! Overriding an existing hazelcastSessionId " + oldHazelcastSessionId);
//            }
//        }
//        if(logger.isFinestEnabled()){
//            logger.finest("Created new session with id: " + id);
//            logger.finest(mapSessions.size() + " is sessions.size and originalSessions.size: " + mapOriginalSessions.size());
//        }
//        addSessionCookie(requestWrapper, id);
//        if (deferredWrite) {
//            loadHazelcastSession(hazelcastSession);
//        }
//        return hazelcastSession;
//    }
//
//    private void loadHazelcastSession(HazelcastHttpSession hazelcastSession) {
//        Set<Entry<String, Object>> entrySet = getClusterMap().entrySet(new SessionAttributePredicate(hazelcastSession.getId()));
//        Map<String, LocalCacheEntry> cache = hazelcastSession.localCache;
//        for (Entry<String, Object> entry : entrySet) {
//            String attributeKey = extractAttributeKey(entry.getKey());
//            LocalCacheEntry cacheEntry = cache.get(attributeKey);
//            if (cacheEntry == null) {
//                cacheEntry = new LocalCacheEntry();
//                cache.put(attributeKey, cacheEntry);
//            }
//            if(logger.isFinestEnabled()){
//                logger.finest("Storing " + attributeKey + " on session " + hazelcastSession.getId());
//            }
//            cacheEntry.value = entry.getValue();
//            cacheEntry.dirty = false;
//        }
//    }
//
//    private void prepareReloadingSession(HazelcastHttpSession hazelcastSession) {
//        if (deferredWrite && hazelcastSession != null) {
//            Map<String, LocalCacheEntry> cache = hazelcastSession.localCache;
//            for (LocalCacheEntry cacheEntry : cache.values()) {
//                cacheEntry.reload = true;
//            }
//        }
//    }
//
//    /**
//     * Destroys a session, determining if it should be destroyed clusterwide automatically or via expiry.
//     *
//     * @param session             The session to be destroyed
//     * @param removeGlobalSession boolean value - true if the session should be destroyed irrespective of active time
//     */
//    private void destroySession(HazelcastHttpSession session, boolean removeGlobalSession) {
//        if(logger.isFinestEnabled()){
//            logger.finest("Destroying local session: " + session.getId());
//        }
//        mapSessions.remove(session.getId());
//        mapOriginalSessions.remove(session.originalSession.getId());
//        session.destroy();
//        if (removeGlobalSession) {
//            if(logger.isFinestEnabled()){
//                logger.finest("Destroying cluster session: " + session.getId() + " => Ignore-timeout: true");
//            }
//            IMap<String, Object> clusterMap = getClusterMap();
//            clusterMap.delete(session.getId());
//            clusterMap.executeOnEntries(new InvalidateEntryProcessor(session.getId()));
//        }
//    }
//
//    private IMap<String, Object> getClusterMap() {
//        return hazelcastInstance.getMap(clusterMapName);
//    }
//
//    private HazelcastHttpSession getSessionWithId(final String sessionId) {
//        HazelcastHttpSession session = mapSessions.get(sessionId);
//        if (session != null && !session.isValid()) {
//            destroySession(session, true);
//            session = null;
//        }
//        return session;
//    }
//
//    private class RequestWrapper extends HttpServletRequestWrapper {
//        HazelcastHttpSession hazelcastSession = null;
//
//        final ResponseWrapper res;
//
//        String requestedSessionId;
//
//        public RequestWrapper(final HttpServletRequest req,
//                              final ResponseWrapper res) {
//            super(req);
//            this.res = res;
//            req.setAttribute(HAZELCAST_REQUEST, this);
//        }
//
//        public void setHazelcastSession(HazelcastHttpSession hazelcastSession, String requestedSessionId) {
//            this.hazelcastSession = hazelcastSession;
//            this.requestedSessionId = requestedSessionId;
//        }
//
//        HttpSession getOriginalSession(boolean create) {
//            return super.getSession(create);
//        }
//
//        @Override
//        public RequestDispatcher getRequestDispatcher(final String path) {
//            final ServletRequest original = getRequest();
//            return new RequestDispatcher() {
//                public void forward(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
//                    original.getRequestDispatcher(path).forward(RequestWrapper.this, servletResponse);
//                }
//
//                public void include(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
//                    original.getRequestDispatcher(path).include(RequestWrapper.this, servletResponse);
//                }
//            };
//        }
//
//        public String fetchHazelcastSessionId() {
//            if (requestedSessionId != null) {
//                return requestedSessionId;
//            }
//            requestedSessionId = getSessionCookie(this);
//            if (requestedSessionId != null) {
//                return requestedSessionId;
//            }
//            requestedSessionId = getParameter(HAZELCAST_SESSION_COOKIE_NAME);
//            return requestedSessionId;
//        }
//
//        @Override
//        public HttpSession getSession() {
//            return getSession(true);
//        }
//
//        @Override
//        public HazelcastHttpSession getSession(final boolean create) {
//            if (hazelcastSession != null && !hazelcastSession.isValid()) {
//                logger.finest("Session is invalid!");
//                destroySession(hazelcastSession, true);
//                hazelcastSession = null;
//            }
//            if (hazelcastSession == null) {
//                HttpSession originalSession = getOriginalSession(false);
//                if (originalSession != null) {
//                    String hazelcastSessionId = mapOriginalSessions.get(originalSession.getId());
//                    if (hazelcastSessionId != null) {
//                        hazelcastSession = mapSessions.get(hazelcastSessionId);
//                    }
//                    if (hazelcastSession == null) {
//                        mapOriginalSessions.remove(originalSession.getId());
//                        originalSession.invalidate();
//                    }
//                }
//            }
//            if (hazelcastSession != null)
//                return hazelcastSession;
//            final String requestedSessionId = fetchHazelcastSessionId();
//            if (requestedSessionId != null) {
//                hazelcastSession = getSessionWithId(requestedSessionId);
//                if (hazelcastSession == null) {
//                    final Boolean existing = (Boolean) getClusterMap().get(requestedSessionId);
//                    if (existing != null && existing) {
//                        // we already have the session in the cluster loading it...
//                        hazelcastSession = createNewSession(RequestWrapper.this, requestedSessionId);
//                    }
//                }
//            }
//            if (hazelcastSession == null && create) {
//                hazelcastSession = createNewSession(RequestWrapper.this, null);
//            }
//            if (deferredWrite) {
//                prepareReloadingSession(hazelcastSession);
//            }
//            return hazelcastSession;
//        }
//    } // END of RequestWrapper
//
//    private static class ResponseWrapper extends HttpServletResponseWrapper {
//
//        public ResponseWrapper(final HttpServletResponse original) {
//            super(original);
//        }
//    }
//
//    private static class LocalCacheEntry {
//        private Object value;
//        volatile boolean dirty = false;
//        volatile boolean reload = false;
//        boolean removed = false; // does not need to be volatile - it's piggybacked on dirty!
//    }
//
//    private class HazelcastHttpSession implements HttpSession {
//        private final Map<String, LocalCacheEntry> localCache;
//
//        private final boolean deferredWrite;
//
//        volatile boolean valid = true;
//
//        final String id;
//
//        final HttpSession originalSession;
//
//        final WebFilter webFilter;
//
//        public HazelcastHttpSession(WebFilter webFilter, final String sessionId, HttpSession originalSession, boolean deferredWrite) {
//            this.webFilter = webFilter;
//            this.id = sessionId;
//            this.originalSession = originalSession;
//            this.deferredWrite = deferredWrite;
//            this.localCache = deferredWrite ? new ConcurrentHashMap<String, LocalCacheEntry>() : null;
//        }
//
//        public Object getAttribute(final String name) {
//            IMap<String, Object> clusterMap = getClusterMap();
//            if (deferredWrite) {
//                LocalCacheEntry cacheEntry = localCache.get(name);
//                if (cacheEntry == null || cacheEntry.reload) {
//                    Object value = clusterMap.get(buildAttributeName(name));
//                    if (value == null) {
//                        cacheEntry = NULL_ENTRY;
//                    } else {
//                        cacheEntry = new LocalCacheEntry();
//                        cacheEntry.value = value;
//                        cacheEntry.reload = false;
//                    }
//                    localCache.put(name, NULL_ENTRY);
//                }
//                return cacheEntry != NULL_ENTRY ? cacheEntry.value : null;
//            }
//            return clusterMap.get(buildAttributeName(name));
//        }
//
//        public Enumeration<String> getAttributeNames() {
//            final Set<String> keys = selectKeys();
//            return new Enumeration<String>() {
//                private final String[] elements = keys.toArray(new String[keys.size()]);
//                private int index = 0;
//
//                @Override
//                public boolean hasMoreElements() {
//                    return index < elements.length;
//                }
//
//                @Override
//                public String nextElement() {
//                    return elements[index++];
//                }
//            };
//        }
//
//        public String getId() {
//            return id;
//        }
//
//        public ServletContext getServletContext() {
//            return servletContext;
//        }
//
//        public HttpSessionContext getSessionContext() {
//            return originalSession.getSessionContext();
//        }
//
//        public Object getValue(final String name) {
//            return getAttribute(name);
//        }
//
//        public String[] getValueNames() {
//            final Set<String> keys = selectKeys();
//            return keys.toArray(new String[keys.size()]);
//        }
//
//        public void invalidate() {
//            originalSession.invalidate();
//            destroySession(this, true);
//        }
//
//        public boolean isNew() {
//            return originalSession.isNew();
//        }
//
//        public void putValue(final String name, final Object value) {
//            setAttribute(name, value);
//        }
//
//        public void removeAttribute(final String name) {
//            if (deferredWrite) {
//                LocalCacheEntry entry = localCache.get(name);
//                if (entry != null) {
//                    entry.value = null;
//                    entry.removed = true;
//                    // dirty needs to be set as last value for memory visibility reasons!
//                    entry.dirty = true;
//                }
//            } else {
//                getClusterMap().delete(buildAttributeName(name));
//            }
//        }
//
//        public void setAttribute(final String name, final Object value) {
//            if (name == null) {
//                throw new NullPointerException("name must not be null");
//            }
//            if (value == null) {
//                throw new IllegalArgumentException("value must not be null");
//            }
//            if (deferredWrite) {
//                LocalCacheEntry entry = localCache.get(name);
//                if (entry == null) {
//                    entry = new LocalCacheEntry();
//                    localCache.put(name, entry);
//                }
//                entry.value = value;
//                entry.dirty = true;
//            } else {
//                getClusterMap().put(buildAttributeName(name), value);
//            }
//        }
//
//        public void removeValue(final String name) {
//            removeAttribute(name);
//        }
//
//        public boolean sessionChanged() {
//            if (!deferredWrite) {
//                return false;
//            }
//            for (Entry<String, LocalCacheEntry> entry : localCache.entrySet()) {
//                if (entry.getValue().dirty) {
//                    return true;
//                }
//            }
//            return false;
//        }
//
//        public long getCreationTime() {
//            return originalSession.getCreationTime();
//        }
//
//        public long getLastAccessedTime() {
//            return originalSession.getLastAccessedTime();
//        }
//
//        public int getMaxInactiveInterval() {
//            return originalSession.getMaxInactiveInterval();
//        }
//
//        public void setMaxInactiveInterval(int maxInactiveSeconds) {
//            originalSession.setMaxInactiveInterval(maxInactiveSeconds);
//        }
//
//        void destroy() {
//            valid = false;
//        }
//
//        public boolean isValid() {
//            return valid;
//        }
//
//        private String buildAttributeName(String name) {
//            return id + HAZELCAST_SESSION_ATTRIBUTE_SEPARATOR + name;
//        }
//
//        private void sessionDeferredWrite() {
//            IMap<String, Object> clusterMap = getClusterMap();
//            if (deferredWrite) {
//                Iterator<Entry<String, LocalCacheEntry>> iterator = localCache.entrySet().iterator();
//                while(iterator.hasNext()) {
//                    Entry<String, LocalCacheEntry> entry = iterator.next();
//                    if (entry.getValue().dirty) {
//                        LocalCacheEntry cacheEntry = entry.getValue();
//                        if (cacheEntry.removed) {
//                            clusterMap.delete(buildAttributeName(entry.getKey()));
//                            iterator.remove();
//                        } else {
//                            clusterMap.put(buildAttributeName(entry.getKey()), cacheEntry.value);
//                            cacheEntry.dirty = false;
//                        }
//                    }
//                }
//            }
//            if (!clusterMap.containsKey(id)) {
//                clusterMap.put(id, Boolean.TRUE);
//            }
//        }
//
//        private Set<String> selectKeys() {
//            if (!deferredWrite) {
//                return getClusterMap().keySet(new SessionAttributePredicate(id));
//            }
//            Set<String> keys = new HashSet<String>();
//            Iterator<Entry<String, LocalCacheEntry>> iterator = localCache.entrySet().iterator();
//            while (iterator.hasNext()) {
//                Entry<String, LocalCacheEntry> entry = iterator.next();
//                if (!entry.getValue().removed) {
//                    keys.add(entry.getKey());
//                }
//            }
//            return keys;
//        }
//    }// END of HazelSession
//
//    private static synchronized String generateSessionId() {
//        final String id = UuidUtil.buildRandomUuidString();
//        final StringBuilder sb = new StringBuilder("HZ");
//        final char[] chars = id.toCharArray();
//        for (final char c : chars) {
//            if (c != '-') {
//                if (Character.isLetter(c)) {
//                    sb.append(Character.toUpperCase(c));
//                } else
//                    sb.append(c);
//            }
//        }
//        return sb.toString();
//    }
//
//    private void addSessionCookie(final RequestWrapper req, final String sessionId) {
//        final Cookie sessionCookie = new Cookie(sessionCookieName, sessionId);
//        String path = req.getContextPath();
//        if ("".equals(path)) {
//            path = "/";
//        }
//        sessionCookie.setPath(path);
//        sessionCookie.setMaxAge(-1);
//        if (sessionCookieDomain != null) {
//            sessionCookie.setDomain(sessionCookieDomain);
//        }
//        try {
//            sessionCookie.setHttpOnly(sessionCookieHttpOnly);
//        } catch (NoSuchMethodError e) {
//            // must be servlet spec before 3.0, don't worry about it!
//        }
//        sessionCookie.setSecure(sessionCookieSecure);
//        req.res.addCookie(sessionCookie);
//    }
//
//    private String getSessionCookie(final RequestWrapper req) {
//        final Cookie[] cookies = req.getCookies();
//        if (cookies != null) {
//            for (final Cookie cookie : cookies) {
//                final String name = cookie.getName();
//                final String value = cookie.getValue();
//                if (name.equalsIgnoreCase(sessionCookieName)) {
//                    return value;
//                }
//            }
//        }
//        return null;
//    }
//
//    public final void doFilter(ServletRequest req, ServletResponse res, final FilterChain chain)
//            throws IOException, ServletException {
//        if (!(req instanceof HttpServletRequest)) {
//            chain.doFilter(req, res);
//        } else {
//            if (req instanceof RequestWrapper) {
//                logger.finest("Request is instance of RequestWrapper! Continue...");
//                chain.doFilter(req, res);
//                return;
//            }
//            HttpServletRequest httpReq = (HttpServletRequest) req;
//            RequestWrapper existingReq = (RequestWrapper) req.getAttribute(HAZELCAST_REQUEST);
//            final ResponseWrapper resWrapper = new ResponseWrapper((HttpServletResponse) res);
//            final RequestWrapper reqWrapper = new RequestWrapper(httpReq, resWrapper);
//            if (existingReq != null) {
//                reqWrapper.setHazelcastSession(existingReq.hazelcastSession, existingReq.requestedSessionId);
//            }
//            chain.doFilter(reqWrapper, resWrapper);
//            if (existingReq != null) return;
//            HazelcastHttpSession session = reqWrapper.getSession(false);
//            if (session != null && session.isValid()) {
//                if (session.sessionChanged() || !deferredWrite) {
//                    if(logger.isFinestEnabled()){
//                        logger.finest("PUTTING SESSION " + session.getId());
//                    }
//                    session.sessionDeferredWrite();
//                }
//            }
//        }
//    }
//
//    public final void destroy() {
//        mapSessions.clear();
//        mapOriginalSessions.clear();
//        shutdownInstance();
//    }
//
//    protected HazelcastInstance getInstance(Properties properties) throws ServletException {
//        return HazelcastInstanceLoader.createInstance(filterConfig, properties);
//    }
//
//    protected void shutdownInstance() {
//        if (shutdownOnDestroy && hazelcastInstance != null) {
//            hazelcastInstance.getLifecycleService().shutdown();
//        }
//    }
//
//    private String getParam(String name) {
//        if (properties != null && properties.containsKey(name)) {
//            return properties.getProperty(name);
//        } else {
//            return filterConfig.getInitParameter(name);
//        }
//    }
//}// END of WebFilter


/*
 * Carrot2 project.
 *
 * Copyright (C) 2002-2014, Dawid Weiss, Stanisław Osiński.
 * All rights reserved.
 *
 * Refer to the full license file "carrot2.LICENSE"
 * in the root folder of the repository checkout or at:
 * http://www.carrot2.org/carrot2.LICENSE
 */

package org.carrot2.dcs;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.eclipse.jetty.util.component.LifeCycle.Listener;

/**
 * Bootstraps the Document Clustering Server using an embedded Jetty server.
 */
public class DcsApp
{
    /**
     * Is this the absolute minimum required for Jetty to run? 
     */
    private final static int MIN_THREADS = 10;

    /**
     * DCS logger. Tests attach to this logger's LOG4J appender.
     */
    final Logger log = org.slf4j.LoggerFactory.getLogger("dcs");

    @Option(name = "-port", usage = "Port number to bind to")
    int port = 8080;

    @Option(name = "-v", aliases =
    {
        "--verbose"
    }, required = false, usage = "Print detailed messages")
    boolean verbose;

    @Option(name = "--accept-queue", required = false, 
        usage = "Socket accept queue length (default 20).")
    int acceptQueue = 20;

    @Option(name = "--threads", required = false, 
        usage = "Maximum number of processing threads (default " + MIN_THREADS + ").")
    int maxThreads = MIN_THREADS;

    String appName;
    Server server;

    /** 
     * Empty implementation of {@link Listener}.
     */
    private static class ListenerAdapter implements LifeCycle.Listener
    {
        public void lifeCycleFailure(LifeCycle lc, Throwable t) { }
        public void lifeCycleStarted(LifeCycle lc) { }
        public void lifeCycleStarting(LifeCycle lc) { }
        public void lifeCycleStopped(LifeCycle lc) { }
        public void lifeCycleStopping(LifeCycle lc) { }
    }    
    
    DcsApp(String appName)
    {
        this.appName = appName;
    }

    void start() throws Exception
    {
        start(null);
    }

    void start(String webPathPrefix) throws Exception
    {
        log.info("Starting DCS...");

        // http://issues.carrot2.org/browse/CARROT-581
        if (maxThreads < MIN_THREADS)
        {
            throw new IllegalArgumentException("Max number of threads must be greater than "
                + MIN_THREADS);
        }

        server = new Server();
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(port);
        connector.setReuseAddress(false);
        connector.setAcceptQueueSize(acceptQueue);
        connector.setThreadPool(new QueuedThreadPool(maxThreads));
        connector.setSoLingerTime(0);
        server.addConnector(connector);

        WebAppContext wac = new WebAppContext();
        wac.setContextPath("/");
        wac.addLifeCycleListener(new ListenerAdapter()
        {
            public void lifeCycleStarted(LifeCycle lc)
            {
                log.info("DCS started on port: " + port);
            }
            
            
            public void lifeCycleFailure(LifeCycle lc, Throwable t)
            {
                log.error("DCS startup failure.");
                stop();
            }
            
            public void lifeCycleStopped(LifeCycle lc)
            {
                log.info("DCS stopped.");
            }
        });
        wac.setParentLoaderPriority(true);

        final String dcsWar = System.getProperty("dcs.war");
        if (dcsWar != null)
        {
            // WAR distribution provides, use it
            wac.setWar(dcsWar);
        }
        else
        {
            // Run from the provided web dir
            wac.setWar(webPathPrefix != null ? webPathPrefix + "/web" : "web");
            wac.setClassLoader(Thread.currentThread().getContextClassLoader());
            
            /*
             * Allow context classloader resource loading.
             */
            System.setProperty(RestProcessorServlet.ENABLE_CLASSPATH_LOCATOR, "true");
        }

        if (System.getProperty("dcs.development.mode") != null)
        {
            wac.setDefaultsDescriptor("etc/webdefault.xml");
        }

        server.setHandler(wac);
        server.setStopAtShutdown(true);

        // Start the http server.
        try
        {
            server.start();
        }
        catch (Exception e)
        {
            stop();
            throw e;
        }
    }

    void stop()
    {
        if (server != null)
        {
            try
            {
                server.stop();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String [] args) throws Exception
    {
        final DcsApp dcs = new DcsApp("dcs");

        final CmdLineParser parser = new CmdLineParser(dcs);
        parser.setUsageWidth(80);

        try
        {
            parser.parseArgument(args);
        }
        catch (CmdLineException e)
        {
            System.out.print("Usage: " + dcs.appName);
            parser.printSingleLineUsage(System.out);
            System.out.println();
            parser.printUsage(System.out);

            System.out.println("\n" + e.getMessage());
            return;
        }

        try
        {
            dcs.start();
        }
        catch (Exception e)
        {
            dcs.log.error("Startup failure: " + 
                e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}

/*
The MIT License (MIT)

Copyright (c) 2015, Hans-Georg Becker

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package de.tu_dortmund.ub.api.paaa;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.*;
import java.util.Properties;

/**
 * @author Hans-Georg Becker
 * @version 0.9.3 (2015-02-03)
 */
public class PaaaService {

    private static String conffile  = "conf/paaa.properties";

    public static void main(String[] args) throws Exception {

        if (args.length == 1) {
            conffile = args[0];
        }

        // Init properties
        Properties config = new Properties();
        try {
            InputStream inputStream = new FileInputStream(conffile);

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

                try {
                    config.load(reader);

                } finally {
                    reader.close();
                }
            }
            finally {
                inputStream.close();
            }
        }
        catch (IOException e) {
            System.out.println("FATAL ERROR: Die Datei '" + conffile + "' konnte nicht geöffnet werden!");
        }

        // init logger
        PropertyConfigurator.configure(config.getProperty("service.log4j-conf"));
        Logger logger = Logger.getLogger(PaaaService.class.getName());

        logger.info("[" + config.getProperty("service.name") + "] " + "Starting '" + config.getProperty("service.name") + "' ...");
        logger.info("[" + config.getProperty("service.name") + "] " + "conf-file = " + conffile);
        logger.info("[" + config.getProperty("service.name") + "] " + "log4j-conf-file = " + config.getProperty("service.log4j-conf"));

        // server
        Server server = new Server(Integer.parseInt(config.getProperty("service.port")));

        ServletContextHandler context = new ServletContextHandler();

        ServletHolder holderHome = new ServletHolder("static-home", DefaultServlet.class);
        holderHome.setInitParameter("resourceBase", config.getProperty("service.resourceBase"));
        context.addServlet(holderHome,"/*");

        context.setContextPath(config.getProperty("service.contextPath"));

        server.setHandler(context);

        context.addServlet(new ServletHolder(new PingEndpoint(conffile)), config.getProperty("service.endpoint.ping"));

        context.addServlet(new ServletHolder(new HealthEndpoint(conffile)), config.getProperty("service.endpoint.health"));

        context.addServlet(new ServletHolder(new PaaaEndpoint(conffile)), config.getProperty("service.endpoint") + "/*");

        server.start();
        server.join();
    }
}

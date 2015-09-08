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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.tu_dortmund.ub.api.paaa.auth.AuthorizationException;
import de.tu_dortmund.ub.api.paaa.auth.AuthorizationInterface;
import de.tu_dortmund.ub.api.paaa.ils.ILSException;
import de.tu_dortmund.ub.api.paaa.ils.IntegratedLibrarySystem;
import de.tu_dortmund.ub.api.paaa.model.*;
import de.tu_dortmund.ub.util.impl.Lookup;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.net.URLDecoder;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.Properties;
import java.util.UUID;

/**
 * @author Hans-Georg Becker
 * @version 0.9 (2015-06-05)
 */
public class PaaaEndpoint extends HttpServlet {

    // Configuration
    private String conffile = "";
    private Properties config = new Properties();
    private Logger logger = Logger.getLogger(PaaaEndpoint.class.getName());
    private Properties apikeys;

    /**
     * @throws java.io.IOException
     */
    public PaaaEndpoint() throws IOException {

        this("conf/paaa.properties");

    }

    /**
     * @throws java.io.IOException
     */
    public PaaaEndpoint(String propfile_api) throws IOException {

        this.conffile = propfile_api;

        // Init properties
        try {
            InputStream inputStream = new FileInputStream(propfile_api);

            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

                try {
                    this.config.load(reader);

                } finally {
                    reader.close();
                }
            } finally {
                inputStream.close();
            }
        } catch (IOException e) {
            System.out.println("FATAL ERROR: Die Datei '" + this.conffile + "' konnte nicht geöffnet werden!");
        }

        // init logger
        PropertyConfigurator.configure(this.config.getProperty("service.log4j-conf"));

        this.logger.info("[" + this.config.getProperty("service.name") + "] " + "Starting 'PaaaService' Endpoint ...");
        this.logger.info("[" + this.config.getProperty("service.name") + "] " + "conf-file = " + this.conffile);
        this.logger.info("[" + this.config.getProperty("service.name") + "] " + "log4j-conf-file = " + this.config.getProperty("service.log4j-conf"));

        this.apikeys = apikeys;
    }

    /**
     * @param httpServletRequest
     * @param httpServletResponse
     * @throws ServletException
     * @throws java.io.IOException
     */
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {

        ObjectMapper mapper = new ObjectMapper();

        this.logger.debug("[" + this.config.getProperty("service.name") + "] " + "PathInfo = " + httpServletRequest.getPathInfo());
        this.logger.debug("[" + this.config.getProperty("service.name") + "] " + "QueryString = " + httpServletRequest.getQueryString());

        String patronid = "";
        String service = "";
        String accept = "";
        String authorization = "";

        String path = httpServletRequest.getPathInfo();
        if (path != null) {
            String[] params = path.substring(1, path.length()).split("/");

            if (params.length == 1) {
                patronid = params[0];
                service = "patron";
            } else if (params.length == 2) {
                patronid = params[0];
                service = params[1];
            }
        }

        // 1. Schritt: Hole 'Accept' und 'Authorization' aus dem Header;
        Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {

            String headerNameKey = (String) headerNames.nextElement();
            this.logger.debug("[" + this.config.getProperty("service.name") + "] " + "headerNameKey = " + headerNameKey + " / headerNameValue = " + httpServletRequest.getHeader(headerNameKey));

            if (headerNameKey.equals("Accept")) {
                accept = httpServletRequest.getHeader(headerNameKey);
            }
            if (headerNameKey.equals("Authorization")) {
                authorization = httpServletRequest.getHeader(headerNameKey);
            }
        }

        if (authorization.equals("") && httpServletRequest.getParameter("access_token") != null && !httpServletRequest.getParameter("access_token").equals("")) {
            authorization = "Bearer " + httpServletRequest.getParameter("access_token");
        }

        this.logger.debug("[" + this.config.getProperty("service.name") + "] " + "Patron: " + patronid);
        this.logger.debug("[" + this.config.getProperty("service.name") + "] " + "Service: " + service);
        this.logger.debug("[" + this.config.getProperty("service.name") + "] " + "Accept: " + accept);
        this.logger.debug("[" + this.config.getProperty("service.name") + "] " + "Authorization: " + authorization);


        this.logger.error("[" + this.config.getProperty("service.name") + "] " + HttpServletResponse.SC_METHOD_NOT_ALLOWED + ": " + "GET for '" + service + "' not allowed!");

        httpServletResponse.setHeader("WWW-Authentificate", "Bearer");
        httpServletResponse.setHeader("WWW-Authentificate", "Bearer realm=\"PAIA Core\"");
        httpServletResponse.setContentType("application/json");
        httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");

        // Error handling mit suppress_response_codes=true
        if (httpServletRequest.getParameter("suppress_response_codes") != null && !httpServletRequest.getParameter("suppress_response_codes").equals("")) {
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        }
        // Error handling mit suppress_response_codes=false (=default)
        else {
            httpServletResponse.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }

        // Json für Response body
        RequestError requestError = new RequestError();
        requestError.setError(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_METHOD_NOT_ALLOWED)));
        requestError.setCode(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        requestError.setDescription(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_METHOD_NOT_ALLOWED) + ".description"));
        requestError.setErrorUri(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_METHOD_NOT_ALLOWED) + ".uri"));

        StringWriter json = new StringWriter();
        mapper.writeValue(json, requestError);
        this.logger.debug("[" + this.config.getProperty("service.name") + "] " + json);

        // send response
        httpServletResponse.getWriter().println(json);
    }

    /**
     * @param httpServletRequest
     * @param httpServletResponse
     * @throws ServletException
     * @throws java.io.IOException
     */
    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {

        ObjectMapper mapper = new ObjectMapper();

        this.logger.debug("[" + this.config.getProperty("service.name") + "] " + "PathInfo = " + httpServletRequest.getPathInfo());
        this.logger.debug("[" + this.config.getProperty("service.name") + "] " + "QueryString = " + httpServletRequest.getQueryString());

        String patronid = "";
        String service = "";
        String accept = "";
        String authorization = "";

        String format = "json";

        String path = httpServletRequest.getPathInfo();
        String[] params = path.substring(1, path.length()).split("/");

        if (params.length == 1) {
            patronid = params[0];
            service = "patron";
        }
        else if (params.length == 2) {
            patronid = params[0];
            service = params[1];
        }

        if (patronid.equals("patronid")) {
            patronid = "";
        }

        this.logger.debug("[" + this.config.getProperty("service.name") + "] " + "Patron: " + patronid);
        this.logger.debug("[" + this.config.getProperty("service.name") + "] " + "Service: " + service);

        if (httpServletRequest.getParameter("format") != null && !httpServletRequest.getParameter("format").equals("")) {

            format = httpServletRequest.getParameter("format");
        }
        else {

            Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
            while ( headerNames.hasMoreElements() ) {
                String headerNameKey = headerNames.nextElement();

                if (headerNameKey.equals("Accept")) {

                    this.logger.debug("headerNameKey = " + httpServletRequest.getHeader( headerNameKey ));

                    if (httpServletRequest.getHeader( headerNameKey ).contains("text/html")) {
                        format = "html";
                    }
                    else if (httpServletRequest.getHeader( headerNameKey ).contains("application/xml")) {
                        format = "xml";
                    }
                    else if (httpServletRequest.getHeader( headerNameKey ).contains("application/json")) {
                        format = "json";
                    }
                }
            }
        }

        this.logger.info("format = " + format);

        if (!format.equals("json") && !format.equals("xml")) {

            this.logger.error("[" + this.config.getProperty("service.name") + "] " + HttpServletResponse.SC_BAD_REQUEST + ": " + format + " not implemented!");

            // Error handling mit suppress_response_codes=true
            if (httpServletRequest.getParameter("suppress_response_codes") != null) {
                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            }
            // Error handling mit suppress_response_codes=false (=default)
            else {
                httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }

            // Json für Response body
            RequestError requestError = new RequestError();
            requestError.setError(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_BAD_REQUEST)));
            requestError.setCode(HttpServletResponse.SC_BAD_REQUEST);
            requestError.setDescription(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_BAD_REQUEST) + ".description"));
            requestError.setErrorUri(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_BAD_REQUEST) + ".uri"));

            this.sendRequestError(httpServletResponse, requestError, format);
        }
        else {
            // PAAA - function
            if (service.equals("signup") || service.equals("newpatron") || service.equals("updatepatron") || service.equals("blockpatron") || service.equals("unblockpatron") || service.equals("newfee")) {

                // get 'Accept' and 'Authorization' from Header;
                Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
                while (headerNames.hasMoreElements()) {

                    String headerNameKey = (String) headerNames.nextElement();
                    this.logger.debug("[" + this.config.getProperty("service.name") + "] " + "headerNameKey = " + headerNameKey + " / headerNameValue = " + httpServletRequest.getHeader(headerNameKey));

                    if (headerNameKey.equals("Accept")) {
                        accept = httpServletRequest.getHeader(headerNameKey);
                    }
                    if (headerNameKey.equals("Authorization")) {
                        authorization = httpServletRequest.getHeader(headerNameKey);
                    }
                }

                this.logger.debug("[" + this.config.getProperty("service.name") + "] " + "Accept: " + accept);
                this.logger.debug("[" + this.config.getProperty("service.name") + "] " + "Authorization: " + authorization);

                // if not exists token: read request parameter
                if (authorization.equals("") && httpServletRequest.getParameter("access_token") != null && !httpServletRequest.getParameter("access_token").equals("")) {
                    authorization = httpServletRequest.getParameter("access_token");
                }

                // if not exists token
                if (authorization.equals("")) {

                    // if exists PaiaService-Cookie: read content
                    Cookie[] cookies = httpServletRequest.getCookies();

                    if (cookies != null) {
                        for (Cookie cookie : cookies) {
                            if (cookie.getName().equals("PaaaService")) {

                                String value = URLDecoder.decode(cookie.getValue(), "UTF-8");
                                this.logger.info(value);
                                LoginResponse loginResponse = mapper.readValue(value, LoginResponse.class);

                                // A C H T U N G: ggf. andere patronID im Cookie als in Request (UniAccount vs. BibAccount)
                                if (loginResponse.getPatron().equals(patronid)) {
                                    authorization = loginResponse.getAccess_token();
                                }

                                break;
                            }
                        }
                    }
                }

                httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");

                // check token ...
                boolean isAuthorized = false;

                if (!authorization.equals("")) {

                    if (Lookup.lookupAll(AuthorizationInterface.class).size() > 0) {

                        AuthorizationInterface authorizationInterface = Lookup.lookup(AuthorizationInterface.class);
                        // init Authorization Service
                        authorizationInterface.init(this.config);

                        try {

                            isAuthorized = authorizationInterface.isTokenValid(httpServletResponse, service, patronid, authorization);
                        }
                        catch (AuthorizationException e) {

                            // TODO correct error handling
                            this.logger.error("[" + config.getProperty("service.name") + "] " + HttpServletResponse.SC_UNAUTHORIZED + "!");
                        }
                    } else {

                        // TODO correct error handling
                        this.logger.error("[" + this.config.getProperty("service.name") + "] " + HttpServletResponse.SC_INTERNAL_SERVER_ERROR + ": " + "Authorization Interface not implemented!");
                    }
                }

                this.logger.debug("[" + config.getProperty("service.name") + "] " + "Authorization: " + authorization + " - " + isAuthorized);

                // ... - if not is authorized - against DFN-AAI service
                if (!isAuthorized) {

                    // TODO if exists OpenAM-Session-Cookie: read content
                    this.logger.debug("[" + config.getProperty("service.name") + "] " + "Authorization: " + authorization + " - " + isAuthorized);
                }

                if (isAuthorized) {

                    // execute query
                    this.provideService(httpServletRequest, httpServletResponse, format, patronid, authorization, service);
                }
                else {

                    // Authorization
                    this.authorize(httpServletRequest, httpServletResponse, format);
                }
            }
            else {

                this.logger.error("[" + this.config.getProperty("service.name") + "] " + HttpServletResponse.SC_METHOD_NOT_ALLOWED + ": " + "POST for '" + service + "' not allowed!");

                httpServletResponse.setHeader("WWW-Authentificate", "Bearer");
                httpServletResponse.setHeader("WWW-Authentificate", "Bearer realm=\"PAAA\"");
                httpServletResponse.setContentType("application/json");

                // Error handling mit suppress_response_codes=true
                if (httpServletRequest.getParameter("suppress_response_codes") != null && !httpServletRequest.getParameter("suppress_response_codes").equals("")) {
                    httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                }
                // Error handling mit suppress_response_codes=false (=default)
                else {
                    httpServletResponse.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                }

                // Json für Response body
                RequestError requestError = new RequestError();
                requestError.setError(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_METHOD_NOT_ALLOWED)));
                requestError.setCode(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                requestError.setDescription(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_METHOD_NOT_ALLOWED) + ".description"));
                requestError.setErrorUri(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_METHOD_NOT_ALLOWED) + ".uri"));

                StringWriter json = new StringWriter();
                mapper.writeValue(json, requestError);
                this.logger.debug("[" + this.config.getProperty("service.name") + "] " + json);

                // send response
                httpServletResponse.getWriter().println(json);
            }
        }
    }

    protected void doDelete(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {

        ObjectMapper mapper = new ObjectMapper();

        this.logger.debug("[" + this.config.getProperty("service.name") + "] " + "PathInfo = " + httpServletRequest.getPathInfo());
        this.logger.debug("[" + this.config.getProperty("service.name") + "] " + "QueryString = " + httpServletRequest.getQueryString());

        String patronid = "";
        String service = "";
        String accept = "";
        String authorization = "";

        String format = "json";

        String path = httpServletRequest.getPathInfo();
        String[] params = path.substring(1, path.length()).split("/");

        if (params.length == 1) {
            patronid = params[0];
            service = "deletepatron";
        } else if (params.length == 2) {
            patronid = params[0];
            service = params[1];
        }

        this.logger.debug("[" + this.config.getProperty("service.name") + "] " + "Patron: " + patronid);
        this.logger.debug("[" + this.config.getProperty("service.name") + "] " + "Service: " + service);

        if (httpServletRequest.getParameter("format") != null && !httpServletRequest.getParameter("format").equals("")) {

            format = httpServletRequest.getParameter("format");
        }
        else {

            Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
            while ( headerNames.hasMoreElements() ) {
                String headerNameKey = headerNames.nextElement();

                if (headerNameKey.equals("Accept")) {

                    this.logger.debug("headerNameKey = " + httpServletRequest.getHeader( headerNameKey ));

                    if (httpServletRequest.getHeader( headerNameKey ).contains("text/html")) {
                        format = "html";
                    }
                    else if (httpServletRequest.getHeader( headerNameKey ).contains("application/xml")) {
                        format = "xml";
                    }
                    else if (httpServletRequest.getHeader( headerNameKey ).contains("application/json")) {
                        format = "json";
                    }
                }
            }
        }

        this.logger.info("format = " + format);

        if (!format.equals("json") && !format.equals("xml")) {

            this.logger.error("[" + this.config.getProperty("service.name") + "] " + HttpServletResponse.SC_BAD_REQUEST + ": " + format + " not implemented!");

            // Error handling mit suppress_response_codes=true
            if (httpServletRequest.getParameter("suppress_response_codes") != null) {
                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            }
            // Error handling mit suppress_response_codes=false (=default)
            else {
                httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }

            // Json für Response body
            RequestError requestError = new RequestError();
            requestError.setError(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_BAD_REQUEST)));
            requestError.setCode(HttpServletResponse.SC_BAD_REQUEST);
            requestError.setDescription(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_BAD_REQUEST) + ".description"));
            requestError.setErrorUri(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_BAD_REQUEST) + ".uri"));

            this.sendRequestError(httpServletResponse, requestError, format);
        }
        else {
            // PAAA - function
            if (service.equals("deletepatron")) {

                // get 'Accept' and 'Authorization' from Header;
                Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
                while (headerNames.hasMoreElements()) {

                    String headerNameKey = (String) headerNames.nextElement();
                    this.logger.debug("[" + this.config.getProperty("service.name") + "] " + "headerNameKey = " + headerNameKey + " / headerNameValue = " + httpServletRequest.getHeader(headerNameKey));

                    if (headerNameKey.equals("Accept")) {
                        accept = httpServletRequest.getHeader(headerNameKey);
                    }
                    if (headerNameKey.equals("Authorization")) {
                        authorization = httpServletRequest.getHeader(headerNameKey);
                    }
                }

                this.logger.debug("[" + this.config.getProperty("service.name") + "] " + "Accept: " + accept);
                this.logger.debug("[" + this.config.getProperty("service.name") + "] " + "Authorization: " + authorization);

                // if not exists token: read request parameter
                if (authorization.equals("") && httpServletRequest.getParameter("access_token") != null && !httpServletRequest.getParameter("access_token").equals("")) {
                    authorization = httpServletRequest.getParameter("access_token");
                }

                // if not exists token
                if (authorization.equals("")) {

                    // if exists PaiaService-Cookie: read content
                    Cookie[] cookies = httpServletRequest.getCookies();

                    if (cookies != null) {
                        for (Cookie cookie : cookies) {
                            if (cookie.getName().equals("PaaaService")) {

                                String value = URLDecoder.decode(cookie.getValue(), "UTF-8");
                                this.logger.info(value);
                                LoginResponse loginResponse = mapper.readValue(value, LoginResponse.class);

                                // A C H T U N G: ggf. andere patronID im Cookie als in Request (UniAccount vs. BibAccount)
                                if (loginResponse.getPatron().equals(patronid)) {
                                    authorization = loginResponse.getAccess_token();
                                }

                                break;
                            }
                        }
                    }
                }

                httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");

                // check token ...
                boolean isAuthorized = false;

                if (!authorization.equals("")) {

                    if (Lookup.lookupAll(AuthorizationInterface.class).size() > 0) {

                        AuthorizationInterface authorizationInterface = Lookup.lookup(AuthorizationInterface.class);
                        // init Authorization Service
                        authorizationInterface.init(this.config);

                        try {

                            isAuthorized = authorizationInterface.isTokenValid(httpServletResponse, service, patronid, authorization);
                        }
                        catch (AuthorizationException e) {

                            // TODO correct error handling
                            this.logger.error("[" + config.getProperty("service.name") + "] " + HttpServletResponse.SC_UNAUTHORIZED + "!");
                        }
                    } else {

                        // TODO correct error handling
                        this.logger.error("[" + this.config.getProperty("service.name") + "] " + HttpServletResponse.SC_INTERNAL_SERVER_ERROR + ": " + "Authorization Interface not implemented!");
                    }
                }

                this.logger.debug("[" + config.getProperty("service.name") + "] " + "Authorization: " + authorization + " - " + isAuthorized);

                // ... - if not is authorized - against DFN-AAI service
                if (!isAuthorized) {

                    // TODO if exists OpenAM-Session-Cookie: read content
                    this.logger.debug("[" + config.getProperty("service.name") + "] " + "Authorization: " + authorization + " - " + isAuthorized);
                }

                if (isAuthorized) {

                    // execute query
                    this.provideService(httpServletRequest, httpServletResponse, format, patronid, authorization, service);
                }
                else {

                    // Authorization
                    this.authorize(httpServletRequest, httpServletResponse, format);
                }
            }
            else {

                this.logger.error("[" + this.config.getProperty("service.name") + "] " + HttpServletResponse.SC_METHOD_NOT_ALLOWED + ": " + "DELETE for '" + service + "' not allowed!");

                httpServletResponse.setHeader("WWW-Authentificate", "Bearer");
                httpServletResponse.setHeader("WWW-Authentificate", "Bearer realm=\"PAAA\"");
                httpServletResponse.setContentType("application/json");
                httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");

                // Error handling mit suppress_response_codes=true
                if (httpServletRequest.getParameter("suppress_response_codes") != null && !httpServletRequest.getParameter("suppress_response_codes").equals("")) {
                    httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                }
                // Error handling mit suppress_response_codes=false (=default)
                else {
                    httpServletResponse.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                }

                // Json für Response body
                RequestError requestError = new RequestError();
                requestError.setError(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_METHOD_NOT_ALLOWED)));
                requestError.setCode(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                requestError.setDescription(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_METHOD_NOT_ALLOWED) + ".description"));
                requestError.setErrorUri(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_METHOD_NOT_ALLOWED) + ".uri"));

                StringWriter json = new StringWriter();
                mapper.writeValue(json, requestError);
                this.logger.debug("[" + this.config.getProperty("service.name") + "] " + json);

                // send response
                httpServletResponse.getWriter().println(json);
            }
        }
    }

    protected void doOptions(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {

        httpServletResponse.setHeader("Access-Control-Allow-Methods", this.config.getProperty("Access-Control-Allow-Methods"));
        httpServletResponse.addHeader("Access-Control-Allow-Headers", this.config.getProperty("Access-Control-Allow-Headers"));
        httpServletResponse.setHeader("Accept", this.config.getProperty("Accept"));
        httpServletResponse.setHeader("Access-Control-Allow-Origin", this.config.getProperty("Access-Control-Allow-Origin"));

        httpServletResponse.getWriter().println();
    }

    /**
     *
     * @param httpServletRequest
     * @param httpServletResponse
     * @throws IOException
     */
    private void authorize(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, String format) throws IOException {

        ObjectMapper mapper = new ObjectMapper();

        // Error handling mit suppress_response_codes=true
        if (httpServletRequest.getParameter("suppress_response_codes") != null) {
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        }
        // Error handling mit suppress_response_codes=false (=default)
        else {
            httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }

        // Json für Response body
        RequestError requestError = new RequestError();
        requestError.setError(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_UNAUTHORIZED)));
        requestError.setCode(HttpServletResponse.SC_UNAUTHORIZED);
        requestError.setDescription(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_UNAUTHORIZED) + ".description"));
        requestError.setErrorUri(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_UNAUTHORIZED) + ".uri"));

        // XML-Ausgabe mit JAXB
        if (format.equals("xml")) {

            try {

                JAXBContext context = JAXBContext.newInstance(RequestError.class);
                Marshaller m = context.createMarshaller();
                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

                // Write to HttpResponse
                httpServletResponse.setContentType("application/xml;charset=UTF-8");
                m.marshal(requestError, httpServletResponse.getWriter());

            } catch (JAXBException e) {
                this.logger.error(e.getMessage(), e.getCause());
                httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: Error while rendering the results.");
            }
        }

        // JSON-Ausgabe mit Jackson
        if (format.equals("json")) {

            httpServletResponse.setContentType("application/json;charset=UTF-8");
            mapper.writeValue(httpServletResponse.getWriter(), requestError);
        }
    }

    /**
     * PAAA services
     */
    private void provideService(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, String format, String patronid, String token, String service) throws IOException {

        String baseurl = httpServletRequest.getServerName() + ":" + httpServletRequest.getServerPort();
        this.logger.info("[" + config.getProperty("service.name") + "] " + "baseurl = " + baseurl);

        ObjectMapper mapper = new ObjectMapper();

        if (Lookup.lookupAll(IntegratedLibrarySystem.class).size() > 0) {

            try {
                IntegratedLibrarySystem integratedLibrarySystem = Lookup.lookup(IntegratedLibrarySystem.class);
                // init ILS
                integratedLibrarySystem.init(this.config);

                switch (service) {

                    case "signup": {

                        Patron patron = null;

                        // read Patron
                        StringBuffer jb = new StringBuffer();
                        String line = null;
                        try {
                            BufferedReader reader = httpServletRequest.getReader();
                            while ((line = reader.readLine()) != null)
                                jb.append(line);
                        } catch (Exception e) { /*report an error*/ }

                        Patron patron2create = mapper.readValue(jb.toString(), Patron.class);
                        if (patron2create.getAccount() == null || patron2create.getAccount().equals("")) {
                            if (!patronid.equals("")) {
                                patron2create.setAccount(patronid);
                            } else {
                                patron2create.setAccount(UUID.randomUUID().toString());
                            }
                        }

                        patron = integratedLibrarySystem.signup(patron2create);

                        this.logger.info("[" + config.getProperty("service.name") + "] " + token + " performed '" + service + "' event for patron '" + patronid + "' >>> success!");

                        if (patron != null) {

                            Block block = new Block();
                            LocalDateTime timePoint = LocalDateTime.now();
                            block.setDate(timePoint.getYear() + "-" + (timePoint.getMonthValue() < 10 ? "0" + timePoint.getMonthValue() : timePoint.getMonthValue()) + "-" + (timePoint.getDayOfMonth() < 10 ? "0" + timePoint.getDayOfMonth() : timePoint.getDayOfMonth()));
                            block.setKey("93");
                            integratedLibrarySystem.blockpatron(patron,block);

                            StringWriter json = new StringWriter();
                            mapper.writeValue(json, patron);
                            this.logger.debug("[" + config.getProperty("service.name") + "] " + json);

                            // If request contains parameter 'redirect_uri', then redirect mit access_token and patronid
                            if (httpServletRequest.getParameter("redirect_uri") != null) {
                                this.logger.debug("[" + config.getProperty("service.name") + "] " + "REDIRECT? " + httpServletRequest.getParameter("redirect_uri"));

                                httpServletResponse.sendRedirect(httpServletRequest.getParameter("redirect_uri") + "&patron=" + patronid + "&token=" + token);
                            }
                            else {
                                httpServletResponse.setContentType("application/json");
                                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                                httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");

                                httpServletResponse.getWriter().println(json);
                            }
                        }
                        else {
                            this.logger.error("[" + config.getProperty("service.name") + "] " + HttpServletResponse.SC_SERVICE_UNAVAILABLE + ": ILS!");

                            httpServletResponse.setHeader("WWW-Authentificate", "Bearer");
                            httpServletResponse.setHeader("WWW-Authentificate", "Bearer realm=\"PAAA\"");
                            httpServletResponse.setContentType("application/json");
                            httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");

                            // Error handling mit suppress_response_codes=true
                            if (httpServletRequest.getParameter("suppress_response_codes") != null) {
                                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                            }
                            // Error handling mit suppress_response_codes=false (=default)
                            else {
                                httpServletResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                            }

                            // Json für Response body
                            RequestError requestError = new RequestError();
                            requestError.setError(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE)));
                            requestError.setCode(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                            requestError.setDescription(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE) + ".description"));
                            requestError.setErrorUri(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE) + ".uri"));

                            StringWriter json = new StringWriter();
                            mapper.writeValue(json, requestError);
                            this.logger.debug("[" + config.getProperty("service.name") + "] " + json);

                            // send response
                            httpServletResponse.getWriter().println(json);
                        }

                        break;
                    }
                    case "newpatron": {

                        Patron patron = null;

                        // read Patron
                        StringBuffer jb = new StringBuffer();
                        String line = null;
                        try {
                            BufferedReader reader = httpServletRequest.getReader();
                            while ((line = reader.readLine()) != null)
                                jb.append(line);
                        } catch (Exception e) { /*report an error*/ }

                        Patron patron2create = mapper.readValue(jb.toString(), Patron.class);
                        patron2create.setAccount(patronid);

                        patron = integratedLibrarySystem.newpatron(patron2create);

                        this.logger.info("[" + config.getProperty("service.name") + "] " + token + " performed '" + service + "' event for patron '" + patronid + "' >>> success!");

                        if (patron != null) {

                            StringWriter json = new StringWriter();
                            mapper.writeValue(json, patron);
                            this.logger.debug("[" + config.getProperty("service.name") + "] " + json);

                            // If request contains parameter 'redirect_uri', then redirect mit access_token and patronid
                            if (httpServletRequest.getParameter("redirect_uri") != null) {
                                this.logger.debug("[" + config.getProperty("service.name") + "] " + "REDIRECT? " + httpServletRequest.getParameter("redirect_uri"));

                                httpServletResponse.sendRedirect(httpServletRequest.getParameter("redirect_uri") + "&patron=" + patronid + "&token=" + token);
                            }
                            else {
                                httpServletResponse.setContentType("application/json");
                                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                                httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");

                                httpServletResponse.getWriter().println(json);
                            }
                        }
                        else {
                            this.logger.error("[" + config.getProperty("service.name") + "] " + HttpServletResponse.SC_SERVICE_UNAVAILABLE + ": ILS!");

                            httpServletResponse.setHeader("WWW-Authentificate", "Bearer");
                            httpServletResponse.setHeader("WWW-Authentificate", "Bearer realm=\"PAAA\"");
                            httpServletResponse.setContentType("application/json");
                            httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");

                            // Error handling mit suppress_response_codes=true
                            if (httpServletRequest.getParameter("suppress_response_codes") != null) {
                                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                            }
                            // Error handling mit suppress_response_codes=false (=default)
                            else {
                                httpServletResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                            }

                            // Json für Response body
                            RequestError requestError = new RequestError();
                            requestError.setError(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE)));
                            requestError.setCode(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                            requestError.setDescription(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE) + ".description"));
                            requestError.setErrorUri(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE) + ".uri"));

                            StringWriter json = new StringWriter();
                            mapper.writeValue(json, requestError);
                            this.logger.debug("[" + config.getProperty("service.name") + "] " + json);

                            // send response
                            httpServletResponse.getWriter().println(json);
                        }

                        break;
                    }
                    case "updatepatron": {

                        Patron patron = null;

                        // read Patron
                        StringBuffer jb = new StringBuffer();
                        String line = null;
                        try {
                            BufferedReader reader = httpServletRequest.getReader();
                            while ((line = reader.readLine()) != null)
                                jb.append(line);
                        } catch (Exception e) { /*report an error*/ }

                        Patron patron2update = mapper.readValue(jb.toString(), Patron.class);
                        patron2update.setAccount(patronid);

                        // TODO Was tun bei Änderung des 'status'?

                        patron = integratedLibrarySystem.updatepatron(patron2update);

                        this.logger.info("[" + config.getProperty("service.name") + "] " + token + " performed '" + service + "' event for patron '" + patronid + "' >>> success!");

                        if (patron != null) {

                            StringWriter json = new StringWriter();
                            mapper.writeValue(json, patron);
                            this.logger.debug("[" + config.getProperty("service.name") + "] " + json);

                            // If request contains parameter 'redirect_uri', then redirect mit access_token and patronid
                            if (httpServletRequest.getParameter("redirect_uri") != null) {
                                this.logger.debug("[" + config.getProperty("service.name") + "] " + "REDIRECT? " + httpServletRequest.getParameter("redirect_uri"));

                                httpServletResponse.sendRedirect(httpServletRequest.getParameter("redirect_uri") + "&patron=" + patronid + "&token=" + token);
                            }
                            else {
                                httpServletResponse.setContentType("application/json");
                                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                                httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");

                                httpServletResponse.getWriter().println(json);
                            }
                        }
                        else {
                            this.logger.error("[" + config.getProperty("service.name") + "] " + HttpServletResponse.SC_SERVICE_UNAVAILABLE + ": ILS!");

                            httpServletResponse.setHeader("WWW-Authentificate", "Bearer");
                            httpServletResponse.setHeader("WWW-Authentificate", "Bearer realm=\"PAAA\"");
                            httpServletResponse.setContentType("application/json");
                            httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");

                            // Error handling mit suppress_response_codes=true
                            if (httpServletRequest.getParameter("suppress_response_codes") != null) {
                                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                            }
                            // Error handling mit suppress_response_codes=false (=default)
                            else {
                                httpServletResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                            }

                            // Json für Response body
                            RequestError requestError = new RequestError();
                            requestError.setError(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE)));
                            requestError.setCode(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                            requestError.setDescription(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE) + ".description"));
                            requestError.setErrorUri(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE) + ".uri"));

                            StringWriter json = new StringWriter();
                            mapper.writeValue(json, requestError);
                            this.logger.debug("[" + config.getProperty("service.name") + "] " + json);

                            // send response
                            httpServletResponse.getWriter().println(json);
                        }

                        break;
                    }
                    case "blockpatron": {

                        Patron patron = null;

                        // read Patron
                        StringBuffer jb = new StringBuffer();
                        String line = null;
                        try {
                            BufferedReader reader = httpServletRequest.getReader();
                            while ((line = reader.readLine()) != null)
                                jb.append(line);
                        } catch (Exception e) { /*report an error*/ }

                        Patron patron2block = new Patron();
                        patron2block.setAccount(patronid);
                        Block block = mapper.readValue(jb.toString(), Block.class);

                        patron = integratedLibrarySystem.blockpatron(patron2block, block);

                        this.logger.info("[" + config.getProperty("service.name") + "] " + token + " performed '" + service + "' event for patron '" + patronid + "' >>> success!");

                        if (patron != null) {

                            StringWriter json = new StringWriter();
                            mapper.writeValue(json, patron);
                            this.logger.debug("[" + config.getProperty("service.name") + "] " + json);

                            // If request contains parameter 'redirect_uri', then redirect mit access_token and patronid
                            if (httpServletRequest.getParameter("redirect_uri") != null) {
                                this.logger.debug("[" + config.getProperty("service.name") + "] " + "REDIRECT? " + httpServletRequest.getParameter("redirect_uri"));

                                httpServletResponse.sendRedirect(httpServletRequest.getParameter("redirect_uri") + "&patron=" + patronid + "&token=" + token);
                            }
                            else {
                                httpServletResponse.setContentType("application/json");
                                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                                httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");

                                httpServletResponse.getWriter().println(json);
                            }
                        }
                        else {
                            this.logger.error("[" + config.getProperty("service.name") + "] " + HttpServletResponse.SC_SERVICE_UNAVAILABLE + ": ILS!");

                            httpServletResponse.setHeader("WWW-Authentificate", "Bearer");
                            httpServletResponse.setHeader("WWW-Authentificate", "Bearer realm=\"PAAA\"");
                            httpServletResponse.setContentType("application/json");
                            httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");

                            // Error handling mit suppress_response_codes=true
                            if (httpServletRequest.getParameter("suppress_response_codes") != null) {
                                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                            }
                            // Error handling mit suppress_response_codes=false (=default)
                            else {
                                httpServletResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                            }

                            // Json für Response body
                            RequestError requestError = new RequestError();
                            requestError.setError(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE)));
                            requestError.setCode(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                            requestError.setDescription(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE) + ".description"));
                            requestError.setErrorUri(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE) + ".uri"));

                            StringWriter json = new StringWriter();
                            mapper.writeValue(json, requestError);
                            this.logger.debug("[" + config.getProperty("service.name") + "] " + json);

                            // send response
                            httpServletResponse.getWriter().println(json);
                        }

                        break;
                    }
                    case "unblockpatron": {

                        Patron patron = null;

                        // read Patron
                        StringBuffer jb = new StringBuffer();
                        String line = null;
                        try {
                            BufferedReader reader = httpServletRequest.getReader();
                            while ((line = reader.readLine()) != null)
                                jb.append(line);
                        } catch (Exception e) { /*report an error*/ }

                        Patron patron2unblock = new Patron();
                        patron2unblock.setAccount(patronid);
                        Block block = mapper.readValue(jb.toString(), Block.class);

                        patron = integratedLibrarySystem.unblockpatron(patron2unblock, block);

                        this.logger.info("[" + config.getProperty("service.name") + "] " + token + " performed '" + service + "' event for patron '" + patronid + "' >>> success!");

                        if (patron != null) {

                            StringWriter json = new StringWriter();
                            mapper.writeValue(json, patron);
                            this.logger.debug("[" + config.getProperty("service.name") + "] " + json);

                            // If request contains parameter 'redirect_uri', then redirect mit access_token and patronid
                            if (httpServletRequest.getParameter("redirect_uri") != null) {
                                this.logger.debug("[" + config.getProperty("service.name") + "] " + "REDIRECT? " + httpServletRequest.getParameter("redirect_uri"));

                                httpServletResponse.sendRedirect(httpServletRequest.getParameter("redirect_uri") + "&patron=" + patronid + "&token=" + token);
                            } else {
                                httpServletResponse.setContentType("application/json");
                                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                                httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");

                                httpServletResponse.getWriter().println(json);
                            }
                        }
                        else {
                            this.logger.error("[" + config.getProperty("service.name") + "] " + HttpServletResponse.SC_SERVICE_UNAVAILABLE + ": ILS!");

                            httpServletResponse.setHeader("WWW-Authentificate", "Bearer");
                            httpServletResponse.setHeader("WWW-Authentificate", "Bearer realm=\"PAAA\"");
                            httpServletResponse.setContentType("application/json");
                            httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");

                            // Error handling mit suppress_response_codes=true
                            if (httpServletRequest.getParameter("suppress_response_codes") != null) {
                                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                            }
                            // Error handling mit suppress_response_codes=false (=default)
                            else {
                                httpServletResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                            }

                            // Json für Response body
                            RequestError requestError = new RequestError();
                            requestError.setError(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE)));
                            requestError.setCode(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                            requestError.setDescription(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE) + ".description"));
                            requestError.setErrorUri(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE) + ".uri"));

                            StringWriter json = new StringWriter();
                            mapper.writeValue(json, requestError);
                            this.logger.debug("[" + config.getProperty("service.name") + "] " + json);

                            // send response
                            httpServletResponse.getWriter().println(json);
                        }

                        break;
                    }
                    case "deletepatron": {

                        Patron patron = null;

                        Patron patron2delete = new Patron();
                        patron2delete.setAccount(patronid);

                        patron = integratedLibrarySystem.deletepatron(patron2delete);

                        this.logger.info("[" + config.getProperty("service.name") + "] " + token + " performed '" + service + "' event for patron '" + patronid + "' >>> success!");

                        if (patron != null) {

                            StringWriter json = new StringWriter();
                            mapper.writeValue(json, patron);
                            this.logger.debug("[" + config.getProperty("service.name") + "] " + json);

                            // If request contains parameter 'redirect_uri', then redirect mit access_token and patronid
                            if (httpServletRequest.getParameter("redirect_uri") != null) {
                                this.logger.debug("[" + config.getProperty("service.name") + "] " + "REDIRECT? " + httpServletRequest.getParameter("redirect_uri"));

                                httpServletResponse.sendRedirect(httpServletRequest.getParameter("redirect_uri") + "&patron=" + patronid + "&token=" + token);
                            }
                            else {
                                httpServletResponse.setContentType("application/json");
                                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                                httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");

                                httpServletResponse.getWriter().println(json);
                            }
                        }
                        else {
                            this.logger.error("[" + config.getProperty("service.name") + "] " + HttpServletResponse.SC_SERVICE_UNAVAILABLE + ": ILS!");

                            httpServletResponse.setHeader("WWW-Authentificate", "Bearer");
                            httpServletResponse.setHeader("WWW-Authentificate", "Bearer realm=\"PAAA\"");
                            httpServletResponse.setContentType("application/json");
                            httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");

                            // Error handling mit suppress_response_codes=true
                            if (httpServletRequest.getParameter("suppress_response_codes") != null) {
                                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                            }
                            // Error handling mit suppress_response_codes=false (=default)
                            else {
                                httpServletResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                            }

                            // Json für Response body
                            RequestError requestError = new RequestError();
                            requestError.setError(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE)));
                            requestError.setCode(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                            requestError.setDescription(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE) + ".description"));
                            requestError.setErrorUri(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE) + ".uri"));

                            StringWriter json = new StringWriter();
                            mapper.writeValue(json, requestError);
                            this.logger.debug("[" + config.getProperty("service.name") + "] " + json);

                            // send response
                            httpServletResponse.getWriter().println(json);
                        }

                        break;
                    }
                    case "newfee": {

                        Patron patron = null;
                        Fee resultFee = null;

                        patron = new Patron();
                        patron.setAccount(patronid);

                        // read Fee
                        StringBuffer jb = new StringBuffer();
                        String line = null;
                        try {
                            BufferedReader reader = httpServletRequest.getReader();
                            while ((line = reader.readLine()) != null)
                                jb.append(line);
                        } catch (Exception e) { /*report an error*/ }

                        this.logger.debug("[" + config.getProperty("service.name") + "] " + "Fee = " + jb);

                        Fee fee = mapper.readValue(jb.toString(), Fee.class);

                        resultFee = integratedLibrarySystem.newfee(patron, fee);

                        this.logger.info("[" + config.getProperty("service.name") + "] " + token + " performed '" + service + "' event for patron '" + patronid + "' >>> success!");
                        StringWriter stringWriter = new StringWriter();
                        mapper.writeValue(stringWriter, resultFee);
                        this.logger.debug("[" + config.getProperty("service.name") + "] " + "Fee: " + stringWriter.toString());

                        if (patron != null) {

                            StringWriter json = new StringWriter();
                            mapper.writeValue(json, resultFee);
                            this.logger.debug(json);

                            // If request contains parameter 'redirect_uri', then redirect mit access_token and patronid
                            if (httpServletRequest.getParameter("redirect_uri") != null) {
                                this.logger.debug("[" + config.getProperty("service.name") + "] " + "REDIRECT? " + httpServletRequest.getParameter("redirect_uri"));

                                httpServletResponse.sendRedirect(httpServletRequest.getParameter("redirect_uri") + "&patron=" + patronid + "&token=" + token);
                            }
                            else {
                                httpServletResponse.setContentType("application/json");
                                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                                httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");

                                httpServletResponse.getWriter().println(json);
                            }
                        }
                        else {
                            this.logger.error("[" + config.getProperty("service.name") + "] " + HttpServletResponse.SC_SERVICE_UNAVAILABLE + ": ILS!");

                            httpServletResponse.setHeader("WWW-Authentificate", "Bearer");
                            httpServletResponse.setHeader("WWW-Authentificate", "Bearer realm=\"PAAA\"");
                            httpServletResponse.setContentType("application/json");
                            httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");

                            // Error handling mit suppress_response_codes=true
                            if (httpServletRequest.getParameter("suppress_response_codes") != null) {
                                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                            }
                            // Error handling mit suppress_response_codes=false (=default)
                            else {
                                httpServletResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                            }

                            // Json für Response body
                            RequestError requestError = new RequestError();
                            requestError.setError(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE)));
                            requestError.setCode(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                            requestError.setDescription(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE) + ".description"));
                            requestError.setErrorUri(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE) + ".uri"));

                            StringWriter json = new StringWriter();
                            mapper.writeValue(json, requestError);
                            this.logger.debug("[" + config.getProperty("service.name") + "] " + json);

                            // send response
                            httpServletResponse.getWriter().println(json);
                        }

                        break;
                    }
                    default: {
                        // TODO: keine gültige Funktion
                    }
                }
            }
            catch (ILSException e) {

                this.logger.info("[" + config.getProperty("service.name") + "] " + token + " performed '" + service + "' event for patron '" + patronid + "' >>> failed!");
                this.logger.error("[" + config.getProperty("service.name") + "] " + HttpServletResponse.SC_SERVICE_UNAVAILABLE + ": ILS! " + e.getMessage());

                httpServletResponse.setHeader("WWW-Authentificate", "Bearer");
                httpServletResponse.setHeader("WWW-Authentificate", "Bearer realm=\"PAAA\"");
                httpServletResponse.setContentType("application/json");
                httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");

                // Error handling mit suppress_response_codes=true
                if (httpServletRequest.getParameter("suppress_response_codes") != null) {
                    httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                }
                // Error handling mit suppress_response_codes=false (=default)
                else {
                    httpServletResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                }

                // Json für Response body
                RequestError requestError = new RequestError();
                requestError.setError(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE)));
                requestError.setCode(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                requestError.setDescription(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE) + ".description"));
                requestError.setErrorUri(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE) + ".uri"));

                StringWriter json = new StringWriter();
                mapper.writeValue(json, requestError);
                this.logger.debug("[" + config.getProperty("service.name") + "] " + json);

                // send response
                httpServletResponse.getWriter().println(json);
            }
        }
        else {

            this.logger.error("[" + config.getProperty("service.name") + "] " + HttpServletResponse.SC_SERVICE_UNAVAILABLE + ": Config Error!");

            httpServletResponse.setHeader("WWW-Authentificate", "Bearer");
            httpServletResponse.setHeader("WWW-Authentificate", "Bearer realm=\"PAAA\"");
            httpServletResponse.setContentType("application/json");
            httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");

            // Error handling mit suppress_response_codes=true
            if (httpServletRequest.getParameter("suppress_response_codes") != null) {
                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            }
            // Error handling mit suppress_response_codes=false (=default)
            else {
                httpServletResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }

            // Json für Response body
            RequestError requestError = new RequestError();
            requestError.setError(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE)));
            requestError.setCode(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            requestError.setDescription(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE) + ".description"));
            requestError.setErrorUri(this.config.getProperty("error." + Integer.toString(HttpServletResponse.SC_SERVICE_UNAVAILABLE) + ".uri"));

            StringWriter json = new StringWriter();
            mapper.writeValue(json, requestError);
            this.logger.debug("[" + config.getProperty("service.name") + "] " + json);

            // send response
            httpServletResponse.getWriter().println(json);
        }

        }

    private void sendRequestError(HttpServletResponse httpServletResponse, RequestError requestError, String format) {

        ObjectMapper mapper = new ObjectMapper();

        httpServletResponse.setHeader("WWW-Authentificate", "Bearer");
        httpServletResponse.setHeader("WWW-Authentificate", "Bearer realm=\"PAAA\"");
        httpServletResponse.setContentType("application/json");

        try {

            // XML-Ausgabe mit JAXB
            if (format.equals("xml")) {

                try {

                    JAXBContext context = JAXBContext.newInstance(RequestError.class);
                    Marshaller m = context.createMarshaller();
                    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

                    // Write to HttpResponse
                    httpServletResponse.setContentType("application/xml;charset=UTF-8");
                    m.marshal(requestError, httpServletResponse.getWriter());
                } catch (JAXBException e) {
                    this.logger.error(e.getMessage(), e.getCause());
                    httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: Error while rendering the results.");
                }
            }

            // JSON-Ausgabe mit Jackson
            if (format.equals("json")) {

                httpServletResponse.setContentType("application/json;charset=UTF-8");
                mapper.writeValue(httpServletResponse.getWriter(), requestError);
            }
        }
        catch (Exception e) {

            e.printStackTrace();
        }
    }
}

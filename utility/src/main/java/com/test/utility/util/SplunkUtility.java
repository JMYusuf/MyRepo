package com.test.utility.util;

import com.splunk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SplunkUtility {
    private static final Logger LOG = LoggerFactory.getLogger(SplunkUtility.class);

    //Config Details
    private static final String SPLUNK_URL = "https://server.xyz.com:8000";
    private static final String HOST = "server.xyz.com";
    //private static final String ALIAS_HOST = "splunk.xyz.com";
    private static final String PROTOCOL = "https";
    //private static final int PORT = 8000; //Server Port
    private static final int PORT = 8089;   //splunkd Management Port
    private static final String SPLUNK_USER = "TestUser";
    private static final String SPLUNK_PWD = "Test#123";

    //Param Details
    public static final String EARLIEST_TIME_PARAM_NAME = "earliest_time";
    public static final String LATEST_TIME_PARAM_NAME = "latest_time";
    public static final String OUTPUT_MODE_PARAM_NAME = "output_mode";

    private static final int THREAD_WAIT_TIME_IN_MS = 500;
    private static final int BUFFER_SIZE = 4096;
    private static final int ERROR_MIN_LENGTH = 85;
    private static final int DELAY_SECONDS = 3 * 1000;

    public static final String OUTPUT_MODE_JSON = "json";
    //Last 15 minutes to search from current time
    public static final String EARLIEST_TIME = "-15m";
    public static final String LATEST_TIME = "now";
    private static final String LAST_HOURS_TO_SEARCH = "-24h";

    //Splunk Search Details
    public static final String SEARCH_ID_VAL_NAME = "<<SearchIdValue>>";
    public static final String SEARCH_ID_VAL = "123456789";
    private static final String SEARCH_QUERY = "search index=test host=hostname* sourcetype=officeonline error <<SearchIdValue>> | head 1";

    private static Service service = null;

    public static Service getService() throws Exception{
            if(service == null){
                LOG.error("getService :: Trying to get Splunk Service before connection is established!");
                throw new Exception("Trying to get Splunk Service before connection is established!");
            }
        return service;
    }

    public static synchronized Service connect(String host, int port, String userName, String pwd, String protocol) throws Exception {
        try {
            Map<String, Object> connectionArgs = new HashMap<String, Object>();
            connectionArgs.put("host", host);
            connectionArgs.put("port", port);
            connectionArgs.put("username", userName);
            connectionArgs.put("password", pwd);
            connectionArgs.put("scheme", protocol);

         /*As per instruction in https://answers.splunk.com/answers/395432/connecting-to-splunk-enterprise-using-splunk-sdk-f.html
         Overriding the static method setSslSecurityProtocol to implement the security protocol of choice
         or Give -Dhttps.protocols=TLSv1.2 in VM args for local testing and put the below entry in manifest-dev.yml for pcf deployment
         env:
            JBP_CONFIG_JAVA_MAIN: '{ arguments: "-Dhttps.protocols=TLSv1.2" }'
         */
         HttpService.setSslSecurityProtocol(SSLSecurityProtocol.TLSv1_2);
            if(service == null){
                service = Service.connect(connectionArgs);
            }
            LOG.info("connect :: Successfully connected to Splunk");
        } catch (Exception ex) {
            LOG.error("connect :: Error encountered while trying to connect to Splunk!", ex);
            throw ex;
        }
        return service;
    }

    public static void disconnect() throws Exception{
        try{
            if(service == null){
                LOG.error("disconnect :: Trying to disconnect before connection is established!");
                throw new Exception("Trying to disconnect before connection is established!");
            }
            service.logout();
        } catch(Exception ex){
            LOG.error("getService :: Error encountered while trying to disconnect from Splunk!", ex);
            throw ex;
        }
    }

    public static String search(Service service, String searchQuery, String earliestTime, String latestTime, String outputMode) throws Exception{
        String searchResults = "";
        Job job = null;
        Args outputArgs = null;
        int noOfTimes = 0;
        try{
                JobArgs jobargs = new JobArgs();
                jobargs.put(EARLIEST_TIME_PARAM_NAME, earliestTime);
                jobargs.put(LATEST_TIME_PARAM_NAME, latestTime);

                job = service.getJobs().create(searchQuery, jobargs);

                // Wait for the search to finish
                while (!job.isDone()) {
                    try {
                        Thread.sleep(THREAD_WAIT_TIME_IN_MS);
                    } catch (InterruptedException ie) {
                        LOG.error("search :: Error encountered during search!", ie);
                        throw ie;
                    }
                }

                outputArgs = new Args();
                outputArgs.put(OUTPUT_MODE_PARAM_NAME, outputMode);
                byte[] buffer;

                try(InputStream inputStream =  job.getResults(outputArgs)){
                    buffer = new byte[BUFFER_SIZE];
                    while(inputStream.read(buffer) != -1){
                        searchResults = new String(buffer);
                    }
                } catch(IOException ioe){
                    LOG.error("search :: Error encountered while reading search results!", ioe);
                }

                if(searchResults != null){
                    searchResults = searchResults.trim();
                }

                LOG.info("search :: Search Query:{}", searchQuery);
        } catch(Exception ex){
            LOG.error("search :: Error encountered while searching!", ex);
            throw ex;
        }
        return searchResults;
    }

    public static void main(String[] args) {
        try{
            Service service = SplunkUtility.connect(HOST, PORT, SPLUNK_USER, SPLUNK_PWD, PROTOCOL);
            long startTime = System.currentTimeMillis();

            String searchQuery = SEARCH_QUERY.replace(SEARCH_ID_VAL_NAME, SEARCH_ID_VAL);
            System.out.println("Search Query:" + searchQuery);

            SplunkUtility.search(service, searchQuery, EARLIEST_TIME, LATEST_TIME, OUTPUT_MODE_JSON);
            SplunkUtility.disconnect();
            System.out.println("Time for Search in (ms):" + (System.currentTimeMillis() - startTime));
        } catch(Exception ex){
            ex.printStackTrace();
        }
    }
}

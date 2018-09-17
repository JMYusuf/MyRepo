package com.test.utility.util

import com.splunk.Args
import com.splunk.Job
import com.splunk.JobArgs
import com.splunk.JobCollection
import com.splunk.Service
import com.test.utility.util.SplunkUtility
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Specification

class SplunkUtilitySpec extends Specification {

    SplunkUtility splunkUtility
    Service service

    def setup() {
        splunkUtility = new SplunkUtility()
        service = Mock()
        ReflectionTestUtils.setField(splunkUtility, "service", service)
    }

    def connect() {
        given:
        String host = "host"
        int port = 0
        String userName = "userName"
        String pwd = "pwd"
        String protocol = "protocol"
        Map<String, Object> connectionArgs = new HashMap<String, Object>()

        def svc = GroovyMock(Service) {
            connect(connectionArgs) >> service
        }

        when:
        def response = splunkUtility.connect(host, port, userName, pwd, protocol)

        then:
        response
    }

    def disconnect() {
        given:

        when:
        def response = splunkUtility.disconnect()

        then:
        0 * SplunkUtility.getService() >> service
        0 * Service.logout()
    }

    def search() {
        given:
        String searchQuery = "searchQuery"
        String earliestTime = "earliestTime"
        String latestTime = "latestTime"
        String outputMode = "outputMode"
        String searchResult = "searchResult"

        JobArgs jobargs = new JobArgs();
        jobargs.put("earliest_time", earliestTime);
        jobargs.put("latest_time", latestTime);

        Args outputArgs = new Args();
        outputArgs.put("output_mode", outputMode)

        Service service = Mock()
        Job job = Mock()
        byte[] buffer = 'Sample search result which this test method expects from search method in SplunkUtility class!'.bytes
        InputStream inputStream = new ByteArrayInputStream(buffer)
        JobCollection jobCollection = Mock()

        when:
        def response = splunkUtility.search(service, searchQuery, earliestTime, latestTime, outputMode)

        then:
        2 * service.getJobs() >> jobCollection
        1 * service.getJobs().create(searchQuery, jobargs) >> job
        1 * job.isDone() >> true
        1 * job.getResults(outputArgs) >> inputStream
        response
        response.size() == buffer.size()
    }
}

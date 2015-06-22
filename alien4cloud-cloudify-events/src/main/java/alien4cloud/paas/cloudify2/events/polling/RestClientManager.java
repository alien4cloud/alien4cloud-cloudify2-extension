package alien4cloud.paas.cloudify2.events.polling;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.springframework.stereotype.Component;

import alien4cloud.paas.cloudify2.events.Execption.RestEventException;

@Setter
@Getter
@Component
public class RestClientManager {

    private static final Logger log = Logger.getLogger(RestClientManager.class.getName());

    private static final String CDFY_USERNAME = "cdfy.username";
    private static final String CDFY_PASSWORD = "cdfy.password";

    private String version = "2.7.1";
    private String username;
    private String password;
    private String url = "http://localhost:8100";
    private boolean isManagerSecrured = false;
    private RestClient restClient;

    @PostConstruct
    private void PostConstruct() {
        String springProfile = System.getenv("SPRING_PROFILES_ACTIVE");
        String[] profiles = null;
        if (springProfile != null && (profiles = springProfile.split(",")) != null) {
            isManagerSecrured = ArrayUtils.contains(profiles, "secure");
        }

        if (isManagerSecrured) {
            log.info("Manager is secured");
            username = System.getProperty(CDFY_USERNAME);
            password = System.getProperty(CDFY_PASSWORD);
            url = "https://localhost:8100";
        }
    }

    @SneakyThrows(IOException.class)
    public RestClient getRestClient() throws RestEventException {
        if (restClient == null) {
            try {
                restClient = new RestClient(new URL(url), username, password, version);
                if (StringUtils.isNoneBlank(username, password)) {
                    restClient.connect();
                }
            } catch (RestClientException e) {
                String msg = "Fail to create a Cloudify Rest Client. \n" + e.getMessageFormattedText();
                log.severe(msg);
                throw new RestEventException(msg, e);
            }
        }
        return restClient;
    }
}

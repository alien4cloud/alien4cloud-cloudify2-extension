package alien4cloud.paas.cloudify2.events.notify;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.springframework.stereotype.Component;

import alien4cloud.paas.cloudify2.events.Execption.RestEventException;

@Setter
@Getter
@Component
public class RestClientManager {

    private static final Logger log = Logger.getLogger(RestClientManager.class.getName());

    private String version = "2.7.1";
    private String username;
    private String password;
    private RestClient restClient;

    @SneakyThrows(IOException.class)
    public RestClient getRestClient() throws RestEventException {
        if (restClient == null) {
            try {
                restClient = new RestClient(new URL("http://localhost:8100"), username, password, version);
            } catch (RestClientException e) {
                String msg = "Fail to create a Cloudify Rest Client. \n" + e.getMessageFormattedText();
                log.severe(msg);
                throw new RestEventException(msg, e);
            }
        }
        return restClient;
    }
}

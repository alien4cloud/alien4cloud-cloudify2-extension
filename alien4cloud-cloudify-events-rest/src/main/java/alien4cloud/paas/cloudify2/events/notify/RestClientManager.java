package alien4cloud.paas.cloudify2.events.notify;

import java.io.IOException;
import java.net.URL;

import lombok.Getter;
import lombok.Setter;

import org.cloudifysource.restclient.RestClient;
import org.cloudifysource.restclient.exceptions.RestClientException;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
public class RestClientManager {
    private String version = "2.7.1";
    private String username;
    private String password;
    private RestClient restClient;

    public RestClient getRestClient() throws IOException, RestClientException {
        if (restClient == null) {
            restClient = new RestClient(new URL("http://localhost:8100"), username, password, version);
        }
        return restClient;
    }
}

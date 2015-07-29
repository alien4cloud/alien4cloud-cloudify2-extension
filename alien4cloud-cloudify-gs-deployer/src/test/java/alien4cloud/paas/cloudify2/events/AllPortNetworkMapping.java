package alien4cloud.paas.cloudify2.events;

import com.gigaspaces.lrmi.INetworkMapper;
import com.gigaspaces.lrmi.ServerAddress;

/**
 * @author Minh Khang VU
 */
public class AllPortNetworkMapping implements INetworkMapper {

    @Override
    public ServerAddress map(ServerAddress serverAddress) {
        if (serverAddress.getHost().equals("177.86.0.112")) {
            return new ServerAddress("129.185.67.65", serverAddress.getPort());
        } else if (serverAddress.getHost().equals("177.86.0.114")) {
            return new ServerAddress("129.185.67.67", serverAddress.getPort());
        } else {
            return serverAddress;
        }
    }
}

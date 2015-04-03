package alien4cloud.paas.cloudify2.events;

import lombok.Getter;
import lombok.Setter;

import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;

/**
 * Contains the latest instance state of a node in a TOSCA topology.
 */
@Setter
@Getter
public class NodeInstanceState {
    private String topologyId;
    private String nodeTemplateId;
    private String instanceId;
    private String instanceState;

    @SpaceRouting
    @SpaceId(autoGenerate = false)
    public String getId() {
        if (topologyId == null || nodeTemplateId == null || instanceId == null) {
            return null;
        }
        return topologyId + "-" + nodeTemplateId + "-" + instanceId;
    }

    public void setId(String id) {
    }

}
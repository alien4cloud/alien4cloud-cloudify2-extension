package alien4cloud.paas.cloudify2.events;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class RelationshipOperationEvent extends AlienEventDescription {
    private String event;
    private String relationshipId;
    private String source;
    private String target;
    private String sourceService;
    private String targetService;
    private String commandName;
    private boolean executed = false;
    private boolean success = false;
    private List<String> parameters = new ArrayList<String>();
}

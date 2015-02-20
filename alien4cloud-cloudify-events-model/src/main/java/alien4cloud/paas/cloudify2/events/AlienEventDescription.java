package alien4cloud.paas.cloudify2.events;

import java.util.Date;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;

@Setter
@NoArgsConstructor
@ToString
public abstract class AlienEventDescription {

    private String id;

    @Getter
    protected Integer eventIndex;
    @Getter
    protected String applicationName;
    @Getter
    protected String serviceName;
    @Getter
    protected String instanceId;
    @Getter
    protected String deploymentId;
    @Getter
    protected Date dateTimestamp;

    @SpaceRouting
    @SpaceId(autoGenerate = true)
    private String getId() {
        return id;
    }
}

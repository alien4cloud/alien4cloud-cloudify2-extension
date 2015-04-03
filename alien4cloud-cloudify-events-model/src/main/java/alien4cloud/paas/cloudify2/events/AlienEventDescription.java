package alien4cloud.paas.cloudify2.events;

import java.util.Date;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;

@Setter
@Getter
@NoArgsConstructor
@ToString
public abstract class AlienEventDescription {

    private String id;
    private Integer routing = 1;
    private Integer eventIndex;
    private String applicationName;
    private String serviceName;
    private String instanceId;
    private String deploymentId;
    private Date dateTimestamp;

    @SpaceRouting
    public Integer getRouting() {
        return routing;
    }

    @SpaceId(autoGenerate = true)
    private String getId() {
        return id;
    }
}

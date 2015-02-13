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
    private Integer eventIndex;
    @Getter
    private String applicationName;
    @Getter
    private String serviceName;
    @Getter
    private String instanceId;
    @Getter
    private String deploymentId;
    @Getter
    private Date dateTimestamp;

    @SpaceRouting
    @SpaceId(autoGenerate = true)
    private String getId() {
        return id;
    }
}

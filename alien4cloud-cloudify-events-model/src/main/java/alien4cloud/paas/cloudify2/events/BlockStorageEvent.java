package alien4cloud.paas.cloudify2.events;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class BlockStorageEvent extends AlienEvent {

    private String volumeId;

    public BlockStorageEvent() {
    }

}

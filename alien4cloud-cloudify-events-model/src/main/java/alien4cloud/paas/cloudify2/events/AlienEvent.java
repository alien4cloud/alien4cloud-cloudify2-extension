package alien4cloud.paas.cloudify2.events;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Getter
@Setter
@ToString(callSuper = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes({ @Type(value = AlienEvent.class, name = EventType.INSTANCE_STATE), @Type(value = BlockStorageEvent.class, name = EventType.BLOCKSTORAGE) })
public class AlienEvent extends AlienEventDescription {

    private String event;

    public AlienEvent() {
    }
}

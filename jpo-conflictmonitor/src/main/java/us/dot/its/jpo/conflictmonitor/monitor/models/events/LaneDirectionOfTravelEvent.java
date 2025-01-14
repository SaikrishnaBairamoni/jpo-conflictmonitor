package us.dot.its.jpo.conflictmonitor.monitor.models.events;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.EqualsAndHashCode;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;



@Getter
@Setter
@EqualsAndHashCode(callSuper=true)
@Generated
public class LaneDirectionOfTravelEvent extends Event{
    private long timestamp;
    private int laneID;
    private int laneSegmentNumber;
    private double laneSegmentInitialLatitude;
    private double laneSegmentInitialLongitude;
    private double laneSegmentFinalLatitude;
    private double laneSegmentFinalLongitude;
    private double expectedHeading;
    private double medianVehicleHeading;
    private double medianDistanceFromCenterline;
    private int aggregateBSMCount;
    private String source;

    public LaneDirectionOfTravelEvent(){
        super("LaneDirectionOfTravel");
    }

    @JsonIgnore
    public String getKey(){
        return this.getIntersectionID() + "";
    }
}

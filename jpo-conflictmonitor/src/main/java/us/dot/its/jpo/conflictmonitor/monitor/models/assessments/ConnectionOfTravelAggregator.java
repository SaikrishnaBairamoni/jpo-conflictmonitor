package us.dot.its.jpo.conflictmonitor.monitor.models.assessments;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import us.dot.its.jpo.conflictmonitor.monitor.models.events.ConnectionOfTravelEvent;
import us.dot.its.jpo.geojsonconverter.DateJsonMapper;

public class ConnectionOfTravelAggregator {
    private ArrayList<ConnectionOfTravelEvent> events = new ArrayList<>();
    private long aggregatorCreationTime;
    private long messageDurationDays;

    

    public ConnectionOfTravelAggregator(){
        this.aggregatorCreationTime = ZonedDateTime.now().toInstant().toEpochMilli();
    }

    @JsonIgnore
    public ConnectionOfTravelAggregator add(ConnectionOfTravelEvent event){
        events.add(event);

        List<ConnectionOfTravelEvent> removeEvents = new ArrayList<>();
        
        for(ConnectionOfTravelEvent previousEvents: this.events){
            if(previousEvents.getTimestamp() + (messageDurationDays * 3600*1000) < event.getTimestamp()){
                removeEvents.add(previousEvents);
            }else{
                break;
            }
        }

        events.removeAll(removeEvents);
        return this;
    }

    @JsonIgnore
    public ConnectionOfTravelAssessment getConnectionOfTravelAssessment(){
        ConnectionOfTravelAssessment assessment = new ConnectionOfTravelAssessment();
        ArrayList<ConnectionOfTravelAssessmentGroup> assessmentGroups = new ArrayList<>();
        HashMap<String,ConnectionOfTravelAssessmentGroup> connectionGroupLookup = new HashMap<>(); // laneId, Segment Index
        int intersectionID = -1;
        int roadRegulatorID = -1;
        for(ConnectionOfTravelEvent event : this.events){
            String eventKey = getEventKey(event);
            intersectionID = event.getIntersectionID();
            roadRegulatorID = event.getRoadRegulatorID();
            ConnectionOfTravelAssessmentGroup connectionGroup = connectionGroupLookup.get(eventKey);
            if(connectionGroup == null){
                connectionGroup = new ConnectionOfTravelAssessmentGroup();
                connectionGroup.setIngressLaneID(event.getIngressLaneID());
                connectionGroup.setEgressLaneID(event.getEgressLaneID());
                connectionGroup.setConnectionID(event.getConnectionID());
                assessmentGroups.add(connectionGroup);
                connectionGroupLookup.put(eventKey,connectionGroup);
            }
            connectionGroup.addConnectionOfTravelEvent(event);
        }
        
        assessment.setConnectionOfTravelAssessmentGroups(assessmentGroups);
        assessment.setTimestamp(ZonedDateTime.now().toInstant().toEpochMilli());
        assessment.setIntersectionID(intersectionID);
        assessment.setRoadRegulatorID(roadRegulatorID);
        return assessment;
    }

    public String getEventKey(ConnectionOfTravelEvent event){
        return event.getIngressLaneID() + "-" + event.getEgressLaneID();
    }

    public ArrayList<ConnectionOfTravelEvent> getEvents() {
        return events;
    }

    public void setEvents(ArrayList<ConnectionOfTravelEvent> events) {
        this.events = events;
    }

    public long getAggregatorCreationTime() {
        return aggregatorCreationTime;
    }

    public void setAggregatorCreationTime(long aggregatorCreationTime) {
        this.aggregatorCreationTime = aggregatorCreationTime;
    }

    public long getMessageDurationDays() {
        return messageDurationDays;
    }

    public void setMessageDurationDays(long messageDurationDays) {
        this.messageDurationDays = messageDurationDays;
    }

    @Override
    public String toString() {
        ObjectMapper mapper = DateJsonMapper.getInstance();
        String testReturn = "";
        try {
            testReturn = (mapper.writeValueAsString(this));
        } catch (JsonProcessingException e) {
            System.out.println(e);
        }
        return testReturn;
    }
    
}
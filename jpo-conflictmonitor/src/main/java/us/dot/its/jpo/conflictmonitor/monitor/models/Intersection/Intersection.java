package us.dot.its.jpo.conflictmonitor.monitor.models.Intersection;

import java.util.ArrayList;

import us.dot.its.jpo.geojsonconverter.pojos.geojson.map.MapFeature;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.map.MapFeatureCollection;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

public class Intersection {
    
    private ArrayList<Lane> ingressLanes;
    private ArrayList<Lane> egressLanes;
    private ArrayList<StopLine> stopLines;
    
    private Coordinate referencePoint;




   



    





    private int intersectionId;

    public static Intersection fromMapFeatureCollection(MapFeatureCollection map){

        Intersection intersection = new Intersection();
        ArrayList<Lane> ingressLanes = new ArrayList<>();
        ArrayList<Lane> egressLanes = new ArrayList<>();

        if(map.getFeatures().length > 0){
            double[] referencePoint = map.getFeatures()[0].getGeometry().getCoordinates()[0];
            intersection.setReferencePoint(new Coordinate(referencePoint[0], referencePoint[1]));
        }else{
            System.out.println("Cannot Build Intersection from MapFeatureCollection. Feature collection has no Features.");
            return null;
        }
        

        for(MapFeature feature: map.getFeatures()){
            Lane lane = Lane.fromGeoJsonFeature(feature, intersection.getReferencePoint());
            if(lane.getIngress()){
                ingressLanes.add(lane);
            }else{
                egressLanes.add(lane);
            }
        }

        intersection.setIngressLanes(ingressLanes);
        intersection.setEgressLanes(egressLanes);

        

        
        return intersection; 
    }


    public Intersection(){
        
    }

    public void updateStopLines(){
        this.stopLines = new ArrayList<StopLine>();
        for(Lane lane : this.ingressLanes){
            StopLine line = StopLine.fromIngressLane(lane);
            if(line != null){
                this.stopLines.add(line);
            }
        }
    }

    public ArrayList<Lane> getIngressLanes() {
        return ingressLanes;
    }

    public void setIngressLanes(ArrayList<Lane> ingressLanes) {
        this.ingressLanes = ingressLanes;
        this.updateStopLines(); // automatically update Stop Line Locations when new ingress Lanes are assigned.
    }

    public ArrayList<Lane> getEgressLanes() {
        return egressLanes;
    }

    public void setEgressLanes(ArrayList<Lane> egressLanes) {
        this.egressLanes = egressLanes;
    }

    public int getIntersectionId() {
        return intersectionId;
    }

    public void setIntersectionId(int intersectionId) {
        this.intersectionId = intersectionId;
    }

    public ArrayList<StopLine> getStopLines() {
        return stopLines;
    }

    public void setStopLines(ArrayList<StopLine> stopLines) {
        this.stopLines = stopLines;
    }

    public Coordinate getReferencePoint() {
        return referencePoint;
    }


    public void setReferencePoint(Coordinate referencePoint) {
        this.referencePoint = referencePoint;
    }




    @Override
    public String toString(){
        return "Intersection: " + this.getIntersectionId() + " IngressLanes: " + this.getIngressLanes().size() + " Egress Lanes: " + this.getEgressLanes().size();
    }

}

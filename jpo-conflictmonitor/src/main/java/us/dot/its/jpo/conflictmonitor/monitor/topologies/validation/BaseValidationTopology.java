package us.dot.its.jpo.conflictmonitor.monitor.topologies.validation;

import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.internals.TimeWindow;

import us.dot.its.jpo.conflictmonitor.monitor.algorithms.BaseStreamsTopology;
import us.dot.its.jpo.conflictmonitor.monitor.models.events.ProcessingTimePeriod;
import us.dot.its.jpo.conflictmonitor.monitor.models.events.minimum_data.MinimumDataEvent;
import us.dot.its.jpo.geojsonconverter.partitioner.RsuIntersectionKey;
import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;

/**
 * Common code for {@link MapValidationTopology} and {@link SpatAssessmentsTopoloby}
 */
public abstract class BaseValidationTopology<TParams>
    extends BaseStreamsTopology<TParams> {
    
    protected void populateMinDataEvent(
            RsuIntersectionKey key,
            MinimumDataEvent minDataEvent,
            List<ProcessedValidationMessage> valMsgList,
            int rollingPeriodSeconds,
            long timestamp) {

        List<String> validationMessages = 
            valMsgList
                .stream()
                .map(valMsg -> String.format("%s (%s)", valMsg.getMessage(), valMsg.getSchemaPath()))
                .collect(Collectors.toList());

        minDataEvent.setMissingDataElements(validationMessages);
        minDataEvent.setIntersectionId(key.getIntersectionId());
        minDataEvent.setSourceDeviceId(key.getRsuId());

        // Get the time window this event would be in without actually performing windowing
        // we just need to add the window timestamps to the event.

        // Use a tumbling window with no grace to avoid duplicates
        var timeWindows = TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(rollingPeriodSeconds));

        // Gets a map of all time windows this instant could be in 
        Map<Long, TimeWindow> windows = timeWindows.windowsFor(timestamp);

        // Pick one (random map entry, but there should only be one for the tumbling window)
        TimeWindow window = windows.values().stream().findAny().orElse(null);                
        if (window != null) {
            var timePeriod = new ProcessingTimePeriod();
            timePeriod.setBeginTimestamp(window.startTime().toEpochMilli());
            timePeriod.setEndTimestamp(window.endTime().toEpochMilli());
            minDataEvent.setTimePeriod(timePeriod);
        }
    }

    
}
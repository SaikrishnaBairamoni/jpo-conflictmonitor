package us.dot.its.jpo.conflictmonitor.monitor.models.notifications.broadcast_rate;

import us.dot.its.jpo.conflictmonitor.monitor.models.events.broadcast_rate.BroadcastRateEvent;
import us.dot.its.jpo.conflictmonitor.monitor.models.notifications.Notification;

public abstract class BroadcastRateNotification<T extends BroadcastRateEvent> extends Notification {

    public BroadcastRateNotification(String notificationType) {
        super(notificationType);
    }

    private T event;
    
    @Override
    public String uniqueId() {
        return String.format("%s_%s_%s_%s_%s", 
            this.getNotificationType(), 
            event.getSourceDeviceId(), 
            event.getIntersectionId(), 
            event.getTimePeriod() != null ? event.getTimePeriod().periodMillis() : 0L,
            event.getNumberOfMessages());
    }
}

package com.grb.transport;

public interface TransportEventListener {
    /**
     * Callback for transport layer events.
     * 
     * @param event The transport event.
     */
    public void onTransportEvent(TransportEvent event);
}

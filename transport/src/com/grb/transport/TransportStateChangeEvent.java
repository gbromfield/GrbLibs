package com.grb.transport;


public class TransportStateChangeEvent implements TransportEvent {
    
    protected TransportClient mClient;
    protected TransportState mOldState;
    protected TransportState mNewState;
    protected TransportOperation mOperation;
    protected Object mReason;

    public TransportStateChangeEvent(TransportClient client, TransportState oldState,
            TransportState newState, TransportOperation operation, Object error) {
        this(client, oldState, newState, operation, false, error);
    }
    
    public TransportStateChangeEvent(TransportClient client, TransportState oldState,
        TransportState newState, TransportOperation operation, boolean closedByApp, Object reason) {
        mClient = client;
        mOldState = oldState;
        mNewState = newState;
        mOperation = operation;
        mReason = reason;
    }

    public TransportClient getTransportClient() {
        return mClient;
    }

    public TransportState getOldState() {
        return mOldState;
    }

    public TransportState getNewState() {
        return mNewState;
    }

    public TransportOperation getOperation() {
        return mOperation;
    }

    public Object getReason() {
        return mReason;
    }

    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("StateChangeEvent=[");
        bldr.append("client=");
        bldr.append(mClient);
        bldr.append(", from: ");
        bldr.append(mOldState);
        bldr.append(", to: ");
        bldr.append(mNewState);
        bldr.append(", operation: ");
        bldr.append(mOperation);
        if (mReason != null) {
            bldr.append(", reason: ");
            bldr.append(mReason);
        }
        bldr.append("]");
        return bldr.toString();
    }
}

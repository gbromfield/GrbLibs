package com.grb.service.listener;

public interface VetoableListenerCallback<L,E> {
    public void onVetoableListenerCallback(L listener, E event, Object userData) throws ListenerVetoException;
}

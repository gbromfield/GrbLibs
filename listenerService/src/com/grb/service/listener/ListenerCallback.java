package com.grb.service.listener;

public interface ListenerCallback<L,E> {
	public void onListenerCallback(L listener, E event, Object userData);
}

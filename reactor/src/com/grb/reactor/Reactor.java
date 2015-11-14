package com.grb.reactor;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;

public interface Reactor {
    public void addAccept(SelectableChannel channel, ReactorHandler handler) throws ClosedChannelException;
    public void addConnect(SelectableChannel channel, ReactorHandler handler) throws ClosedChannelException;
    public void addRead(SelectableChannel channel, ReactorHandler handler) throws ClosedChannelException;
    public void addWrite(SelectableChannel channel, ReactorHandler handler) throws ClosedChannelException;

    public void close() throws Exception;
    public Selector getSelector();

    public void removeAccept(SelectableChannel channel, ReactorHandler handler) throws ClosedChannelException;
    public void removeConnect(SelectableChannel channel, ReactorHandler handler) throws ClosedChannelException;
    public void removeRead(SelectableChannel channel, ReactorHandler handler) throws ClosedChannelException;
    public void removeWrite(SelectableChannel channel, ReactorHandler handler) throws ClosedChannelException;
}

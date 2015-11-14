package com.grb.dependencyGraph;

public interface DependencyNodeListener {
    public void onStateChange(DependencyNode node, DependencyNode.State oldState, DependencyNode.State newState);
}

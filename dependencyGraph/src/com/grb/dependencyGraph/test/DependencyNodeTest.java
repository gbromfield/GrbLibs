package com.grb.dependencyGraph.test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.grb.dependencyGraph.DependencyNode;
import com.grb.dependencyGraph.DependencyNodeListener;

public class DependencyNodeTest extends DependencyNode {
    
    private int mSleepTime;
    
    public DependencyNodeTest(String name,
            ThreadPoolExecutor executor, 
            DependencyNodeListener listener, 
            CountDownLatch countDownLatch, 
            int sleepTime) {
        super(name, executor, null, listener, countDownLatch);
        mSleepTime = sleepTime;
    }

    public void run() {
        changeState(State.Running);
        try {
            Thread.sleep(mSleepTime);
        } catch(Exception e) {
            e.printStackTrace();
        }
        if (mSleepTime == 10000) {
            signalDone(State.Failure);  // simulate a failure when sleeping for exactly 10s
        } else {
            signalDone(State.Success);
        }
    }

    static public void main(String[] args) {
        ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(100);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 100, TimeUnit.SECONDS, queue);
        
        DependencyNodeListener listener = new DependencyNodeListener() {
            public void onStateChange(DependencyNode node, State oldState, State newState) {
                System.out.println("Node: " + node + ", oldState=" + oldState + ", newState=" + newState);
            }
        };
        CountDownLatch latch = new CountDownLatch(1);
        DependencyNode root = new DependencyNode("root", executor, null, listener, null);
        DependencyNode terminal = new DependencyNode("2-1", executor, null, listener, latch);
        for (int i = 0; i < 5; i++) {
            DependencyNode child = null;
            if (i == 3) {
                child = new DependencyNodeTest("1-" + (i+1), executor, listener, null, 10000);
            } else {
                child = new DependencyNodeTest("1-" + (i+1), executor, listener, null, 5000);
            }
            root.addChild(child);
            child.addChild(terminal);
        }
        System.out.println("Paths \n" + root.toPathString());
        root.start();
        try {
            latch.await();
        } catch(Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}

package se.alexanderblom.imageloader.util;

import java.util.concurrent.ThreadFactory;

import android.os.Process;

public class PriorityThreadFactory implements ThreadFactory {
    private int priority;

    public PriorityThreadFactory(int priority) {
        this.priority = priority;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r) {
            @Override
            public void run() {
                Process.setThreadPriority(priority);

                super.run();
            }
        };
    }
}

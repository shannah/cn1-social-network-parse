/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.demos.parse_social;

import com.codename1.ui.Display;
import com.codename1.ui.util.UITimer;
import java.util.LinkedList;

/**
 *
 * @author shannah
 */
public class OperationQueue {
    private Runnable task = null;
    private int delay;
    
    public OperationQueue(int delay) {
        this.delay = delay;
    }
    
    public void run(final Runnable r) {
        this.task = r;
        UITimer timer = new UITimer(() -> {
            if (r == this.task) {
                r.run();
            }
        });
        timer.schedule(delay, false, Display.getInstance().getCurrent());
    }
}

/* Copyright (C) 2012 Benjamin Wulff
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package cv.lecturesight.heartbeat;

import cv.lecturesight.heartbeat.api.HeartBeat;
import cv.lecturesight.framesource.FrameSource;
import cv.lecturesight.framesource.FrameSourceException;
import cv.lecturesight.framesource.FrameSourceProvider;
import cv.lecturesight.opencl.OpenCLService;
import cv.lecturesight.opencl.api.OCLSignal;
import cv.lecturesight.opencl.api.OCLSignalBarrier;
import cv.lecturesight.opencl.api.Triggerable;
import cv.lecturesight.util.Log;
import cv.lecturesight.util.conf.Configuration;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;

/** Implementation of Service API
 *
 */
@Component(name = "lecturesight.heartbeat", immediate = true)
@Service
public class HeartBeatImpl implements HeartBeat {

  static final String PROPKEY_LISTENTO = "listens.to";
  static final String PROPKEY_AUTOSTART = "autostart";
  private Log log = new Log("Heartbeat");
  @Reference
  private Configuration config;
  @Reference
  private OpenCLService ocl;
  @Reference
  private FrameSourceProvider fsp;
  private FrameSource fsrc;
  private OCLSignal[] listenTo;
  private OCLSignalBarrier barrier;
  private OCLSignal sig_BEGINFRAME;
  private boolean ready = false;
  private int iterationsToRun = 0;

  protected void activate(ComponentContext cc) throws Exception {
    sig_BEGINFRAME = ocl.getSignal("BEGIN-FRAME");
    log.info("Activated.");
    final int autostart = config.getInt(PROPKEY_AUTOSTART);
    if (autostart > 0) {
      init();
      log.info("Autostart in " + autostart + "ms...");
      
      new Thread(new Runnable() {

        @Override
        public void run() {
          try {
            Thread.sleep(autostart);
          } catch (InterruptedException e) {
            // interrupted
          }
          iterationsToRun = -1;
          nextFrame();
        }
        
      }).start();
    }
  }
  
  protected void deactivate(ComponentContext cc) throws Exception {
    if (ready) {
      deinit();
    }
    log.info("Deactivated");
  }

  private void nextFrame() {
    try {
      ocl.castSignal(sig_BEGINFRAME);
      fsrc.captureFrame();
    } catch (FrameSourceException e) {
      log.error("Unable to capture frame", e);
    }
  }

  @Override
  public void init() {
    fsrc = fsp.getFrameSource();                         // get configured FrameSource

    // get list of signals we are listening to
    String[] listenSignals = config.getList(PROPKEY_LISTENTO);
    if (listenSignals.length < 1) {
      log.warn("No signals to listen to.");
      return;
    }
    listenTo = new OCLSignal[listenSignals.length];
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < listenSignals.length; i++) {
      String signame = listenSignals[i].trim();
      sb.append(signame).append(" ");
      listenTo[i] = ocl.getSignal(signame);
    }
    log.info("Listening to: " + sb.toString());

    // create signal barrier
    barrier = ocl.createSignalBarrier(listenTo);
    ocl.registerTriggerable(barrier.getSignal(),
            new Triggerable() {

              @Override
              public void triggered(OCLSignal signal) {   // when all signals have arrived
                if (iterationsToRun != 0) {
                  nextFrame();                              // accquire next frame
                  if (iterationsToRun > 0) {
                    iterationsToRun--;
                  }
                }
              }
            });

    ready = true;
    log.info("Initialized");
  }

  @Override
  public void deinit() {
    iterationsToRun = 0;
    for (int i = 0; i < listenTo.length; i++) {
      ocl.unregisterTriggerable(listenTo[i], barrier);
    }
    fsrc = null;
    ready = false;
    log.info("Stopped");
  }

  @Override
  public void step(int i) {
    if (ready) {
      iterationsToRun = i;
      nextFrame();
    } else {
      throw new IllegalStateException("Cannot step, HeartBeat not initialized");
    }
  }

  @Override
  public void go() {
    if (ready) {
      iterationsToRun = -1;
      nextFrame();
      log.info("Started");
    } else {
      throw new IllegalStateException("Cannot start, HeartBeat not initialized");
    }
  }

  @Override
  public void stop() {
    iterationsToRun = 0;
  }

  @Override
  public boolean isReady() {
    return ready;
  }

  @Override
  public boolean isRunning() {
    return !(iterationsToRun == 0);
  }
}

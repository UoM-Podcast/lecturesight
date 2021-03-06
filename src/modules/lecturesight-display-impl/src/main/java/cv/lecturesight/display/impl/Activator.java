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
package cv.lecturesight.display.impl;

import cv.lecturesight.display.DisplayService;
import cv.lecturesight.opencl.OpenCLService;
import cv.lecturesight.util.Log;
import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator, ServiceListener {

  final static String serviceFilter = "(objectClass=" + OpenCLService.class.getName() + ")";
  
  private Log log = new Log("Display Bundle Activator");
  
  private BundleContext bc;
  private DisplayServiceFactory serviceFactory;
  private ServiceRegistration serviceReg = null;
  
  @Override
  public void start(BundleContext bc) throws Exception {
    this.bc = bc;
    log.info("Starting");
    // try to obtain OpenCLService in case it was registered before this
    ServiceReference oclRef = bc.getServiceReference(OpenCLService.class.getName());
    if (oclRef != null) {
      activateService((OpenCLService)bc.getService(oclRef));
    }
    bc.addServiceListener(this, serviceFilter);
  }

  private void activateService(OpenCLService ocl) {
    log.info("Registering Display Service Factory");
    if (serviceReg == null) {
      serviceFactory = new DisplayServiceFactory(bc);
      serviceFactory.setOpenCL(ocl);
      Dictionary<String,Object> props = new Hashtable<String,Object>();
      props.put("service.description", "Display Service Factory");
      serviceReg = bc.registerService(DisplayService.class.getName(), serviceFactory, props);
    }
  }
  
  private void deactivateService() {
    if (serviceReg != null) {
      serviceFactory.deactivate();
      serviceReg.unregister();
    }
    log.info("Stopped.");
  }
  
  @Override
  public void stop(BundleContext bc) throws Exception {
    deactivateService();
  }

  @Override
  public void serviceChanged(ServiceEvent se) {
    switch (se.getType()) {
      case ServiceEvent.REGISTERED:
        log.debug("OpenCLService registered");
        OpenCLService ocl = (OpenCLService)bc.getService(se.getServiceReference());
        activateService(ocl);
        break;
      case ServiceEvent.UNREGISTERING:
        log.debug("OpenCLService unregistering");
        deactivateService();
        break;
    }
  }
  
}

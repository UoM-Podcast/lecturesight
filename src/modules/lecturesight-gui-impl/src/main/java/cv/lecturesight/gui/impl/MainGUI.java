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
package cv.lecturesight.gui.impl;

import cv.lecturesight.gui.api.UserInterface;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import cv.lecturesight.util.Log;
import cv.lecturesight.util.DummyInterface;

@Component(name="lecturesight.gui", immediate=true)
@Service
public class MainGUI implements DummyInterface {
  
  Log log = new Log("UI");
  UserInterfaceTracker uiTracker;
  MainGUIFrame window;
  
  protected void activate(ComponentContext cc) {
    log.info("Activated");
    window = new MainGUIFrame();
    window.setVisible(true);
    uiTracker = new UserInterfaceTracker(cc.getBundleContext());
    uiTracker.open();
  }
  
  protected void deactivate(ComponentContext cc) {
    uiTracker.close();
    log.info("Deactivated");
  }
  
  void install(UserInterface ui) {
    log.info("Installing interface: " + ui.getTitle());
    window.addServiceUI(ui);
  }
  
  void uninstall(UserInterface ui) {
    log.info("Uninstalling interface: " + ui.getTitle());
    window.removeServiceUI(ui);
  }
  
  private class UserInterfaceTracker extends ServiceTracker {
    
    public UserInterfaceTracker(BundleContext bc) {
      super(bc, UserInterface.class.getName(), null);
    }
    
    @Override
    public Object addingService(ServiceReference ref) {
      UserInterface ui = (UserInterface)context.getService(ref);
      install(ui);
      return ui;
    }
    
    @Override
    public void removedService(ServiceReference ref, Object so) {
      uninstall((UserInterface)so);
    }
  }
}

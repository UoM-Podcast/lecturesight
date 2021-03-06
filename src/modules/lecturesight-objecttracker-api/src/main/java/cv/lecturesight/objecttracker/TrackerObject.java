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
package cv.lecturesight.objecttracker;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author wulff
 */
public class TrackerObject {
  
  private static int nextId = 1;
  private int id = nextId++;
  private long firstSeen, lastSeen;
  private HashMap<String,Object> properties = new HashMap<String,Object>();
  
  public TrackerObject() {
    firstSeen = System.currentTimeMillis();
    lastSeen = firstSeen;
  }
  
  public TrackerObject(long time) {
    firstSeen = time;
    lastSeen = firstSeen;
  }

  public int getId() {
    return id;
  }
  
  public long firstSeen() {
    return firstSeen;
  }
  
  public void setLastSeen(long time) {
    lastSeen = time;
  }
  
  public long lastSeen() {
    return lastSeen;
  }
  
  public boolean hasProperty(String key) {
    return properties.containsKey(key);
  }
  
  public Object getProperty(String key) {
    return properties.get(key); 
  }
  
  public Map<String,Object> getProperties() {
    return (Map<String,Object>)properties.clone();
  }
  
  public void setProperty(String key, Object value) {
    properties.put(key, value);
  }
}

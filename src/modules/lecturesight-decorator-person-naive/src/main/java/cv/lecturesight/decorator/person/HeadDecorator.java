package cv.lecturesight.decorator.head;

import cv.lecturesight.object.ObjectDecorator;
import cv.lecturesight.object.TrackerObject;
import cv.lecturesight.util.Log;
import cv.lecturesight.util.conf.Configuration;
import cv.lecturesight.util.geometry.BoundingBox;
import cv.lecturesight.util.geometry.Position;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Random;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;

/** ObjectDecorator Service: Head finder
 *
 */
@Component(name = "lecturesight.decorator.head", immediate = true)
@Service
@Properties({
  @Property(name = "lecturesight.decorator.name", value = "Head Finder"),
  @Property(name = "lecturesight.decorator.callon", value = "EACHFRAME"),
  @Property(name = "lecturesight.decorator.produces", value = {"head.center", "head.boundingbox"})
})
public class HeadDecorator implements ObjectDecorator {

  final static String PROPKEY_CENTROID = "head.center";
  final static String PROPKEY_BBOX = "head.boundingbox";
  final static String PROPKEY_K = "k";
  final static String PROPKEY_MAXITER = "iterations.max";
  
  private Log log = new Log("Head Finder");
  @Reference
  Configuration config;
  private int PARAM_K = 7;
  private int MAX_ITER = 100;

  protected void activate(ComponentContext cc) throws Exception {
    PARAM_K = config.getInt(PROPKEY_K);
    MAX_ITER = config.getInt(PROPKEY_MAXITER);
    log.info("BrainzzZ!");
  }

  protected void deactivate(ComponentContext cc) throws Exception {
    log.info("Deactivated");
  }

  @Override
  public void examine(TrackerObject obj) {

    // Try to read the image
    try {
      BufferedImage image = obj.getVisual();
      WritableRaster r = image.getRaster();

      // We will store all points in this stack
      PointStack points = new PointStack();

      int width = image.getWidth();
      int height = image.getHeight();

      // get all points
      for (int i = 0; i < width; i++) {
        for (int j = 0; j < height; j++) {
          if (r.getSample(i, j, 0) > 0 && r.getSample(i, j, 1) > 0
                  && r.getSample(i, j, 2) > 0) {
            points.push(new Point(i, j));
          }
        }
      }

      Point gravity = new ClusterStack(points).get_center();

      // Cluster array
      ClusterStack[] clusters = new ClusterStack[PARAM_K];
      Random generator = new Random();

      for (int i = 0; i < PARAM_K; i++) {
        int index = generator.nextInt(points.length());
        clusters[i] = new ClusterStack(points.index(index));
      }

      int iterations = 0;
      boolean noChange = false;

      while (noChange == false && iterations < MAX_ITER) {
        // Reset clusters
        for (int i = 0; i < clusters.length; i++) {
          clusters[i].reset();
        }
        noChange = true;
        // check distance for all points
        for (int i = 0; i < points.length(); i++) {
          int shortest = 0;
          // distance between first cluster and actual point
          double distance = Helper.euclidean_distance(points.index(i),
                  clusters[0].get_center());
          // Compute distance to all clusters
          for (int j = 1; j < clusters.length; j++) {
            double distance_new = Helper.euclidean_distance(points.index(i),
                    clusters[j].get_center());
            if (distance_new < distance) {
              distance = distance_new;
              shortest = j;
            }
          }
          // push point to the nearest cluster
          clusters[shortest].push(points.index(i));
        }
        // recalculate the cluster centers
        for (int i = 0; i < clusters.length; i++) {
          if (clusters[i].recalculate_center()) {
            noChange = false;
          }
        }
        iterations++;
      }

      log.debug("iterations: " + iterations);

      double d = Double.MAX_VALUE;
      int optimal = 0;

      for (int i = 0; i < clusters.length; i++) {
        double x_distance = Math.pow((clusters[i].get_center().getPoint_x()
                - gravity.getPoint_x()), 2);
        double y_distance = Math.pow((clusters[i].get_center().getPoint_y()), 2);
        if (x_distance + y_distance < d) {
          d = x_distance + y_distance;
          optimal = i;
        }
      }
      Point[] boundaries = clusters[optimal].min_max();
      
      // save results to TackerObject
      obj.setProperty(PROPKEY_CENTROID, new Position((int)gravity.getPoint_x(), (int)gravity.getPoint_y()));
      obj.setProperty(PROPKEY_BBOX, new BoundingBox(
              new Position((int)boundaries[0].getPoint_x(), (int)boundaries[0].getPoint_y()),
              new Position((int)boundaries[1].getPoint_x(), (int)boundaries[1].getPoint_y())));
      
    } catch (Exception e) {
      log.error("Error in head finder!", e);
    }
  }
}
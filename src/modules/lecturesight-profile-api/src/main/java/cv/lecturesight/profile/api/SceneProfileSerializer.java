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
package cv.lecturesight.profile.api;

import cv.lecturesight.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author wulff
 */
public class SceneProfileSerializer {

  private static final String TAB = "\t";
  private static final String NEWLINE = "\n";
  private static final String COMMENT_INDICATOR = "#";

  private static Log log = new Log("Scene Profile Serializer");

  public static void serialize(SceneProfile profile, OutputStream os) throws ProfileSerializerException {
    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(os));

    try {
      // write some comment in first line
      out.append("# Generated by cv.lecturesight.profile.api.SceneProfileSerializer");

      // write name and description in second line
      out.append(sanitize(profile.name)).append(TAB).append(sanitize(profile.description)).append(NEWLINE);

      // write zones
      for (Zone zone : profile.zones) {
        out.append(zone.type.name()).append(TAB).append(sanitize(zone.name)).append(TAB)
                .append(Integer.toString(zone.x)).append(TAB)
                .append(Integer.toString(zone.y)).append(TAB)
                .append(Integer.toString(zone.width)).append(TAB)
                .append(Integer.toString(zone.height)).append(NEWLINE);
      }
      out.flush();
    } catch (IOException e) {
      String msg = "Failed to serialize SceneProfile. ";
      log.error(msg, e);
      throw new ProfileSerializerException(msg, e);
    }
  }

  public static String serialize(SceneProfile profile) throws ProfileSerializerException {
    String out = null;
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    serialize(profile, os);
    try {
      out = new String(os.toByteArray());
      os.close();
    } catch (IOException ex) {
      String msg = "Failed to convert OutputStream to String after serialization of Profile.";
      log.error(msg, ex);
      throw new ProfileSerializerException(msg, ex);
    }
    return out;
  }

  public static SceneProfile deserialize(InputStream is) throws ProfileSerializerException {
    SceneProfile profile = null;
    BufferedReader in = new BufferedReader(new InputStreamReader(is));

    int line_num = 0;
    try {

      String line = null;
      while ((line = in.readLine()) != null) {

        // parse every line that is not a comment line
        if (!line.trim().startsWith(COMMENT_INDICATOR)) {

          line_num++;
          String[] token = line.trim().split("\\t");

          // first line is profile name and description
          if (line_num == 0) {

            // make sure there is at least a name
            if (token.length == 0) {
              throw new ProfileSerializerException("First non-comment line in file must contain at least the profile name.");
            }
            String name = token[0];
            String desc = token.length > 1 ? token[1] : "";
            desc = desc.replaceAll("\\\\n", "\n");
            profile = new SceneProfile(name, desc);

            // line containing a zone
          } else {

            // make sure we have 5 fields in line, otherwise we can fail here already
            if (token.length != 6) {
              throw new ProfileSerializerException("Wrong format in line " + line_num + ". Format must be: type \\t name \\t x \\t y \\t width \\t height \\n");
            }

            Zone.Type type = Zone.Type.valueOf(token[0]);
            String name = token[1];
            int x = Integer.parseInt(token[2]);
            int y = Integer.parseInt(token[3]);
            int w = Integer.parseInt(token[4]);
            int h = Integer.parseInt(token[5]);

            Zone new_zone = new Zone(name, type, x, y, w, h);
            if (profile != null) {
              profile.putZone(new_zone);
            } else {
              throw new ProfileSerializerException("First non-comment line in file must contain at least the profile name.");
            }
          }
        }
      }

      is.close();

    } catch (IllegalArgumentException ex) {
      String msg = "Error while parsing value in line " + line_num + ". ";
      log.error(msg, ex);
      throw new ProfileSerializerException(msg, ex);

    } catch (IOException ex) {
      String msg = "Error while reading line " + line_num + ". ";
      log.error(msg, ex);
      throw new ProfileSerializerException(msg, ex);
    }

    return profile;
  }

  public static SceneProfile deserialize(String in) throws ProfileSerializerException {
    SceneProfile profile = null;
    ByteArrayInputStream is = new ByteArrayInputStream(in.getBytes(StandardCharsets.UTF_8));
    try {
      profile = deserialize(is);
      is.close();
    } catch (IOException ex) {
      String msg = "Failed to convert OutputStream to String after serialization of Profile.";
      log.error(msg, ex);
      throw new ProfileSerializerException(msg, ex);
    }
    return profile;
  }

  private static String sanitize(String in) {
    return in.replaceAll("\\t", "   ").replaceAll("\\n", "\\n");
  }
}

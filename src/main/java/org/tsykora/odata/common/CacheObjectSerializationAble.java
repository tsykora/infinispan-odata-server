package org.tsykora.odata.common;

import java.io.Serializable;

/**
 * Helper class for OData transfer experiments
 *
 * @author tsykora
 * @since 4.0
 */
public class CacheObjectSerializationAble implements Serializable {
   String keyx;
   String valuex;
   public CacheObjectSerializationAble(String keyx, String valuex) {
      this.keyx = keyx;
      this.valuex = valuex;
   }
   public String getKeyx() {
      return keyx;
   }
   public void setKeyx(String keyx) {
      this.keyx = keyx;
   }

   public String getValuex() {
      return valuex;
   }

   public void setValuex(String valuex) {
      this.valuex = valuex;
   }

   @Override
   public String toString() {
      return "[" + getKeyx() + "," + getValuex() + "]";
   }
}

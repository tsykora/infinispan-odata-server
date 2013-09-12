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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CacheObjectSerializationAble that = (CacheObjectSerializationAble) o;

        if (!keyx.equals(that.keyx)) return false;
        if (!valuex.equals(that.valuex)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = keyx.hashCode();
        result = 31 * result + valuex.hashCode();
        return result;
    }
}

package org.tsykora.odata.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

/**
 *
 * @author tsykora
 */
public class Utils {

    /**
     * Serialize Object into byte[].
     *
     * @param obj for serialization
     * @return
     * @throws IOException
     */
    public static byte[] serialize(Object obj) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(obj);
            byte[] yourBytes = bos.toByteArray();
            return yourBytes;
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            return null;            
        } finally {
            try {
                bos.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println(e.getMessage());
            }
        }
    }

    /**
     * Deserialize byte[] into Object.
     * 
     * TODO: better exception handling desing!!!
     *
     * @param data for deserialization
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Object deserialize(byte[] data) {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            Object o = in.readObject();
            return o;
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
            return null;
        } catch (ClassNotFoundException c) {
            c.printStackTrace();
            System.err.println(c.getMessage());
            return null;
        } finally {
            try {
                bis.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();                
                System.err.println(e.getMessage());
            }
        }
    }
}

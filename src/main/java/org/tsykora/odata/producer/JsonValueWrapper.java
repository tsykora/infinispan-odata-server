package org.tsykora.odata.producer;

/**
 * This is used to wrap JSON values. JsonValueWrapperFieldBridge is used as
 * a class bridge to allow indexing of the JSON payload.
 *
 * @author tsykora@redhat.com
 * @since 4.0
 */
public final class JsonValueWrapper {

    // The JSON payload
    private final String json;

    private int hashCode = 0;

    public JsonValueWrapper(String json) {
        this.json = json;
    }

    public String getJson() {
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JsonValueWrapper that = (JsonValueWrapper) o;

        if (hashCode != that.hashCode) return false;
        if (!json.equals(that.json)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = json.hashCode();
        result = 31 * result + hashCode;
        return result;
    }

    @Override
    public String toString() {
        return "JsonValueWrapper(" + json + ')';
    }


    // Do we need to adjust this to JSON (There is no serialization/deserialization needed)
//    public static final class Externalizer extends AbstractExternalizer<ProtobufValueWrapper> {
//
//        @Override
//        public void writeObject(ObjectOutput output, ProtobufValueWrapper protobufValueWrapper) throws IOException {
//            UnsignedNumeric.writeUnsignedInt(output, protobufValueWrapper.getBinary().length);
//            output.write(protobufValueWrapper.getBinary());
//        }
//
//        @Override
//        public ProtobufValueWrapper readObject(ObjectInput input) throws IOException {
//            int length = UnsignedNumeric.readUnsignedInt(input);
//            byte[] binary = new byte[length];
//            input.readFully(binary);
//            return new ProtobufValueWrapper(binary);
//        }
//
//        @Override
//        public Integer getId() {
//            return ExternalizerIds.PROTOBUF_VALUE_WRAPPER;
//        }
//
//        @Override
//        public Set<Class<? extends ProtobufValueWrapper>> getTypeClasses() {
//            return Collections.<Class<? extends ProtobufValueWrapper>>singleton(ProtobufValueWrapper.class);
//        }
//    }
}

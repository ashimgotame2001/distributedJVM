package com.distributed.jvm.shared;

import java.io.*;

public class TaskSerializationUtils {

    public static byte[] serialize(Object obj) throws IOException {
        if (!(obj instanceof Serializable)) {
            throw new IllegalArgumentException("Object must implement Serializable");
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
            return bos.toByteArray();
        }
    }

    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream in = new ObjectInputStream(bis) {
                 @Override
                 protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
                     return Class.forName(desc.getName(), false, Thread.currentThread().getContextClassLoader());
                 }
             }) {
            return in.readObject();
        }
    }
}

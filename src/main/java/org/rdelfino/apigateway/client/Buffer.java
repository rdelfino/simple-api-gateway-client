package org.rdelfino.apigateway.client;

import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Simple in memory buffer implementation
 */
class Buffer extends ByteArrayOutputStream {

    public Buffer(int size) {
        super(size);
    }

    /**
     * @return this buffer contents as an InputStream
     */
    public InputStream asInputStream() {
        return new ByteArrayInputStream(this.buf, 0, this.count);
    }

    /**
     * @return this buffer contents as a ByteBuffer
     */
    public ByteBuffer asByteBuffer() {
        return ByteBuffer.wrap(this.buf, 0, this.count);
    }

    /**
     * Transfers the contents of an InputStream into this buffer
     *
     * @param inputStream the InputStream to transfer from
     */
    @SneakyThrows
    public void transferFrom(final InputStream inputStream){
        inputStream.transferTo(this);
    }

    /**
     * Creates a buffer with the contents of an InputStream
     *
     * @param inputStream the InputStream to create the buffer from
     */
    public static Buffer from(InputStream inputStream){
        Buffer buffer = new Buffer(256);
        buffer.transferFrom(inputStream);
        return buffer;
    }

    /**
     * Returns this buffer contents as a String
     * @param charset the charset used to decode the buffer contents
     * @return this buffer contents as a String
     */
    public String asString(Charset charset) {
         return new String(this.buf, 0, this.count, charset);
    }
}

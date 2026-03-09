package io.smartdbf.config;

public class DbfConfig {

    private int bufferSize = 8192;

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }
}
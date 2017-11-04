package net.schmidtie.presentationrecording.info;

public class ResolutionInfo {
    public boolean isAuto;
    public int mHeight;
    public int mWidth;

    public ResolutionInfo() {
        this.mWidth = 1920;
        this.mHeight = 1080;
        this.isAuto = false;
        this.isAuto = true;
    }

    public ResolutionInfo(int i, int i2) {
        this.mWidth = 1920;
        this.mHeight = 1080;
        this.isAuto = false;
        this.mWidth = i;
        this.mHeight = i2;
        this.isAuto = false;
    }
}
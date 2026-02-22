package dev.verrai.bootstrap;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLImageElement;

public class Image extends Widget {

    private HTMLImageElement img;
    private boolean fluid = false;
    private boolean thumbnail = false;
    private boolean rounded = false;

    public Image() {
        this.element = Window.current().getDocument().createElement("img");
        this.img = (HTMLImageElement) this.element;
    }

    public Image(String src) {
        this();
        setSrc(src);
    }

    public void setSrc(String src) {
        this.img.setSrc(src);
    }

    public void setAlt(String alt) {
        this.img.setAlt(alt);
    }

    public void setFluid(boolean fluid) {
        this.fluid = fluid;
        updateClass();
    }

    public void setThumbnail(boolean thumbnail) {
        this.thumbnail = thumbnail;
        updateClass();
    }

    public void setRounded(boolean rounded) {
        this.rounded = rounded;
        updateClass();
    }

    public String getSrc() {
        return this.img.getSrc();
    }

    public String getAlt() {
        return this.img.getAlt();
    }

    public boolean isFluid() {
        return fluid;
    }

    public boolean isThumbnail() {
        return thumbnail;
    }

    public boolean isRounded() {
        return rounded;
    }

    private void updateClass() {
        StringBuilder sb = new StringBuilder();
        if (fluid) {
            sb.append("img-fluid");
        }
        if (thumbnail) {
            if (sb.length() > 0) sb.append(" ");
            sb.append("img-thumbnail");
        }
        if (rounded) {
            if (sb.length() > 0) sb.append(" ");
            sb.append("rounded");
        }
        this.element.setClassName(sb.toString());
    }
}

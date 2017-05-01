package com.cate.cate.api;


import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name="response", strict=false)
public class ThaCatApiResponse {

    @ElementList(name="images", required=false)
    @Path("data")
    private List<ThaCatApiImage> imageList;

    public List<ThaCatApiImage> getImageList() {
        return imageList;
    }

    public void setImageList(List<ThaCatApiImage> imageList) {
        this.imageList = imageList;
    }
}

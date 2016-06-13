package org.ninthworld.simplegeometry.models;

/**
 * Created by NinthWorld on 6/6/2016.
 */
public class Model {

    private RawModel rawModel;

    public Model(RawModel model){
        this.rawModel = model;
    }

    public RawModel getRawModel() {
        return rawModel;
    }
}
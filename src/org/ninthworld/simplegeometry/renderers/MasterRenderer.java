package org.ninthworld.simplegeometry.renderers;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.util.vector.Matrix4f;
import org.ninthworld.simplegeometry.entities.Camera;
import org.ninthworld.simplegeometry.entities.Entity;
import org.ninthworld.simplegeometry.lights.Light;
import org.ninthworld.simplegeometry.models.Model;
import org.ninthworld.simplegeometry.shaders.MainShader;
import org.ninthworld.simplegeometry.shaders.TerrainShader;
import org.ninthworld.simplegeometry.shadows.ShadowMapMasterRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by NinthWorld on 6/6/2016.
 */
public class MasterRenderer {
    public static final float FOV = 70;
    public static final float NEAR_PLANE = 0.1f;
    public static final float FAR_PLANE = 1000;

    private Matrix4f projectionMatrix;

    private MainShader shader = new MainShader();
    private TerrainShader terrainShader = new TerrainShader();

    private EntityRenderer renderer;
    private TerrainRenderer terrainRenderer;
    private ShadowMapMasterRenderer shadowMapRenderer;

    private Map<Model, List<Entity>> entities = new HashMap<>();

    public MasterRenderer(Loader loader, Camera cam){
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
        createProjectionMatrix();
        renderer = new EntityRenderer(shader, projectionMatrix);
        terrainRenderer = new TerrainRenderer(loader, terrainShader, projectionMatrix);
        shadowMapRenderer = new ShadowMapMasterRenderer(cam, terrainRenderer);
    }

    public void render(List<Entity> entityList, Light sun, Camera camera){
        for(Entity entity : entityList){
            processEntity(entity);
        }

        prepare();
        terrainShader.start();
        terrainShader.connectTextureUnits();
        terrainShader.loadLight(sun);
        terrainShader.loadViewMatrix(camera);
        terrainRenderer.render(shadowMapRenderer.getToShadowMapSpaceMatrix());
        terrainShader.stop();

        shader.start();
        shader.connectTextureUnits();
        shader.loadLight(sun);
        shader.loadViewMatrix(camera);
        renderer.render(entities, shadowMapRenderer.getToShadowMapSpaceMatrix());
        shader.stop();
        entities.clear();
    }

    public void processEntity(Entity entity){
        Model entityModel = entity.getModel();
        List<Entity> batch = entities.get(entityModel);
        if(batch != null){
            batch.add(entity);
        }else{
            List<Entity> newBatch = new ArrayList<>();
            newBatch.add(entity);
            entities.put(entityModel, newBatch);
        }
    }

    public void renderShadowMap(List<Entity> entityList, Light sun){
        for(Entity entity : entityList){
            processEntity(entity);
        }

        shadowMapRenderer.render(entities, sun);
        entities.clear();
    }

    public int getShadowMapTexture(){
        return shadowMapRenderer.getShadowMap();
    }

    public void cleanUp(){
        terrainShader.cleanUp();
        shader.cleanUp();
        shadowMapRenderer.cleanUp();
    }

    public void prepare() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glClearColor(0.49f, 89f, 0.98f, 1);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, getShadowMapTexture());
    }

    private void createProjectionMatrix() {
        float aspectRatio = (float) Display.getWidth() / (float) Display.getHeight();
        float y_scale = (float) ((1f / Math.tan(Math.toRadians(FOV / 2f))) * aspectRatio);
        float x_scale = y_scale / aspectRatio;
        float frustum_length = FAR_PLANE - NEAR_PLANE;

        projectionMatrix = new Matrix4f();
        projectionMatrix.m00 = x_scale;
        projectionMatrix.m11 = y_scale;
        projectionMatrix.m22 = -((FAR_PLANE + NEAR_PLANE) / frustum_length);
        projectionMatrix.m23 = -1;
        projectionMatrix.m32 = -((2 * NEAR_PLANE * FAR_PLANE) / frustum_length);
        projectionMatrix.m33 = 0;
    }
}

package org.ninthworld.simplegeometry.engine;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.ninthworld.simplegeometry.entities.Camera;
import org.ninthworld.simplegeometry.entities.Entity;
import org.ninthworld.simplegeometry.guis.GuiRenderer;
import org.ninthworld.simplegeometry.guis.GuiTexture;
import org.ninthworld.simplegeometry.helper.VoxelHelper;
import org.ninthworld.simplegeometry.lights.Light;
import org.ninthworld.simplegeometry.postProcessing.Fbo;
import org.ninthworld.simplegeometry.postProcessing.PostProcessing;
import org.ninthworld.simplegeometry.renderers.*;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by NinthWorld on 6/2/2016.
 */
public class Main {

    public static void main(String[] args) throws IOException {

        DisplayManager.createDisplay();
        Loader loader = new Loader();

        List<Entity> entities = new ArrayList<>();
        //Entity pumpkin = VoxelHelper.load(loader, new File("models/pumpkin.vobj"));
        Entity tankBody = VoxelHelper.load(loader, ImageIO.read(new File("models/tank_body.png")));
        Entity tankWheelLeft = VoxelHelper.load(loader, ImageIO.read(new File("models/tank_wheel.png")));
        Entity tankWheelRight = VoxelHelper.load(loader, ImageIO.read(new File("models/tank_wheel.png")));

        //pumpkin.setPosition(new Vector3f(0, 10, 0));
        tankBody.setPosition(new Vector3f(0, 10, -15));
        tankWheelLeft.setPosition(new Vector3f(-66, 10, -15-6));
        tankWheelLeft.setRotY(180);
        tankWheelRight.setPosition(new Vector3f(0, 10, -15-7));

        entities.add(tankBody);
        entities.add(tankWheelLeft);
        entities.add(tankWheelRight);

        float lightAngle = 0, lightDistance = 10000;
        Light light = new Light(new Vector3f(0, 0, 0), new Vector3f(1, 1, 1));
        Camera camera = new Camera(new Vector3f(0, 5, 10));

        MasterRenderer renderer = new MasterRenderer(loader, camera);

        NormalRenderer normalRenderer = new NormalRenderer();

        Fbo normalFbo = new Fbo(Display.getWidth(), Display.getHeight(), Fbo.DEPTH_TEXTURE);
        Fbo multisampleFbo = new Fbo(Display.getWidth(), Display.getHeight());
        Fbo outputFbo = new Fbo(Display.getWidth(), Display.getHeight(), Fbo.DEPTH_TEXTURE);
        PostProcessing.init(loader);

        List<GuiTexture> guis = new ArrayList<>();
        // guis.add(new GuiTexture(renderer.getShadowMapTexture(), new Vector2f(0.5f, 0.5f), new Vector2f(0.25f, 0.25f)));
        GuiRenderer guiRenderer = new GuiRenderer(loader);

        boolean showWireframe = false;
        int showWireframe_tick = 0;

        while(!Display.isCloseRequested()){
            if(Keyboard.isKeyDown(Keyboard.KEY_1) && showWireframe_tick <= 0){
                showWireframe = !showWireframe;
                showWireframe_tick = 10;
            }else {
                showWireframe_tick--;
            }

            if(showWireframe) {
                GL11.glEnable(GL11.GL_POLYGON_MODE);
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
            }else{
                GL11.glDisable(GL11.GL_POLYGON_MODE);
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
            }

            if(Keyboard.isKeyDown(Keyboard.KEY_LEFT)){
                lightAngle -= 0.02f;
            }else if(Keyboard.isKeyDown(Keyboard.KEY_RIGHT)){
                lightAngle += 0.02f;
            }
            light.setPosition(new Vector3f((float) Math.cos(lightAngle)*lightDistance, lightDistance - 2000, (float) Math.sin(lightAngle)*lightDistance));

            camera.move();

            normalFbo.bindFrameBuffer();
            normalRenderer.render(entities, camera);
            normalFbo.unbindFrameBuffer();

            renderer.renderShadowMap(entities, light);

            if(!showWireframe)
                multisampleFbo.bindFrameBuffer();
            renderer.render(entities, light, camera);
            if(!showWireframe) {
                multisampleFbo.unbindFrameBuffer();
                multisampleFbo.resolveToFbo(outputFbo);
            }

            if(!showWireframe)
                PostProcessing.doPostProcessing(outputFbo.getColorTexture(), outputFbo.getDepthTexture(), normalFbo.getColorTexture());

            guiRenderer.render(guis);

            DisplayManager.updateDisplay();
        }

        normalFbo.cleanUp();
        multisampleFbo.cleanUp();
        outputFbo.cleanUp();

        PostProcessing.cleanUp();

        renderer.cleanUp();
        normalRenderer.cleanUp();
        guiRenderer.cleanUp();

        loader.cleanUp();
        DisplayManager.closeDisplay();
    }
}